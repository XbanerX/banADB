package com.baner.banadb.adb

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Retrieves all active non-loopback IPv4 addresses assigned to the device.
     */
    fun getLocalIpv4Addresses(): List<String> {
        val addresses = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        addr.hostAddress?.let { ip ->
                            if (ip.isNotBlank() && !addresses.contains(ip)) {
                                addresses.add(ip)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return addresses
    }

    /**
     * Returns the best candidate IPv4 address for local network/Wi-Fi operations,
     * prioritizing standard Wi-Fi LAN subnets (192.168.x.x, 10.x.x.x, 172.x.x.x).
     * Falls back to 127.0.0.1 if no external network interface is found.
     */
    fun getPreferredLocalIp(): String {
        val addrs = getLocalIpv4Addresses()
        return addrs.firstOrNull { it.startsWith("192.168.") }
            ?: addrs.firstOrNull { it.startsWith("10.") }
            ?: addrs.firstOrNull { it.startsWith("172.") }
            ?: addrs.firstOrNull()
            ?: "127.0.0.1"
    }
}
