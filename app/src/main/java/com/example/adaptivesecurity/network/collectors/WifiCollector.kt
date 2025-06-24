package com.example.adaptivesecurity.network.collectors

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.adaptivesecurity.network.models.SecurityType
import com.example.adaptivesecurity.network.models.WifiNetwork

class WifiCollector(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun getCurrentNetwork(): WifiNetwork? {
        if (!hasRequiredPermissions()) {
            return null
        }

        return try {
            val connectionInfo = wifiManager.connectionInfo ?: return null
            val ssid = connectionInfo.ssid?.removeSurrounding("\"") ?: return null
            val bssid = connectionInfo.bssid ?: return null

            val securityType = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    mapSecurityTypeFromWifiInfo(connectionInfo.currentSecurityType)
                }
                else -> {
                    val scanResults = try {
                        wifiManager.scanResults
                    } catch (se: SecurityException) {
                        emptyList()
                    }
                    val scanResult = scanResults.find { it.BSSID == bssid }
                    scanResult?.let { mapSecurityTypeFromScanResult(it) } ?: SecurityType.UNKNOWN
                }
            }

            WifiNetwork(
                ssid = ssid,
                bssid = bssid,
                securityType = securityType,
                signalStrength = connectionInfo.rssi,
                isCurrent = true
            )
        } catch (se: SecurityException) {
            null
        }
    }

    fun scanNetworks(): List<WifiNetwork> {
        if (!hasRequiredPermissions()) {
            return emptyList()
        }

        return try {
            wifiManager.scanResults.map { scanResult ->
                WifiNetwork(
                    ssid = scanResult.SSID,
                    bssid = scanResult.BSSID,
                    securityType = mapSecurityTypeFromScanResult(scanResult),
                    signalStrength = scanResult.level,
                    isCurrent = false
                )
            }
        } catch (se: SecurityException) {
            emptyList()
        }
    }

    private fun mapSecurityTypeFromWifiInfo(securityType: Int): SecurityType {
        return when (securityType) {
            WifiInfo.SECURITY_TYPE_SAE -> SecurityType.WPA3_SAE
            WifiInfo.SECURITY_TYPE_EAP,
            WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE -> SecurityType.EAP
            WifiInfo.SECURITY_TYPE_PSK -> SecurityType.WPA2_PSK
            WifiInfo.SECURITY_TYPE_OPEN -> SecurityType.OPEN
            WifiInfo.SECURITY_TYPE_WEP -> SecurityType.WEP
            else -> SecurityType.UNKNOWN
        }
    }

    private fun mapSecurityTypeFromScanResult(scanResult: ScanResult): SecurityType {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Use securityTypes list which is available since API 31
                val securityTypes = scanResult.securityTypes
                when {
                    securityTypes.contains(4) -> SecurityType.WPA3_SAE  // SECURITY_TYPE_SAE
                    securityTypes.contains(3) || securityTypes.contains(5) -> SecurityType.EAP  // EAP or EAP_WPA3
                    securityTypes.contains(2) -> SecurityType.WPA2_PSK  // SECURITY_TYPE_PSK
                    securityTypes.contains(0) -> SecurityType.OPEN       // SECURITY_TYPE_OPEN
                    securityTypes.contains(1) -> SecurityType.WEP        // SECURITY_TYPE_WEP
                    else -> SecurityType.UNKNOWN
                }
            } else {
                val capabilities = scanResult.capabilities.uppercase()
                when {
                    capabilities.contains("WPA3") -> SecurityType.WPA3_SAE
                    capabilities.contains("EAP") -> SecurityType.EAP
                    capabilities.contains("WPA2") || capabilities.contains("WPA") -> SecurityType.WPA2_PSK
                    capabilities.contains("WEP") -> SecurityType.WEP
                    !capabilities.contains("WPA") && !capabilities.contains("WEP") -> SecurityType.OPEN
                    else -> SecurityType.UNKNOWN
                }
            }
        } catch (e: Exception) {
            SecurityType.UNKNOWN
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions = mutableListOf(
            android.Manifest.permission.ACCESS_WIFI_STATE
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requiredPermissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
