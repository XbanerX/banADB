package com.baner.banadb.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baner.banadb.adb.AdbManager
import com.baner.banadb.adb.MdnsDiscoveryManager
import com.baner.banadb.adb.NetworkUtils
import com.baner.banadb.data.model.*
import com.baner.banadb.data.repository.DevicePreferences
import com.baner.banadb.service.AdbConnectionService
import com.baner.banadb.service.LocalPairingService
import com.baner.banadb.service.LocalPairingStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream

class AdbViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = DevicePreferences(application)
    private val discoveryManager = MdnsDiscoveryManager(application)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    private val _isOperationLoading = MutableStateFlow(false)
    val isOperationLoading: StateFlow<Boolean> = _isOperationLoading.asStateFlow()

    fun clearOperationStatus() {
        _operationStatus.value = null
    }

    val discoveredDevices = discoveryManager.discoveredDevices

    private val _savedDevices = MutableStateFlow<List<SavedDevice>>(emptyList())
    val savedDevices: StateFlow<List<SavedDevice>> = _savedDevices.asStateFlow()

    private val _consoleOutput = MutableStateFlow("")
    val consoleOutput: StateFlow<String> = _consoleOutput.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _dashboardData = MutableStateFlow(DeviceDashboardData())
    val dashboardData: StateFlow<DeviceDashboardData> = _dashboardData.asStateFlow()

    private val _remoteFiles = MutableStateFlow<List<RemoteFile>>(emptyList())
    val remoteFiles: StateFlow<List<RemoteFile>> = _remoteFiles.asStateFlow()

    private val _currentRemotePath = MutableStateFlow("/sdcard")
    val currentRemotePath: StateFlow<String> = _currentRemotePath.asStateFlow()

    private val _installedApps = MutableStateFlow<List<RemoteAppInfo>>(emptyList())
    val installedApps: StateFlow<List<RemoteAppInfo>> = _installedApps.asStateFlow()

    private val _processes = MutableStateFlow<List<RunningProcess>>(emptyList())
    val processes: StateFlow<List<RunningProcess>> = _processes.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _isNotificationSyncEnabled = MutableStateFlow(false)
    val isNotificationSyncEnabled: StateFlow<Boolean> = _isNotificationSyncEnabled.asStateFlow()

    private val _galleryMedia = MutableStateFlow<List<RemoteMediaItem>>(emptyList())
    val galleryMedia: StateFlow<List<RemoteMediaItem>> = _galleryMedia.asStateFlow()

    private val _isGalleryLoading = MutableStateFlow(false)
    val isGalleryLoading: StateFlow<Boolean> = _isGalleryLoading.asStateFlow()

    private val _scripts = MutableStateFlow<List<ScriptItem>>(emptyList())
    val scripts: StateFlow<List<ScriptItem>> = _scripts.asStateFlow()

    private val _screenshotBitmap = MutableStateFlow<Bitmap?>(null)
    val screenshotBitmap: StateFlow<Bitmap?> = _screenshotBitmap.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val localPairingState = LocalPairingStateHolder

    init {
        loadSavedDevices()
        loadScripts()
        appendConsole("banADB Terminal initialized.\nType a command or choose a suggestion below.\n")

        // Monitor background connection state changes (e.g. from LocalPairingReceiver)
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                if (AdbManager.isConnected && _connectionState.value !is ConnectionState.Connected) {
                    val host = AdbManager.getConnectedHost().ifEmpty { "127.0.0.1" }
                    val port = AdbManager.getConnectedPort()
                    val devName = try { AdbManager.fetchDashboard().model } catch (_: Exception) { "Local Device" }
                    _connectionState.value = ConnectionState.Connected(devName, host, port)
                    val saved = SavedDevice(
                        id = java.util.UUID.randomUUID().toString(),
                        name = devName,
                        ip = host,
                        port = port,
                        lastConnected = System.currentTimeMillis()
                    )
                    prefs.saveDevice(saved)
                    loadSavedDevices()
                    appendConsole("Connected to $devName ($host:$port)")
                    refreshDashboard()
                }
            }
        }
    }

    fun loadSavedDevices() {
        _savedDevices.value = prefs.getSavedDevices()
    }

    fun loadScripts() {
        _scripts.value = prefs.getScripts()
    }

    fun startDiscovery() {
        discoveryManager.startDiscovery()
    }

    fun stopDiscovery() {
        discoveryManager.stopDiscovery()
    }

    private val _isLocalScanning = MutableStateFlow(false)
    val isLocalScanning: StateFlow<Boolean> = _isLocalScanning.asStateFlow()

    fun scanAndConnectLocal(onPortFound: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLocalScanning.value = true
            _statusMessage.value = "Scanning for local ADB daemon..."
            appendConsole("Starting Auto-Scan for local ADB daemon on 127.0.0.1...")
            discoveryManager.startDiscovery()

            val excludePairingPort = LocalPairingStateHolder.discoveredPairingPort.value ?: -1
            var foundPort: Int? = null

            // 1. Check if LocalPairingStateHolder has a discovered connect port
            val candidateConnectPort = LocalPairingStateHolder.discoveredConnectPort.value
            if (candidateConnectPort != null && candidateConnectPort > 0 && candidateConnectPort != excludePairingPort) {
                if (isPortOpen("127.0.0.1", candidateConnectPort)) {
                    foundPort = candidateConnectPort
                    Log.d("AdbViewModel", "Found active connect port from LocalPairingService: $foundPort")
                }
            }

            // 2. Check if mDNS discovered a connect service
            if (foundPort == null) {
                val localIps = NetworkUtils.getLocalIpv4Addresses() + "127.0.0.1"
                val mDnsDev = discoveredDevices.value.firstOrNull { dev ->
                    !dev.isPairing && dev.port != excludePairingPort &&
                        (localIps.contains(dev.ip) || dev.ip.startsWith("127."))
                }
                if (mDnsDev != null && isPortOpen("127.0.0.1", mDnsDev.port)) {
                    foundPort = mDnsDev.port
                    Log.d("AdbViewModel", "Found active connect port from mDNS: $foundPort")
                }
            }

            // 3. Check standard port 5555
            if (foundPort == null && excludePairingPort != 5555 && isPortOpen("127.0.0.1", 5555)) {
                foundPort = 5555
                Log.d("AdbViewModel", "Found open port 5555 on 127.0.0.1")
            }

            // 4. Scan dynamic Android 11+ Wireless Debugging ranges in parallel chunks
            if (foundPort == null) {
                val ranges = listOf(
                    37000..45000,
                    32768..36999,
                    45001..52000
                )
                for (range in ranges) {
                    if (foundPort != null || !isActive) break
                    for (chunk in range.chunked(60)) {
                        if (foundPort != null || !isActive) break
                        val deferreds = chunk.map { p ->
                            async(Dispatchers.IO) {
                                if (p != excludePairingPort && isPortOpen("127.0.0.1", p, timeoutMs = 75)) p else null
                            }
                        }
                        val results = deferreds.awaitAll()
                        val detected = results.firstOrNull { it != null }
                        if (detected != null) {
                            foundPort = detected
                            Log.d("AdbViewModel", "Auto-Scan detected open port: $foundPort")
                            break
                        }
                    }
                }
            }

            _isLocalScanning.value = false

            withContext(Dispatchers.Main) {
                if (foundPort != null) {
                    onPortFound?.invoke(foundPort)
                    _statusMessage.value = "Found local ADB on port $foundPort. Connecting..."
                    appendConsole("Auto-Scan found port $foundPort! Connecting to 127.0.0.1:$foundPort...")
                    connect("127.0.0.1", foundPort)
                } else {
                    _statusMessage.value = "No local ADB port found."
                    appendConsole("Auto-Scan: No open ADB port detected on 127.0.0.1. Ensure Wireless Debugging is enabled.")
                }
            }
        }
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int = 100): Boolean {
        return try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun startLocalPairingService(context: Context) {
        LocalPairingService.start(context)
    }

    fun stopLocalPairingService(context: Context) {
        LocalPairingService.stop(context)
    }

    fun getPreferredLocalIp(): String = NetworkUtils.getPreferredLocalIp()

    fun getLocalIpv4Addresses(): List<String> = NetworkUtils.getLocalIpv4Addresses()

    fun pairLocalDevice(
        ip: String,
        pairingPort: Int,
        pairingCode: String,
        connectionPort: Int,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Pairing("Pairing local device on port $pairingPort...")
            appendConsole("Local pairing on port $pairingPort (Code: $pairingCode)...")

            val targetIps = if (ip.isNotBlank()) listOf(ip) else listOf(
                NetworkUtils.getPreferredLocalIp(),
                "127.0.0.1"
            ).distinct()

            var success = false
            var usedIp = targetIps.first()
            var lastErr = "Pairing failed"

            for (targetIp in targetIps) {
                appendConsole("Trying local pairing on $targetIp:$pairingPort...")
                val result = AdbManager.pair(targetIp, pairingPort, pairingCode)
                if (result.isSuccess) {
                    success = true
                    usedIp = targetIp
                    break
                } else {
                    lastErr = result.exceptionOrNull()?.message ?: "Handshake failed"
                    appendConsole("Failed on $targetIp: $lastErr")
                }
            }

            if (success) {
                appendConsole("Local pairing succeeded! Waiting for target daemon...")
                kotlinx.coroutines.delay(800)
                val connPort = if (connectionPort > 0) connectionPort else 5555
                appendConsole("Connecting locally to $usedIp:$connPort...")
                doConnect(usedIp, connPort)
                onComplete(true)
            } else {
                _connectionState.value = ConnectionState.Error(lastErr)
                appendConsole("Local pairing failed: $lastErr")
                onComplete(false)
            }
        }
    }

    fun doPairing(ip: String, pairingPort: Int, pairingCode: String, connectionPort: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Pairing("Pairing with $ip:$pairingPort...")
            appendConsole("Pairing with $ip:$pairingPort (Code: $pairingCode)...")
            
            val pairResult = AdbManager.pair(ip, pairingPort, pairingCode)
            if (pairResult.isSuccess) {
                appendConsole("Pairing succeeded! Waiting for target daemon to register key...")
                kotlinx.coroutines.delay(800)
                appendConsole("Connecting to port $connectionPort...")
                val connPort = if (connectionPort > 0) connectionPort else 5555
                doConnect(ip, connPort)
                onComplete(true)
            } else {
                val err = pairResult.exceptionOrNull()?.message ?: "Unknown pairing error"
                _connectionState.value = ConnectionState.Error(err)
                appendConsole("Pairing failed: $err")
                onComplete(false)
            }
        }
    }

    fun pairAndConnect(
        ip: String,
        pairingPort: Int,
        pairingCode: String,
        connectionPort: Int? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        val port = if (connectionPort != null && connectionPort > 0) connectionPort else 5555
        doPairing(ip, pairingPort, pairingCode, port, onResult)
    }

    fun connect(ip: String, port: Int) {
        doConnect(ip, port)
    }

    fun doConnect(ip: String, port: Int) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting("Connecting to $ip:$port...")
            appendConsole("Connecting to $ip:$port...")

            val result = AdbManager.connect(ip, port)
            if (result.isSuccess) {
                val devName = AdbManager.fetchDashboard().model
                _connectionState.value = ConnectionState.Connected(devName, ip, port)
                appendConsole("Connected to $devName ($ip:$port)")
                
                // Save device
                val saved = SavedDevice(
                    id = java.util.UUID.randomUUID().toString(),
                    name = devName,
                    ip = ip,
                    port = port
                )
                prefs.saveDevice(saved)
                loadSavedDevices()

                // Start Foreground Service
                AdbConnectionService.start(getApplication(), devName, ip)

                // Load initial dashboard data
                refreshDashboard()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Connection failed"
                _connectionState.value = ConnectionState.Error(err)
                appendConsole("Connection failed: $err")
            }
        }
    }

    fun disconnect() {
        AdbManager.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        AdbConnectionService.stop(getApplication())
        appendConsole("Disconnected from device.")
    }

    fun deleteSavedDevice(device: SavedDevice) {
        prefs.removeDevice(device)
        loadSavedDevices()
    }

    // Console / Terminal
    fun executeCommand(command: String) {
        if (command.isBlank()) return
        appendConsole("$ $command")
        viewModelScope.launch {
            val output = AdbManager.executeShell(command)
            appendConsole(output)
        }
    }

    fun appendConsole(text: String) {
        _consoleOutput.value = _consoleOutput.value + "\n" + text
    }

    fun clearConsole() {
        _consoleOutput.value = "# banADB Terminal Cleared.\n"
    }

    // Dashboard
    fun refreshDashboard() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _dashboardData.value = AdbManager.fetchDashboard()
            _isRefreshing.value = false
        }
    }

    // File Manager
    fun navigateToPath(path: String) {
        _currentRemotePath.value = path
        refreshFiles()
    }

    fun refreshFiles() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _remoteFiles.value = AdbManager.listRemoteFiles(_currentRemotePath.value)
            _isRefreshing.value = false
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val fullPath = "${_currentRemotePath.value.trimEnd('/')}/$name"
            if (AdbManager.createRemoteDir(fullPath)) {
                refreshFiles()
            }
        }
    }

    fun deleteFile(file: RemoteFile) {
        viewModelScope.launch {
            if (AdbManager.deleteRemoteFile(file.path)) {
                refreshFiles()
            }
        }
    }

    // App Manager
    fun refreshApps(includeSystem: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _installedApps.value = AdbManager.fetchInstalledApps(includeSystem)
            _isRefreshing.value = false
        }
    }

    fun launchApp(pkg: String) {
        viewModelScope.launch {
            AdbManager.launchApp(pkg)
        }
    }

    fun forceStopApp(pkg: String) {
        viewModelScope.launch {
            AdbManager.forceStopApp(pkg)
        }
    }

    fun clearAppData(pkg: String) {
        viewModelScope.launch {
            AdbManager.clearAppData(pkg)
        }
    }

    fun uninstallApp(pkg: String) {
        viewModelScope.launch {
            if (AdbManager.uninstallApp(pkg)) {
                refreshApps()
            }
        }
    }

    fun installApkFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            _operationStatus.value = "Transferring APK to remote device..."
            try {
                val tempFile = File(context.cacheDir, "install_${System.currentTimeMillis()}.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                _operationStatus.value = "Executing remote install..."
                val result = AdbManager.installApk(tempFile)
                tempFile.delete()
                result.onSuccess {
                    _operationStatus.value = "APK installed successfully!"
                    refreshApps()
                }.onFailure { err ->
                    _operationStatus.value = "Install failed: ${err.message}"
                }
            } catch (e: Exception) {
                _operationStatus.value = "Failed: ${e.message}"
            } finally {
                _isOperationLoading.value = false
            }
        }
    }

    fun extractApk(pkg: String, label: String, context: Context) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            _operationStatus.value = "Extracting $label APK..."
            try {
                val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "banADB")
                val result = AdbManager.extractApk(pkg, label, destDir)
                result.onSuccess { file ->
                    _operationStatus.value = "APK extracted to Downloads: ${file.name}"
                }.onFailure { err ->
                    _operationStatus.value = "Extract failed: ${err.message}"
                }
            } catch (e: Exception) {
                _operationStatus.value = "Failed: ${e.message}"
            } finally {
                _isOperationLoading.value = false
            }
        }
    }

    // Process Manager
    fun refreshProcesses() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _processes.value = AdbManager.fetchProcesses()
            _isRefreshing.value = false
        }
    }

    fun killProcess(pid: String) {
        viewModelScope.launch {
            if (AdbManager.killProcess(pid)) {
                refreshProcesses()
            }
        }
    }

    // Notifications
    private var notificationSyncJob: kotlinx.coroutines.Job? = null

    fun refreshNotifications() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _notifications.value = AdbManager.fetchNotifications()
            } catch (e: Exception) {
                Log.e("AdbViewModel", "refreshNotifications error", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleNotificationSync(enabled: Boolean) {
        _isNotificationSyncEnabled.value = enabled
        notificationSyncJob?.cancel()
        if (enabled) {
            notificationSyncJob = viewModelScope.launch {
                while (isActive && _isNotificationSyncEnabled.value) {
                    if (AdbManager.isConnected) {
                        try {
                            _notifications.value = AdbManager.fetchNotifications()
                        } catch (e: Exception) {
                            Log.e("AdbViewModel", "Notification sync loop error", e)
                        }
                    }
                    kotlinx.coroutines.delay(5000)
                }
            }
        }
    }

    fun dismissNotification(item: NotificationItem) {
        viewModelScope.launch {
            val ok = AdbManager.dismissNotification(item.key)
            if (ok) {
                _notifications.value = _notifications.value.filter { it.id != item.id && it.key != item.key }
            }
        }
    }

    // Remote Gallery
    fun refreshGallery() {
        viewModelScope.launch {
            _isGalleryLoading.value = true
            try {
                _galleryMedia.value = AdbManager.fetchRemoteGalleryMedia()
            } catch (e: Exception) {
                Log.e("AdbViewModel", "refreshGallery error", e)
            } finally {
                _isGalleryLoading.value = false
            }
        }
    }

    fun deleteRemoteMedia(item: RemoteMediaItem, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = AdbManager.deleteRemoteMedia(item.path)
            if (success) {
                _galleryMedia.value = _galleryMedia.value.filter { it.path != item.path }
            }
            onComplete(success)
        }
    }

    fun downloadMediaToLocal(
        context: Context,
        item: RemoteMediaItem,
        onProgress: (String) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onProgress("Downloading ${item.name}...")
                val subDir = if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                val publicDir = File(Environment.getExternalStoragePublicDirectory(subDir), "banADB")
                if (!publicDir.exists()) publicDir.mkdirs()
                val targetFile = File(publicDir, item.name)

                val success = AdbManager.pullFile(item.path, targetFile)
                if (success && targetFile.exists() && targetFile.length() > 0) {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(if (item.isVideo) "video/*" else "image/*"),
                        null
                    )
                    onComplete(true, targetFile.absolutePath)
                } else {
                    onComplete(false, "Download failed or file empty")
                }
            } catch (e: Exception) {
                Log.e("AdbViewModel", "downloadMediaToLocal error", e)
                onComplete(false, e.localizedMessage ?: "Download failed")
            }
        }
    }

    private val thumbnailMemoryCache = LruCache<String, Bitmap>(100)
    private val thumbMutex = Mutex()

    fun getCachedThumbnail(path: String, size: Long): Bitmap? {
        val cacheKey = "${path}_$size"
        return thumbnailMemoryCache.get(cacheKey)
    }

    suspend fun loadThumbnail(context: Context, item: RemoteMediaItem): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${item.path}_${item.size}"
        thumbnailMemoryCache.get(cacheKey)?.let { return@withContext it }

        val thumbsDir = File(context.cacheDir, "gallery_thumbs")
        if (!thumbsDir.exists()) thumbsDir.mkdirs()
        val thumbFile = File(thumbsDir, "thumb_${item.path.hashCode()}_${item.size}.jpg")

        if (thumbFile.exists() && thumbFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (bmp != null) {
                    thumbnailMemoryCache.put(cacheKey, bmp)
                    return@withContext bmp
                }
            } catch (e: Exception) {
                thumbFile.delete()
            }
        }

        // Fetch from remote device sequentially using Mutex
        thumbMutex.withLock {
            // Re-check after acquiring lock in case another coroutine fetched it
            thumbnailMemoryCache.get(cacheKey)?.let { return@withLock it }
            if (thumbFile.exists() && thumbFile.length() > 0) {
                val bmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (bmp != null) {
                    thumbnailMemoryCache.put(cacheKey, bmp)
                    return@withLock bmp
                }
            }

            if (!AdbManager.isConnected) return@withLock null

            val tempDir = File(context.cacheDir, "gallery_tmp")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, "tmp_${item.path.hashCode()}_${System.currentTimeMillis()}")

            try {
                val success = AdbManager.pullFile(item.path, tempFile)
                if (!success || !tempFile.exists() || tempFile.length() == 0L) {
                    return@withLock null
                }

                var extractedBitmap: Bitmap? = null

                if (item.isVideo) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(tempFile.absolutePath)
                        extractedBitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.frameAtTime
                        retriever.release()
                    } catch (e: Exception) {
                        Log.w("AdbViewModel", "Video frame extraction failed for ${item.path}", e)
                    }
                } else {
                    try {
                        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(tempFile.absolutePath, boundsOpts)
                        val sampleSize = calculateInSampleSize(boundsOpts, 360, 360)
                        val decodeOpts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        extractedBitmap = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOpts)
                    } catch (e: Exception) {
                        Log.w("AdbViewModel", "Image decoding failed for ${item.path}", e)
                    }
                }

                // Delete raw temp file to save storage
                tempFile.delete()

                if (extractedBitmap != null) {
                    try {
                        FileOutputStream(thumbFile).use { out ->
                            extractedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                    } catch (e: Exception) {
                        Log.w("AdbViewModel", "Failed to cache thumb on disk", e)
                    }
                    thumbnailMemoryCache.put(cacheKey, extractedBitmap)
                    extractedBitmap
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("AdbViewModel", "loadThumbnail error for ${item.path}", e)
                null
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    fun loadPreviewFile(
        context: Context,
        item: RemoteMediaItem,
        onLoaded: (File?) -> Unit
    ) {
        viewModelScope.launch {
            val cacheDir = File(context.cacheDir, "gallery_preview")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val localCache = File(cacheDir, "${item.name.hashCode()}_${item.name}")
            if (localCache.exists() && localCache.length() == item.size) {
                onLoaded(localCache)
                return@launch
            }
            val ok = thumbMutex.withLock {
                AdbManager.pullFile(item.path, localCache)
            }
            if (ok && localCache.exists() && localCache.length() > 0) {
                onLoaded(localCache)
            } else {
                onLoaded(null)
            }
        }
    }

    // Remote Control (D-Pad, Touchpad, KeyEvents)
    fun sendKeyevent(keyCode: Int) {
        viewModelScope.launch {
            AdbManager.sendKeyevent(keyCode)
        }
    }

    fun sendTap(x: Float, y: Float) {
        viewModelScope.launch {
            AdbManager.sendTap(x, y)
        }
    }

    fun sendSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Int = 300) {
        viewModelScope.launch {
            AdbManager.sendSwipe(x1, y1, x2, y2, durationMs)
        }
    }

    fun sendText(text: String) {
        viewModelScope.launch {
            AdbManager.sendText(text)
        }
    }

    // Clipboard & Toggles
    fun syncClipboardToRemote(text: String, injectInput: Boolean = false, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val clipSuccess = AdbManager.setClipboard(text)
            val injectSuccess = if (injectInput) AdbManager.sendText(text) else true
            onResult?.invoke(clipSuccess || injectSuccess)
            refreshDashboard()
        }
    }

    fun syncClipboardFromRemote(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val text = AdbManager.getClipboard()
            onResult(text)
        }
    }

    fun triggerRemoteSearch() {
        viewModelScope.launch {
            AdbManager.triggerSearch()
        }
    }

    fun toggleWifi(enable: Boolean) {
        viewModelScope.launch {
            AdbManager.toggleWifi(enable)
        }
    }

    fun toggleBluetooth(enable: Boolean) {
        viewModelScope.launch {
            AdbManager.toggleBluetooth(enable)
        }
    }

    fun rebootDevice(mode: String = "") {
        viewModelScope.launch {
            AdbManager.reboot(mode)
            disconnect()
        }
    }

    // Scripts
    fun saveScript(title: String, command: String, category: String) {
        prefs.saveScript(title, command, category)
        loadScripts()
    }

    fun deleteScript(id: String) {
        prefs.deleteScript(id)
        loadScripts()
    }

    fun runScript(script: ScriptItem) {
        executeCommand(script.command)
    }

    // Screen Mirroring / Screenshot
    fun captureScreenshot(onCaptured: ((Bitmap?) -> Unit)? = null) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val bmp = AdbManager.captureScreenshot()
            _screenshotBitmap.value = bmp
            _isRefreshing.value = false
            onCaptured?.invoke(bmp)
        }
    }

    fun saveScreenshotToGallery(context: Context, bitmap: Bitmap, onSaved: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = AdbManager.saveScreenshotToGallery(context, bitmap)
            onSaved(uri)
        }
    }

    // File Manager Downloads & Preview Cache
    fun pullFileToCache(file: RemoteFile, context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isOperationLoading.value = true
            try {
                val previewDir = File(context.cacheDir, "previews")
                if (!previewDir.exists()) previewDir.mkdirs()
                val localFile = File(previewDir, file.name)
                if (localFile.exists()) localFile.delete()
                val success = AdbManager.pullFile(file.path, localFile)
                if (success && localFile.exists()) {
                    onResult(localFile)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            } finally {
                _isOperationLoading.value = false
            }
        }
    }

    fun saveFileToDownloads(file: RemoteFile, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isOperationLoading.value = true
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, file.name)
                val success = AdbManager.pullFile(file.path, targetFile)
                onResult(success)
            } catch (e: Exception) {
                onResult(false)
            } finally {
                _isOperationLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.stopDiscovery()
    }
}
