package com.baner.banadb.adb

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.baner.banadb.data.model.*
import com.flyfishxu.kadb.Kadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

object AdbManager {
    private const val TAG = "AdbManager"

    @Volatile
    private var activeKadb: Kadb? = null
    private var connectedHost: String = ""
    private var connectedPort: Int = 5555

    val isConnected: Boolean
        get() = activeKadb?.connectionCheck() == true

    fun getConnectedHost(): String = connectedHost
    fun getConnectedPort(): Int = connectedPort

    suspend fun pair(ip: String, port: Int, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting pairing with $ip:$port using code $code")
            val certUtilsClass = Class.forName("com.flyfishxu.kadb.cert.CertUtils")
            val certUtils = certUtilsClass.getField("INSTANCE").get(null)

            var keyPair: Any? = null
            try {
                keyPair = certUtilsClass.getMethod("loadKeyPair").invoke(certUtils)
            } catch (_: Throwable) {}

            if (keyPair == null) {
                val policyClass = Class.forName("com.flyfishxu.kadb.cert.KadbCertPolicy")
                val policy = policyClass.getConstructor().newInstance()
                val identity = certUtilsClass.getMethod("generateNewIdentity", policyClass).invoke(certUtils, policy)
                keyPair = identity?.javaClass?.getMethod("getKeyPair")?.invoke(identity)
            }

            val pairClass = Class.forName("com.flyfishxu.kadb.pair.PairingConnectionCtx")
            val adbKeyPairClass = Class.forName("com.flyfishxu.kadb.cert.AdbKeyPair")
            val constructor = pairClass.getConstructor(
                String::class.java,
                Int::class.javaPrimitiveType,
                ByteArray::class.java,
                adbKeyPairClass,
                String::class.java
            )
            val pairingCtx = constructor.newInstance(ip, port, code.toByteArray(Charsets.UTF_8), keyPair, "BanaADB") as java.io.Closeable
            try {
                pairClass.getMethod("start").invoke(pairingCtx)
            } finally {
                try {
                    pairingCtx.close()
                } catch (_: Throwable) {}
            }
            Log.d(TAG, "Pairing completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Pairing error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun connect(ip: String, port: Int): Result<Kadb> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            Log.d(TAG, "Connecting to $ip:$port...")
            val kadb = Kadb(host = ip, port = port)
            if (!kadb.connectionCheck()) {
                throw java.io.IOException("Failed to connect to $ip:$port")
            }
            activeKadb = kadb
            connectedHost = ip
            connectedPort = port
            Log.d(TAG, "Connected to $ip:$port successfully")
            Result.success(kadb)
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}", e)
            disconnect()
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            activeKadb?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection", e)
        } finally {
            activeKadb = null
            connectedHost = ""
            connectedPort = 5555
        }
    }

    suspend fun executeShell(command: String): String = withContext(Dispatchers.IO) {
        val kadb = activeKadb ?: return@withContext "Error: Not connected to any device."
        try {
            val response = kadb.shell(command)
            response.output
        } catch (e: Exception) {
            "Error executing command: ${e.message}"
        }
    }

    suspend fun executeShellStream(command: String, onLine: (String) -> Unit) = withContext(Dispatchers.IO) {
        val kadb = activeKadb ?: run {
            onLine("Error: Not connected to any device.")
            return@withContext
        }
        try {
            val stream = kadb.openShell(command)
            while (true) {
                val packet = stream.read()
                val text = String(packet.payload, Charsets.UTF_8)
                if (text.isEmpty()) break
                text.lines().forEach { onLine(it) }
            }
            stream.close()
        } catch (e: Exception) {
            onLine("Shell stream ended: ${e.message}")
        }
    }

    suspend fun listRemoteFiles(path: String): List<RemoteFile> = withContext(Dispatchers.IO) {
        val cleanPath = if (path.isEmpty()) "/sdcard" else path
        val output = executeShell("ls -laL \"$cleanPath\"")
        val files = mutableListOf<RemoteFile>()
        
        output.lines().forEach { line ->
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size >= 8) {
                val perms = parts[0]
                if (perms.startsWith("total") || perms.isEmpty()) return@forEach
                val isDir = perms.startsWith("d")
                val size = parts[4].toLongOrNull() ?: 0L
                val modified = "${parts[5]} ${parts[6]} ${parts[7]}"
                val name = parts.subList(8, parts.size).joinToString(" ")
                if (name != "." && name != "..") {
                    val fullPath = if (cleanPath.endsWith("/")) "$cleanPath$name" else "$cleanPath/$name"
                    files.add(
                        RemoteFile(
                            name = name,
                            path = fullPath,
                            size = size,
                            isDirectory = isDir,
                            permissions = perms,
                            modified = modified
                        )
                    )
                }
            }
        }
        files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun createRemoteDir(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        val res = executeShell("mkdir -p \"$remotePath\"")
        !res.contains("error", ignoreCase = true)
    }

    suspend fun deleteRemoteFile(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        val res = executeShell("rm -rf \"$remotePath\"")
        !res.contains("error", ignoreCase = true)
    }

    suspend fun pullFile(remotePath: String, destFile: File): Boolean = withContext(Dispatchers.IO) {
        val kadb = activeKadb ?: return@withContext false
        try {
            kadb.pull(destFile, remotePath)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Pull file failed, fallback to stream", e)
            false
        }
    }

    suspend fun pushFile(srcFile: File, remotePath: String): Boolean = withContext(Dispatchers.IO) {
        val kadb = activeKadb ?: return@withContext false
        try {
            kadb.push(srcFile, remotePath)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Push file failed", e)
            false
        }
    }

    suspend fun fetchInstalledApps(includeSystem: Boolean = false): List<RemoteAppInfo> = withContext(Dispatchers.IO) {
        val flag = if (includeSystem) "" else "-3"
        val output = executeShell("pm list packages -f $flag")
        val list = mutableListOf<RemoteAppInfo>()

        output.lines().forEach { line ->
            if (line.startsWith("package:")) {
                val clean = line.removePrefix("package:")
                val eqIdx = clean.lastIndexOf('=')
                if (eqIdx != -1) {
                    val sourceDir = clean.substring(0, eqIdx)
                    val pkgName = clean.substring(eqIdx + 1).trim()
                    val isSys = sourceDir.startsWith("/system") || sourceDir.startsWith("/product") || sourceDir.startsWith("/vendor")
                    val label = pkgName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                    list.add(
                        RemoteAppInfo(
                            packageName = pkgName,
                            label = label,
                            sourceDir = sourceDir,
                            isSystem = isSys
                        )
                    )
                }
            }
        }
        list.sortedBy { it.label.lowercase() }
    }

    suspend fun launchApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val out = executeShell("monkey -p \"$packageName\" -c android.intent.category.LAUNCHER 1")
        out.contains("Events injected: 1")
    }

    suspend fun forceStopApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        executeShell("am force-stop \"$packageName\"")
        true
    }

    suspend fun clearAppData(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val out = executeShell("pm clear \"$packageName\"")
        out.contains("Success", ignoreCase = true)
    }

    suspend fun uninstallApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val out = executeShell("pm uninstall \"$packageName\"")
        out.contains("Success", ignoreCase = true)
    }

    suspend fun fetchDashboard(): DeviceDashboardData = withContext(Dispatchers.IO) {
        val model = executeShell("getprop ro.product.model").trim().ifEmpty { "Android Device" }
        val manufacturer = executeShell("getprop ro.product.manufacturer").trim()
        val androidVersion = executeShell("getprop ro.build.version.release").trim()
        val sdkVersion = executeShell("getprop ro.build.version.sdk").trim()
        
        // Battery info
        val batteryDump = executeShell("dumpsys battery")
        var level = -1
        var status = "Unknown"
        var health = "Good"
        var temp = 0f
        var voltage = 0

        batteryDump.lines().forEach { l ->
            val trim = l.trim()
            when {
                trim.startsWith("level:") -> level = trim.substringAfter(":").trim().toIntOrNull() ?: level
                trim.startsWith("status:") -> {
                    status = when (trim.substringAfter(":").trim()) {
                        "2" -> "Charging"
                        "3" -> "Discharging"
                        "4" -> "Not Charging"
                        "5" -> "Full"
                        else -> "Plugged"
                    }
                }
                trim.startsWith("health:") -> {
                    health = when (trim.substringAfter(":").trim()) {
                        "2" -> "Good"
                        "3" -> "Overheat"
                        "4" -> "Dead"
                        "5" -> "Over Voltage"
                        else -> "Normal"
                    }
                }
                trim.startsWith("temperature:") -> {
                    temp = (trim.substringAfter(":").trim().toFloatOrNull() ?: 0f) / 10f
                }
                trim.startsWith("voltage:") -> {
                    voltage = trim.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }
        }

        // Screen resolution
        val wmSize = executeShell("wm size").trim()
        var w = 1080
        var h = 2400
        val sizeMatch = Regex("(\\d+)x(\\d+)").find(wmSize)
        if (sizeMatch != null) {
            w = sizeMatch.groupValues[1].toIntOrNull() ?: 1080
            h = sizeMatch.groupValues[2].toIntOrNull() ?: 2400
        }

        val uptime = executeShell("uptime").trim()

        DeviceDashboardData(
            model = "$manufacturer $model".trim(),
            manufacturer = manufacturer,
            androidVersion = androidVersion,
            sdkVersion = sdkVersion,
            batteryLevel = level,
            batteryStatus = status,
            batteryHealth = health,
            batteryTemp = temp,
            batteryVoltage = voltage,
            screenWidth = w,
            screenHeight = h,
            ipAddress = connectedHost,
            uptime = uptime
        )
    }

    suspend fun fetchProcesses(): List<RunningProcess> = withContext(Dispatchers.IO) {
        val out = executeShell("ps -A")
        val procs = mutableListOf<RunningProcess>()
        val lines = out.lines()
        if (lines.size > 1) {
            lines.drop(1).forEach { line ->
                val tokens = line.trim().split("\\s+".toRegex())
                if (tokens.size >= 9) {
                    val user = tokens[0]
                    val pid = tokens[1]
                    val name = tokens.last()
                    procs.add(RunningProcess(pid = pid, user = user, name = name))
                }
            }
        }
        procs.sortedBy { it.name }
    }

    suspend fun killProcess(pid: String): Boolean = withContext(Dispatchers.IO) {
        executeShell("kill -9 $pid")
        true
    }

    suspend fun fetchNotifications(): List<NotificationItem> = withContext(Dispatchers.IO) {
        val out = executeShell("dumpsys notification --noredact")
        val items = mutableListOf<NotificationItem>()
        var currentPkg = ""
        var currentTitle = ""
        var currentText = ""

        out.lines().forEach { line ->
            val trim = line.trim()
            if (trim.startsWith("pkg=")) {
                currentPkg = trim.substringAfter("pkg=").substringBefore(" ")
            } else if (trim.startsWith("android.title=")) {
                currentTitle = trim.substringAfter("android.title=").removeSurrounding("String (", ")")
            } else if (trim.startsWith("android.text=")) {
                currentText = trim.substringAfter("android.text=").removeSurrounding("String (", ")")
                if (currentPkg.isNotEmpty() && (currentTitle.isNotEmpty() || currentText.isNotEmpty())) {
                    items.add(NotificationItem(pkg = currentPkg, title = currentTitle, text = currentText))
                    currentTitle = ""
                    currentText = ""
                }
            }
        }
        items
    }

    // Remote Navigation and Control
    suspend fun sendKeyevent(keyCode: Int): Boolean = withContext(Dispatchers.IO) {
        executeShell("input keyevent $keyCode")
        true
    }

    suspend fun sendTap(x: Float, y: Float): Boolean = withContext(Dispatchers.IO) {
        executeShell("input tap ${x.toInt()} ${y.toInt()}")
        true
    }

    suspend fun sendSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Int = 300): Boolean = withContext(Dispatchers.IO) {
        executeShell("input swipe ${x1.toInt()} ${y1.toInt()} ${x2.toInt()} ${y2.toInt()} $durationMs")
        true
    }

    suspend fun sendText(text: String): Boolean = withContext(Dispatchers.IO) {
        val sanitized = text.replace(" ", "%s").replace("'", "\\'")
        executeShell("input text '$sanitized'")
        true
    }

    suspend fun getClipboard(): String = withContext(Dispatchers.IO) {
        executeShell("cmd clipboard get").trim()
    }

    suspend fun setClipboard(text: String): Boolean = withContext(Dispatchers.IO) {
        val sanitized = text.replace("'", "\\'")
        executeShell("cmd clipboard set '$sanitized'")
        true
    }

    suspend fun toggleWifi(enable: Boolean): Boolean = withContext(Dispatchers.IO) {
        executeShell("svc wifi ${if (enable) "enable" else "disable"}")
        true
    }

    suspend fun toggleBluetooth(enable: Boolean): Boolean = withContext(Dispatchers.IO) {
        executeShell("svc bluetooth ${if (enable) "enable" else "disable"}")
        true
    }

    suspend fun reboot(mode: String = ""): Boolean = withContext(Dispatchers.IO) {
        executeShell("reboot $mode")
        true
    }

    suspend fun captureScreenshot(): Bitmap? = withContext(Dispatchers.IO) {
        val kadb = activeKadb ?: return@withContext null
        try {
            val stream = kadb.open("shell:screencap -p")
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            val input: InputStream = stream.source.inputStream()
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            stream.close()
            val rawBytes = output.toByteArray()
            if (rawBytes.isNotEmpty()) {
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot capture failed", e)
            null
        }
    }
}
