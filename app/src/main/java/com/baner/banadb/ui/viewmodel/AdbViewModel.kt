package com.baner.banadb.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baner.banadb.adb.AdbManager
import com.baner.banadb.adb.MdnsDiscoveryManager
import com.baner.banadb.data.model.*
import com.baner.banadb.data.repository.DevicePreferences
import com.baner.banadb.service.AdbConnectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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

    private val _scripts = MutableStateFlow<List<ScriptItem>>(emptyList())
    val scripts: StateFlow<List<ScriptItem>> = _scripts.asStateFlow()

    private val _screenshotBitmap = MutableStateFlow<Bitmap?>(null)
    val screenshotBitmap: StateFlow<Bitmap?> = _screenshotBitmap.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadSavedDevices()
        loadScripts()
        appendConsole("BanaADB Terminal initialized.\nType a command or choose a suggestion below.\n")
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
        _consoleOutput.value = "# BanaADB Terminal Cleared.\n"
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
                    ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BanaADB")
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
    fun refreshNotifications() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _notifications.value = AdbManager.fetchNotifications()
            _isRefreshing.value = false
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
    fun syncClipboardToRemote(text: String) {
        viewModelScope.launch {
            AdbManager.setClipboard(text)
            refreshDashboard()
        }
    }

    fun syncClipboardFromRemote(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val text = AdbManager.getClipboard()
            onResult(text)
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
    fun captureScreenshot() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val bmp = AdbManager.captureScreenshot()
            _screenshotBitmap.value = bmp
            _isRefreshing.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.stopDiscovery()
    }
}
