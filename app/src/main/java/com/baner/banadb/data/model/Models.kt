package com.baner.banadb.data.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Connecting(val message: String) : ConnectionState()
    data class Connected(val deviceName: String, val ip: String, val port: Int) : ConnectionState()
    data class Pairing(val message: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class DiscoveryDevice(
    val ip: String,
    val port: Int,
    val name: String,
    val serviceType: String,
    val isPaired: Boolean = false
)

data class SavedDevice(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val pairingPort: Int = 0,
    val lastConnected: Long = System.currentTimeMillis()
)

data class RemoteFile(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val permissions: String = "",
    val modified: String = ""
)

data class RemoteAppInfo(
    val packageName: String,
    val label: String,
    val versionName: String = "",
    val versionCode: Long = 0,
    val isSystem: Boolean = false,
    val sourceDir: String = ""
)

data class DeviceDashboardData(
    val model: String = "Unknown",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val sdkVersion: String = "",
    val batteryLevel: Int = -1,
    val batteryStatus: String = "Unknown",
    val batteryHealth: String = "Good",
    val batteryTemp: Float = 0f,
    val batteryVoltage: Int = 0,
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400,
    val clipboardText: String = "",
    val ipAddress: String = "",
    val uptime: String = ""
)

data class RunningProcess(
    val pid: String,
    val user: String,
    val name: String,
    val cpu: String = "",
    val mem: String = ""
)

data class NotificationItem(
    val pkg: String,
    val title: String,
    val text: String,
    val subText: String = "",
    val postTime: String = ""
)

data class ScriptItem(
    val id: String,
    val title: String,
    val command: String,
    val category: String = "Custom",
    val isPreset: Boolean = false
)
