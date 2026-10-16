package com.baner.banadb

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BanaAdbApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        com.baner.banadb.adb.AdbManager.init(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val connectionChannel = NotificationChannel(
                CHANNEL_ID,
                "banADB Connection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps wireless ADB connection active"
            }

            val pairingChannel = NotificationChannel(
                CHANNEL_PAIRING_ID,
                "banADB Local Pairing Helper",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Interactive notifications for Wireless Debugging local pairing"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(connectionChannel)
            manager?.createNotificationChannel(pairingChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "banadb_connection_channel"
        const val CHANNEL_PAIRING_ID = "banadb_local_pairing_channel"
    }
}
