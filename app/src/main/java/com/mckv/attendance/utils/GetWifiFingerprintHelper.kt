package com.mckv.attendance.utils

import android.content.Context
import android.net.wifi.WifiManager
import org.json.JSONArray
import org.json.JSONObject

fun getWifiFingerPrint(context: Context): JSONArray {
    val jsonArray = JSONArray()
    try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val scanResults = wifiManager.scanResults

        for (result in scanResults) {
            val obj = JSONObject().apply {
                put("SSID", result.SSID ?: "")
                put("BSSID", result.BSSID ?: "")
                put("level", result.level)  // RSSI in dBm
            }
            jsonArray.put(obj)
        }
    } catch (e: SecurityException) {
        // Permission missing
        val err = JSONObject().apply {
            put("SSID", "Permission not granted")
            put("BSSID", "")
            put("level", 0)
        }
        jsonArray.put(err)
    }
    return jsonArray
}
