package com.baner.banadb.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.baner.banadb.data.model.SavedDevice
import com.baner.banadb.data.model.ScriptItem
import org.json.JSONArray
import org.json.JSONObject

class DevicePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("banadb_preferences", Context.MODE_PRIVATE)

    fun getSavedDevices(): List<SavedDevice> {
        val raw = prefs.getString(KEY_SAVED_DEVICES, null) ?: return emptyList()
        val list = mutableListOf<SavedDevice>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SavedDevice(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "Android Device"),
                        ip = obj.getString("ip"),
                        port = obj.getInt("port"),
                        pairingPort = obj.optInt("pairingPort", 0),
                        lastConnected = obj.optLong("lastConnected", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveDevice(device: SavedDevice) {
        val current = getSavedDevices().toMutableList()
        val index = current.indexOfFirst { it.ip == device.ip }
        if (index >= 0) {
            current[index] = device
        } else {
            current.add(device)
        }
        persistDevices(current)
    }

    fun removeDevice(device: SavedDevice) {
        val current = getSavedDevices().toMutableList()
        current.removeAll { it.ip == device.ip }
        persistDevices(current)
    }

    private fun persistDevices(list: List<SavedDevice>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("ip", item.ip)
                put("port", item.port)
                put("pairingPort", item.pairingPort)
                put("lastConnected", item.lastConnected)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_DEVICES, jsonArray.toString()).apply()
    }

    fun getScripts(): List<ScriptItem> {
        val raw = prefs.getString(KEY_CUSTOM_SCRIPTS, null)
        val list = mutableListOf<ScriptItem>()
        // Add default presets
        list.addAll(defaultPresets())
        if (!raw.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(raw)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ScriptItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            command = obj.getString("command"),
                            category = obj.optString("category", "Custom"),
                            isPreset = false
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    fun saveScript(title: String, command: String, category: String = "Custom") {
        val custom = getScripts().filter { !it.isPreset }.toMutableList()
        val item = ScriptItem(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            command = command,
            category = category,
            isPreset = false
        )
        custom.add(item)
        val jsonArray = JSONArray()
        for (s in custom) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("command", s.command)
                put("category", s.category)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_SCRIPTS, jsonArray.toString()).apply()
    }

    fun deleteScript(id: String) {
        val custom = getScripts().filter { !it.isPreset && it.id != id }
        val jsonArray = JSONArray()
        for (s in custom) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("command", s.command)
                put("category", s.category)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_SCRIPTS, jsonArray.toString()).apply()
    }

    private fun defaultPresets(): List<ScriptItem> = listOf(
        ScriptItem("p1", "Device Properties", "getprop | grep -E 'ro.product|ro.build'", "Info", true),
        ScriptItem("p2", "Battery Health", "dumpsys battery", "System", true),
        ScriptItem("p3", "List Third-Party Apps", "pm list packages -3", "Apps", true),
        ScriptItem("p4", "Screen Dimensions & Density", "wm size && wm density", "Display", true),
        ScriptItem("p5", "Active Network Interfaces", "ip addr show", "Network", true),
        ScriptItem("p6", "Top CPU Processes", "top -n 1 -b -m 10", "System", true),
        ScriptItem("p7", "Reboot Device", "reboot", "System", true),
        ScriptItem("p8", "Clear App Cache", "pm trim-caches 999M", "Apps", true)
    )

    companion object {
        private const val KEY_SAVED_DEVICES = "key_saved_devices"
        private const val KEY_CUSTOM_SCRIPTS = "key_custom_scripts"
    }
}
