package com.baner.banadb.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.baner.banadb.BanaAdbApplication
import com.baner.banadb.MainActivity
import com.baner.banadb.R
import com.baner.banadb.adb.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow

object LocalPairingStateHolder {
    val isRunning = MutableStateFlow(false)
    val discoveredPairingIp = MutableStateFlow<String?>(null)
    val discoveredPairingPort = MutableStateFlow<Int?>(null)
    val discoveredConnectPort = MutableStateFlow<Int?>(null)
    val statusMessage = MutableStateFlow("")
    val isPairingInProgress = MutableStateFlow(false)
    val lastError = MutableStateFlow<String?>(null)

    fun reset() {
        discoveredPairingIp.value = null
        discoveredPairingPort.value = null
        discoveredConnectPort.value = null
        statusMessage.value = ""
        isPairingInProgress.value = false
        lastError.value = null
    }
}

class LocalPairingService : Service() {

    private val TAG = "LocalPairingService"
    private var nsdManager: NsdManager? = null
    private var pairingListener: NsdManager.DiscoveryListener? = null
    private var connectListener: NsdManager.DiscoveryListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        nsdManager = getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopDiscovery()
            LocalPairingStateHolder.isRunning.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        LocalPairingStateHolder.reset()
        LocalPairingStateHolder.isRunning.value = true
        LocalPairingStateHolder.statusMessage.value = getString(R.string.local_pairing_status_listening)

        val notification = buildNotification(
            title = getString(R.string.notif_pairing_active_title),
            content = getString(R.string.notif_pairing_active_desc)
        )
        startForeground(NOTIFICATION_ID, notification)

        startDiscovery()

        return START_STICKY
    }

    private fun startDiscovery() {
        stopDiscovery()

        if (nsdManager == null) {
            Log.e(TAG, "NsdManager is null on this device")
            LocalPairingStateHolder.statusMessage.value = getString(R.string.local_pairing_status_nsd_unavailable)
            return
        }

        // 1. Discover Wireless Pairing Service (_adb-tls-pairing._tcp.)
        pairingListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Pairing discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Pairing service found: ${service.serviceName}")
                try {
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "Pairing service resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val port = serviceInfo.port
                            val host = serviceInfo.host?.hostAddress ?: NetworkUtils.getPreferredLocalIp()
                            Log.d(TAG, "Resolved pairing service: $host:$port")

                            LocalPairingStateHolder.discoveredPairingIp.value = host
                            LocalPairingStateHolder.discoveredPairingPort.value = port
                            LocalPairingStateHolder.statusMessage.value = getString(R.string.local_pairing_status_detected, port)

                            updateNotification(
                                title = getString(R.string.notif_pairing_detected_title, port),
                                content = getString(R.string.notif_pairing_detected_desc)
                            )
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error resolving pairing service", e)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Pairing service lost: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Pairing discovery start failed: $errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager?.discoverServices("_adb-tls-pairing._tcp.", NsdManager.PROTOCOL_DNS_SD, pairingListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting pairing discovery", e)
        }

        // 2. Discover Connect Service (_adb-tls-connect._tcp.)
        connectListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Connect service found: ${service.serviceName}")
                try {
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val port = serviceInfo.port
                            Log.d(TAG, "Resolved connect service port: $port")
                            LocalPairingStateHolder.discoveredConnectPort.value = port
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error resolving connect service", e)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager?.discoverServices("_adb-tls-connect._tcp.", NsdManager.PROTOCOL_DNS_SD, connectListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting connect discovery", e)
        }
    }

    private fun stopDiscovery() {
        try {
            pairingListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
        try {
            connectListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
        pairingListener = null
        connectListener = null
    }

    private fun buildNotification(title: String, content: String): Notification {
        // Tap notification to open app
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            200,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Open Developer Options directly
        val devOptionsIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val devOptionsPendingIntent = PendingIntent.getActivity(
            this,
            201,
            devOptionsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: RemoteInput for direct pairing code entry
        val remoteInput = RemoteInput.Builder(LocalPairingReceiver.KEY_PAIRING_CODE)
            .setLabel(getString(R.string.notif_pair_remote_input_label))
            .build()

        val replyIntent = Intent(this, LocalPairingReceiver::class.java).apply {
            action = LocalPairingReceiver.ACTION_SUBMIT_CODE
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            202,
            replyIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_input_add,
            getString(R.string.notif_action_enter_code),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Action: Stop Service
        val stopIntent = Intent(this, LocalPairingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            203,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, BanaAdbApplication.CHANNEL_PAIRING_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_preferences, getString(R.string.notif_action_dev_options), devOptionsPendingIntent)
            .addAction(replyAction)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notif_action_stop), stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopDiscovery()
        LocalPairingStateHolder.isRunning.value = false
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val ACTION_START = "com.baner.banadb.service.local_pairing.START"
        const val ACTION_STOP = "com.baner.banadb.service.local_pairing.STOP"

        fun start(context: Context) {
            val intent = Intent(context, LocalPairingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocalPairingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
