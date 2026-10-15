package com.baner.banadb.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.baner.banadb.data.model.DiscoveryDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class MdnsDiscoveryManager(context: Context) {
    private val TAG = "MdnsDiscoveryManager"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredDevices = MutableStateFlow<List<DiscoveryDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveryDevice>> = _discoveredDevices.asStateFlow()

    private var pairingListener: NsdManager.DiscoveryListener? = null
    private var connectListener: NsdManager.DiscoveryListener? = null
    private var scanJob: Job? = null

    fun startDiscovery() {
        stopDiscovery()
        _discoveredDevices.value = emptyList()

        if (nsdManager == null) {
            Log.e(TAG, "NsdManager is null on this device")
            return
        }

        // 1. Discover Wireless Pairing Service (Android 11+)
        pairingListener = createListener("_adb-tls-pairing._tcp.")
        try {
            nsdManager.discoverServices("_adb-tls-pairing._tcp.", NsdManager.PROTOCOL_DNS_SD, pairingListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering pairing services", e)
        }

        // 2. Discover Connect Service
        connectListener = createListener("_adb-tls-connect._tcp.")
        try {
            nsdManager.discoverServices("_adb-tls-connect._tcp.", NsdManager.PROTOCOL_DNS_SD, connectListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering connect services", e)
        }
    }

    private fun createListener(serviceType: String) = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started: $regType")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "Service found: ${service.serviceName}")
            try {
                nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress ?: return
                        val port = serviceInfo.port
                        val name = serviceInfo.serviceName ?: "Android Device"
                        addDevice(
                            DiscoveryDevice(
                                ip = host,
                                port = port,
                                name = name,
                                serviceType = serviceType,
                                isPaired = serviceType.contains("connect")
                            )
                        )
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error during resolve call", e)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d(TAG, "Service lost: ${service.serviceName}")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery start failed: $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery stop failed: $errorCode")
        }
    }

    private fun addDevice(device: DiscoveryDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.ip == device.ip && it.port == device.port }
        if (existingIndex >= 0) {
            current[existingIndex] = device
        } else {
            current.add(device)
        }
        _discoveredDevices.value = current
    }

    fun scanSubnetLegacy(localIp: String, scope: CoroutineScope) {
        scanJob?.cancel()
        scanJob = scope.launch(Dispatchers.IO) {
            val prefix = localIp.substringBeforeLast('.') + "."
            for (i in 1..254) {
                if (!isActive) break
                val targetIp = "$prefix$i"
                launch {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(targetIp, 5555), 400)
                        socket.close()
                        addDevice(
                            DiscoveryDevice(
                                ip = targetIp,
                                port = 5555,
                                name = "Device ($targetIp:5555)",
                                serviceType = "legacy-5555",
                                isPaired = true
                            )
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun stopDiscovery() {
        scanJob?.cancel()
        scanJob = null
        try {
            pairingListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
        try {
            connectListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
        pairingListener = null
        connectListener = null
    }
}
