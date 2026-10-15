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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BanaADB Connection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps wireless ADB connection active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "banadb_connection_channel"
    }
}
