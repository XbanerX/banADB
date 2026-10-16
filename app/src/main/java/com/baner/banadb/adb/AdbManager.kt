package com.baner.banadb.adb

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.baner.banadb.data.model.*
import com.flyfishxu.kadb.Kadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import okio.FileSystem
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

    private var appContext: android.content.Context? = null

    fun getConnectedHost(): String = connectedHost
    fun getConnectedPort(): Int = connectedPort

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        try {
            val keyFile = File(context.filesDir, "kadb_key")
            val certFile = File(context.filesDir, "kadb_cert")
            val path = keyFile.absolutePath.toPath()
            val fileSystem = FileSystem.SYSTEM
            val storeClass = Class.forName("com.flyfishxu.kadb.cert.OkioFilePrivateKeyStore")
            val store = storeClass.getConstructor(okio.Path::class.java, okio.FileSystem::class.java).newInstance(path, fileSystem)
            val kadbCertClass = Class.forName("com.flyfishxu.kadb.cert.KadbCert")
            val kadbCert = kadbCertClass.getField("INSTANCE").get(null)
            val policyClass = Class.forName("com.flyfishxu.kadb.cert.KadbCertPolicy")
            val policy = policyClass.getConstructor().newInstance()
            val configureMethod = kadbCertClass.getMethod(
                "configure",
                Class.forName("com.flyfishxu.kadb.cert.KadbPrivateKeyStore"),
                policyClass,
                List::class.java
            )
            configureMethod.invoke(kadbCert, store, policy, emptyList<ByteArray>())

            if (keyFile.exists() && certFile.exists() && keyFile.length() > 0 && certFile.length() > 0) {
                val keyBytes = keyFile.readBytes()
                val certBytes = certFile.readBytes()
                val importMethod = kadbCertClass.getMethod("importIdentity", ByteArray::class.java, ByteArray::class.java)
                importMethod.invoke(kadbCert, keyBytes, certBytes)
                Log.d(TAG, "Imported persistent key and certificate successfully")
            } else {
                val ensureReadyMethod = kadbCertClass.getMethod("ensureReady")
                val snapshot = ensureReadyMethod.invoke(kadbCert)
                if (snapshot != null) {
                    val keyBytes = snapshot.javaClass.getMethod("getPrivateKeyPem").invoke(snapshot) as ByteArray
                    val certBytes = snapshot.javaClass.getMethod("getCertificatePem").invoke(snapshot) as ByteArray
                    keyFile.writeBytes(keyBytes)
                    certFile.writeBytes(certBytes)
                    Log.d(TAG, "Generated and saved persistent key and certificate")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing KadbCert with persistent store", e)
        }
    }

    suspend fun pair(ip: String, port: Int, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting pairing with $ip:$port using code $code")
            val certUtilsClass = Class.forName("com.flyfishxu.kadb.cert.CertUtils")
            val certUtils = certUtilsClass.getField("INSTANCE").get(null)
            val keyPair = certUtilsClass.getMethod("loadKeyPair").invoke(certUtils)
                ?: throw java.lang.IllegalStateException("No key pair available in KadbCert")

            val pairClass = Class.forName("com.flyfishxu.kadb.pair.PairingConnectionCtx")
            val adbKeyPairClass = Class.forName("com.flyfishxu.kadb.cert.AdbKeyPair")
            val constructor = pairClass.getConstructor(
                String::class.java,
                Int::class.javaPrimitiveType,
                ByteArray::class.java,
                adbKeyPairClass,
                String::class.java
            )
            val pairingCtx = constructor.newInstance(ip, port, code.toByteArray(Charsets.UTF_8), keyPair, "banADB") as java.io.Closeable
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
            val kadb = Kadb(host = ip, port = port, connectTimeout = 10000, socketTimeout = 15000)
            
            // In Kadb, connection() is lazy and triggered by operations.
            // Executing a quick probe command triggers the TLS/ADB handshake and verifies authentication.
            val testOutput = kadb.shell("echo ok").output.trim()
            Log.d(TAG, "Connection handshake successful: $testOutput")

            if (!kadb.connectionCheck()) {
                throw java.io.IOException("Connection failed: transport channel is not open.")
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
        val cleanPath = when {
            path.isBlank() -> "/sdcard"
            path.length > 1 && path.endsWith("/") -> path.dropLast(1)
            else -> path
        }
        val output = executeShell("ls -laL \"$cleanPath\"")
        val files = mutableListOf<RemoteFile>()
        
        output.lines().forEach { rawLine ->
            try {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("total ") || line.startsWith("ls:")) return@forEach
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 8) {
                    val perms = parts[0]
                    if (!perms.startsWith("d") && !perms.startsWith("-") && !perms.startsWith("l")) return@forEach
                    val isDir = perms.startsWith("d")
                    val size = parts[4].toLongOrNull() ?: 0L

                    // Detect date format: Android toybox (YYYY-MM-DD HH:MM) vs GNU coreutils (MMM DD HH:MM)
                    val nameStartIndex = if (parts[5].contains("-") || parts[6].contains(":")) {
                        7
                    } else {
                        8
                    }

                    if (parts.size > nameStartIndex) {
                        var rawName = parts.subList(nameStartIndex, parts.size).joinToString(" ").trim()
                        if (rawName.contains(" -> ")) {
                            rawName = rawName.substringBefore(" -> ").trim()
                        }
                        val name = rawName
                        if (name.isNotEmpty() && name != "." && name != "..") {
                            val fullPath = if (cleanPath == "/") "/$name" else "$cleanPath/$name"
                            val modified = if (nameStartIndex == 7) {
                                "${parts.getOrElse(5) { "" }} ${parts.getOrElse(6) { "" }}".trim()
                            } else {
                                "${parts.getOrElse(5) { "" }} ${parts.getOrElse(6) { "" }} ${parts.getOrElse(7) { "" }}".trim()
                            }
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
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing ls line: $rawLine", e)
            }
        }
        files.distinctBy { it.path }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
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

    suspend fun extractApk(packageName: String, label: String, destDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pathOutput = executeShell("pm path $packageName").trim()
            val remotePath = pathOutput.lineSequence()
                .firstOrNull { it.startsWith("package:") }
                ?.substringAfter("package:")
                ?.trim()
                ?: return@withContext Result.failure(Exception("Path not found for $packageName"))

            val safeRemote = "/sdcard/Download/${packageName}_extract.apk"
            executeShell("cp \"$remotePath\" \"$safeRemote\"")

            if (!destDir.exists()) destDir.mkdirs()
            val cleanLabel = label.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val localFile = File(destDir, "${cleanLabel}_${packageName}.apk")

            val pulled = pullFile(safeRemote, localFile)
            executeShell("rm -f \"$safeRemote\"")

            if (pulled && localFile.exists() && localFile.length() > 0) {
                Result.success(localFile)
            } else {
                Result.failure(Exception("Could not download APK file"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "extractApk error", e)
            Result.failure(e)
        }
    }

    suspend fun installApk(srcFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val remoteTemp = "/data/local/tmp/app_install.apk"
            val pushed = pushFile(srcFile, remoteTemp)
            if (!pushed) {
                return@withContext Result.failure(Exception("Failed to push APK to remote device"))
            }
            val output = executeShell("pm install -r \"$remoteTemp\"").trim()
            executeShell("rm -f \"$remoteTemp\"")

            if (output.contains("Success", ignoreCase = true)) {
                Result.success("Installation successful")
            } else {
                Result.failure(Exception(output.ifEmpty { "Installation failed" }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "installApk error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchDashboard(): DeviceDashboardData = withContext(Dispatchers.IO) {
        try {
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

            val rawUptime = executeShell("uptime").trim()
            val uptime = if (rawUptime.contains("up ")) {
                rawUptime.substringAfter("up ")
                    .substringBefore(",  ")
                    .substringBefore(", 0 user")
                    .substringBefore(", 1 user")
                    .substringBefore(", 2 user")
                    .trim()
            } else rawUptime

            DeviceDashboardData(
                model = "$manufacturer $model".trim().ifEmpty { "Connected Device" },
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
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching dashboard data", e)
            DeviceDashboardData(
                model = "Connected Device",
                ipAddress = connectedHost
            )
        }
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
        val records = out.split("NotificationRecord(")
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

        for (record in records.drop(1)) {
            val pkg = Regex("pkg=([^\\s]+)").find(record)?.groupValues?.get(1) ?: ""
            if (pkg.isEmpty()) continue

            val rawKey = Regex("key=([^\\s\\n:]+)").find(record)?.groupValues?.get(1) ?: ""
            val whenMatch = Regex("when=(\\d+)").find(record)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

            var title = Regex("android\\.title=String \\((.*?)\\)").find(record)?.groupValues?.get(1) ?: ""
            var text = Regex("android\\.text=String \\((.*?)\\)").find(record)?.groupValues?.get(1) ?: ""
            if (text.isEmpty()) {
                text = Regex("android\\.bigText=String \\((.*?)\\)").find(record)?.groupValues?.get(1) ?: ""
            }
            val ticker = Regex("tickerText=(.*?)\\n").find(record)?.groupValues?.get(1)?.trim()
            if (ticker != null && ticker != "null" && ticker.isNotEmpty()) {
                if (text.isEmpty()) text = ticker
                if (title.isEmpty()) title = ticker
            }
            val subText = Regex("android\\.subText=String \\((.*?)\\)").find(record)?.groupValues?.get(1) ?: ""

            if (title.isNotEmpty() || text.isNotEmpty()) {
                val category = categorizePkg(pkg)
                val postTime = if (whenMatch > 0L) timeFormat.format(java.util.Date(whenMatch)) else ""
                items.add(
                    NotificationItem(
                        id = rawKey.ifEmpty { "${pkg}_${whenMatch}" },
                        key = rawKey,
                        pkg = pkg,
                        title = title,
                        text = text,
                        subText = subText,
                        postTime = postTime,
                        category = category,
                        timestampMs = whenMatch
                    )
                )
            }
        }
        items.distinctBy { "${it.pkg}_${it.title}_${it.text}" }.sortedByDescending { it.timestampMs }
    }

    private fun categorizePkg(pkg: String): String {
        val p = pkg.lowercase()
        return when {
            p.contains("sms") || p.contains("mms") || p.contains("message") ||
            p.contains("whatsapp") || p.contains("telegram") || p.contains("signal") ||
            p.contains("messenger") || p.contains("viber") || p.contains("slack") ||
            p.contains("line") || p.contains("chat") -> "Messages"

            p.contains("facebook") || p.contains("instagram") || p.contains("twitter") ||
            p.contains("x.com") || p.contains("tiktok") || p.contains("reddit") ||
            p.contains("discord") || p.contains("threads") || p.contains("snapchat") ||
            p.contains("pinterest") || p.contains("linkedin") -> "Social"

            p.contains("mail") || p.contains("gmail") || p.contains("outlook") ||
            p.contains("yahoo") || p.contains("spark") || p.contains("thunderbird") -> "Email"

            else -> "System"
        }
    }

    suspend fun dismissNotification(key: String): Boolean = withContext(Dispatchers.IO) {
        if (key.isNotEmpty()) {
            executeShell("cmd notification cancel \"$key\" 2>/dev/null || cmd notification snooze --for 86400000 \"$key\" 2>/dev/null")
            true
        } else false
    }

    suspend fun fetchRemoteGalleryMedia(): List<RemoteMediaItem> = withContext(Dispatchers.IO) {
        val cmd = "find /sdcard/DCIM /sdcard/Pictures /sdcard/Movies /sdcard/Download -maxdepth 4 -type f \\( -name '*.jpg' -o -name '*.jpeg' -o -name '*.png' -o -name '*.webp' -o -name '*.mp4' -o -name '*.mkv' -o -name '*.mov' \\) 2>/dev/null | grep -v '/\\.' | while read -r f; do stat -c '%s|%Y|%n' \"\$f\"; done"
        val out = executeShell(cmd)
        val list = mutableListOf<RemoteMediaItem>()

        out.lines().forEach { line ->
            val trim = line.trim()
            if (trim.contains('|')) {
                val parts = trim.split('|', limit = 3)
                if (parts.size == 3) {
                    val size = parts[0].toLongOrNull() ?: 0L
                    val epochSec = parts[1].toLongOrNull() ?: 0L
                    val path = parts[2].trim()
                    if (path.isNotEmpty() && size > 0) {
                        val name = path.substringAfterLast('/')
                        val isVideo = path.endsWith(".mp4", ignoreCase = true) ||
                                      path.endsWith(".mkv", ignoreCase = true) ||
                                      path.endsWith(".mov", ignoreCase = true)
                        val category = when {
                            path.contains("/DCIM", ignoreCase = true) || path.contains("Camera", ignoreCase = true) -> "Camera"
                            path.contains("Screenshot", ignoreCase = true) -> "Screenshots"
                            isVideo || path.contains("/Movies", ignoreCase = true) -> "Videos"
                            path.contains("/Download", ignoreCase = true) -> "Downloads"
                            else -> "Pictures"
                        }
                        list.add(
                            RemoteMediaItem(
                                path = path,
                                name = name,
                                size = size,
                                timestamp = epochSec * 1000L,
                                isVideo = isVideo,
                                category = category
                            )
                        )
                    }
                }
            }
        }
        list.sortedByDescending { it.timestamp }
    }

    suspend fun deleteRemoteMedia(path: String): Boolean = withContext(Dispatchers.IO) {
        executeShell("rm -f \"$path\"")
        executeShell("content delete --uri content://media/external/file --where \"_data='$path'\" 2>/dev/null")
        true
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
        try {
            val sanitized = text.replace(" ", "%s")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("&", "\\&")
                .replace("<", "\\<")
                .replace(">", "\\>")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace(";", "\\;")
            executeShell("input text '$sanitized'")
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendText error", e)
            false
        }
    }

    suspend fun triggerSearch(): Boolean = withContext(Dispatchers.IO) {
        executeShell("am start -a android.intent.action.WEB_SEARCH 2>/dev/null || input keyevent 84 || am start -a android.intent.action.ASSIST")
        true
    }

    private suspend fun ensureClipBinaryDeployed(): Boolean = withContext(Dispatchers.IO) {
        val kadb = activeKadb ?: return@withContext false
        try {
            val check = executeShell("ls -l /data/local/tmp/banaclip.jar 2>/dev/null")
            if (check.contains("banaclip.jar") && !check.contains("No such file")) {
                return@withContext true
            }

            val ctx = appContext ?: return@withContext false
            val localTmp = File(ctx.cacheDir, "banaclip.jar")
            ctx.assets.open("banaclip.jar").use { input ->
                localTmp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (localTmp.exists()) {
                val pushed = pushFile(localTmp, "/data/local/tmp/banaclip.jar")
                executeShell("chmod 644 /data/local/tmp/banaclip.jar")
                return@withContext pushed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed deploying banaclip.jar", e)
        }
        false
    }

    suspend fun getClipboard(): String = withContext(Dispatchers.IO) {
        try {
            ensureClipBinaryDeployed()
            val out = executeShell("CLASSPATH=/data/local/tmp/banaclip.jar app_process /data/local/tmp com.baner.clip.ClipRunner get")
            if (out.isNotEmpty() && !out.contains("Error", ignoreCase = true) && !out.contains("Exception", ignoreCase = true)) {
                return@withContext out
            }

            // Fallback for systems that support cmd clipboard
            val legacyOut = executeShell("cmd clipboard get 2>/dev/null").trim()
            if (legacyOut.isNotEmpty() && !legacyOut.contains("No shell command", ignoreCase = true) && !legacyOut.contains("error", ignoreCase = true)) {
                return@withContext legacyOut
            }
        } catch (e: Exception) {
            Log.w(TAG, "getClipboard failed", e)
        }
        ""
    }

    suspend fun setClipboard(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            ensureClipBinaryDeployed()
            val b64 = android.util.Base64.encodeToString(text.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            val res = executeShell("CLASSPATH=/data/local/tmp/banaclip.jar app_process /data/local/tmp com.baner.clip.ClipRunner setb64 '$b64'").trim()
            if (res.contains("OK")) {
                return@withContext true
            }

            // Fallback for older legacy devices
            val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("`", "\\`").replace("$", "\\$")
            val cmd = "cmd clipboard set text \"$escaped\" 2>/dev/null || service call clipboard 2 s16 \"$escaped\""
            executeShell(cmd)
            true
        } catch (e: Exception) {
            Log.e(TAG, "setClipboard error", e)
            false
        }
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

    suspend fun saveScreenshotToGallery(context: android.content.Context, bitmap: Bitmap): android.net.Uri? = withContext(Dispatchers.IO) {
        try {
            val filename = "banADB_${System.currentTimeMillis()}.png"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/banADB")
                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                uri
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "saveScreenshotToGallery failed", e)
            null
        }
    }
}
