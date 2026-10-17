package com.baner.banadb.service

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.baner.banadb.BanaAdbApplication
import com.baner.banadb.MainActivity
import com.baner.banadb.R
import com.baner.banadb.adb.AdbManager
import com.baner.banadb.adb.NetworkUtils
import com.baner.banadb.data.model.SavedDevice
import com.baner.banadb.data.repository.DevicePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocalPairingReceiver : BroadcastReceiver() {

    private val TAG = "LocalPairingReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SUBMIT_CODE) return

        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val rawInput = remoteInput?.getCharSequence(KEY_PAIRING_CODE)?.toString()?.trim() ?: ""

        if (rawInput.isBlank()) {
            notifyUser(context, context.getString(R.string.notif_pair_code_empty_title), context.getString(R.string.notif_pair_code_empty_desc))
            return
        }

        var port = LocalPairingStateHolder.discoveredPairingPort.value ?: 0
        var code = rawInput

        // Support manual format "PORT:CODE" or "PORT CODE" if mDNS hasn't discovered the port
        if (rawInput.contains(":") || rawInput.contains(" ")) {
            val delimiter = if (rawInput.contains(":")) ":" else " "
            val parts = rawInput.split(delimiter, limit = 2).map { it.trim() }
            val parsedPort = parts.getOrNull(0)?.toIntOrNull()
            val parsedCode = parts.getOrNull(1)
            if (parsedPort != null && !parsedCode.isNullOrBlank()) {
                port = parsedPort
                code = parsedCode
            }
        }

        if (port <= 0) {
            LocalPairingStateHolder.statusMessage.value = context.getString(R.string.local_pairing_status_port_unknown)
            notifyUser(
                context,
                context.getString(R.string.notif_pair_code_empty_title),
                context.getString(R.string.notif_pair_code_empty_desc)
            )
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LocalPairingStateHolder.isPairingInProgress.value = true
                LocalPairingStateHolder.statusMessage.value = context.getString(R.string.local_pairing_status_in_progress, port, code)
                notifyUser(context, context.getString(R.string.notif_pairing_in_progress_title), context.getString(R.string.notif_pairing_in_progress_desc, port))

                val candidateIps = mutableListOf<String>()
                val prefIp = NetworkUtils.getPreferredLocalIp()
                candidateIps.add(prefIp)
                if (prefIp != "127.0.0.1") candidateIps.add("127.0.0.1")

                var pairSuccess = false
                var successfulIp = prefIp
                var lastErrMsg = context.getString(R.string.notif_unknown_error)

                for (ip in candidateIps) {
                    Log.d(TAG, "Attempting local pair on $ip:$port...")
                    val result = AdbManager.pair(ip, port, code)
                    if (result.isSuccess) {
                        pairSuccess = true
                        successfulIp = ip
                        break
                    } else {
                        lastErrMsg = result.exceptionOrNull()?.message ?: context.getString(R.string.notif_handshake_failed)
                        Log.w(TAG, "Pair failed on $ip:$port: $lastErrMsg")
                    }
                }

                if (pairSuccess) {
                    LocalPairingStateHolder.statusMessage.value = context.getString(R.string.local_pairing_status_succeeded_connecting)
                    notifyUser(
                        context,
                        context.getString(R.string.notif_pairing_succeeded_title),
                        context.getString(R.string.notif_pairing_authenticating_desc)
                    )

                    delay(800)

                    var connectPort = LocalPairingStateHolder.discoveredConnectPort.value
                    if (connectPort == null) {
                        for (i in 1..5) {
                            delay(300)
                            connectPort = LocalPairingStateHolder.discoveredConnectPort.value
                            if (connectPort != null) break
                        }
                    }
                    val targetConnectPort = connectPort ?: 5555
                    Log.d(TAG, "Attempting connect on $successfulIp:$targetConnectPort...")
                    var connectResult = AdbManager.connect(successfulIp, targetConnectPort)
                    var activeConnectPort = targetConnectPort

                    if (!connectResult.isSuccess && targetConnectPort != 5555) {
                        Log.d(TAG, "Trying fallback to port 5555...")
                        val fallback = AdbManager.connect(successfulIp, 5555)
                        if (fallback.isSuccess) {
                            connectResult = fallback
                            activeConnectPort = 5555
                        }
                    }

                    if (connectResult.isSuccess) {
                        val devName = try { AdbManager.fetchDashboard().model } catch (_: Exception) { context.getString(R.string.local_device_fallback) }
                        LocalPairingStateHolder.statusMessage.value = context.getString(R.string.local_pairing_status_connected, devName)

                        // 1. Save device in DevicePreferences so it displays in Saved Devices on Home
                        val prefs = DevicePreferences(context)
                        val saved = SavedDevice(
                            id = java.util.UUID.randomUUID().toString(),
                            name = devName,
                            ip = successfulIp,
                            port = activeConnectPort,
                            lastConnected = System.currentTimeMillis()
                        )
                        prefs.saveDevice(saved)

                        // 2. Start ongoing foreground connection service
                        AdbConnectionService.start(context, devName, "$successfulIp:$activeConnectPort")

                        // 3. Post notification with clear "Open App" action button (dedicated ID)
                        notifyConnected(context, devName, "$successfulIp:$activeConnectPort")

                        // 4. Auto-Return to App if overlay permission is granted
                        if (Settings.canDrawOverlays(context)) {
                            try {
                                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    action = Intent.ACTION_MAIN
                                    addCategory(Intent.CATEGORY_LAUNCHER)
                                }
                                context.startActivity(openAppIntent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Could not auto-return to app via overlay", e)
                            }
                        }

                        // Stop pairing discovery service
                        delay(1000)
                        LocalPairingService.stop(context)
                    } else {
                        LocalPairingStateHolder.statusMessage.value = context.getString(R.string.local_pairing_status_paired_open_app)
                        notifyUser(
                            context,
                            context.getString(R.string.notif_paired_success_title),
                            context.getString(R.string.notif_paired_success_desc)
                        )
                    }
                } else {
                    LocalPairingStateHolder.lastError.value = lastErrMsg
                    LocalPairingStateHolder.statusMessage.value = context.getString(R.string.local_pairing_status_failed, lastErrMsg)
                    notifyUser(
                        context,
                        context.getString(R.string.notif_pairing_failed_title),
                        context.getString(R.string.notif_pairing_failed_desc, lastErrMsg)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in pairing receiver", e)
                LocalPairingStateHolder.lastError.value = e.message
                notifyUser(context, context.getString(R.string.notif_pairing_error_title), e.message ?: context.getString(R.string.notif_pairing_unexpected_error))
            } finally {
                LocalPairingStateHolder.isPairingInProgress.value = false
                pendingResult.finish()
            }
        }
    }

    private fun notifyConnected(context: Context, devName: String, sessionInfo: String) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            205,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, BanaAdbApplication.CHANNEL_PAIRING_ID)
            .setContentTitle(context.getString(R.string.notif_connected_local_title, devName))
            .setContentText(context.getString(R.string.notif_connected_local_desc))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notif_connected_local_big, sessionInfo)))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_directions, context.getString(R.string.notif_action_open_app), openAppPendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.notify(NOTIFICATION_ID_CONNECTED, notification)
    }

    private fun notifyUser(context: Context, title: String, content: String) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            200,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val remoteInput = RemoteInput.Builder(KEY_PAIRING_CODE)
            .setLabel(context.getString(R.string.notif_pair_remote_input_label))
            .build()

        val replyIntent = Intent(context, LocalPairingReceiver::class.java).apply {
            action = ACTION_SUBMIT_CODE
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            202,
            replyIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_input_add,
            context.getString(R.string.notif_action_enter_code),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(context, BanaAdbApplication.CHANNEL_PAIRING_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(appPendingIntent)
            .addAction(replyAction)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.notify(LocalPairingService.NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_SUBMIT_CODE = "com.baner.banadb.action.SUBMIT_PAIRING_CODE"
        const val KEY_PAIRING_CODE = "key_pairing_code"
        const val NOTIFICATION_ID_CONNECTED = 1003
    }
}
