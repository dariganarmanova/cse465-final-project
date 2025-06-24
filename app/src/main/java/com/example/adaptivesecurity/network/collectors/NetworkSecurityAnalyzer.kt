package com.example.adaptivesecurity.network.collectors
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.net.wifi.WifiInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.os.Build

object NetworkSecurityAnalyzer {

    // Security classification constants
    const val TRUSTED = "Trusted"
    const val UNTRUSTED = "Untrusted"
    const val UNKNOWN = "Unknown"
    const val PERMISSION_DENIED = "Permission Denied"

    /**
     * Analyzes current Wi-Fi connection security
     */
    fun analyzeCurrentNetwork(context: Context): String {
        // Check if we have required permissions
        if (!hasWifiPermissions(context)) {
            return PERMISSION_DENIED
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return try {
            val network = connectivityManager.activeNetwork ?: return UNKNOWN
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return UNKNOWN

            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return UNKNOWN
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo: WifiInfo? = wifiManager.connectionInfo

            when {
                // Modern API (Android 12+)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    when (wifiInfo?.currentSecurityType) {
                        WifiInfo.SECURITY_TYPE_SAE,
                        WifiInfo.SECURITY_TYPE_EAP,
                        WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE -> TRUSTED
                        WifiInfo.SECURITY_TYPE_OPEN -> UNTRUSTED
                        WifiInfo.SECURITY_TYPE_PSK -> TRUSTED
                        WifiInfo.SECURITY_TYPE_WEP -> UNTRUSTED
                        else -> UNKNOWN
                    }
                }
                // Legacy API
                else -> analyzeLegacySecurity(wifiManager)
            }
        } catch (se: SecurityException) {
            PERMISSION_DENIED
        }
    }

    /**
     * Scans for nearby networks and classifies them
     */
    fun analyzeAvailableNetworks(context: Context): Map<String, String> {
        // Check if we have required permissions
        if (!hasWifiPermissions(context)) {
            return emptyMap()
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        return try {
            wifiManager.scanResults.associate { scanResult ->
                scanResult.SSID to classifyScanResultSecurity(scanResult)
            }
        } catch (se: SecurityException) {
            emptyMap()
        }
    }

    private fun classifyScanResultSecurity(scanResult: ScanResult): String {
        val capabilities = scanResult.capabilities.uppercase()
        return when {
            capabilities.contains("SAE") || capabilities.contains("WPA3") -> TRUSTED
            capabilities.contains("WPA2") || capabilities.contains("WPA") -> TRUSTED
            capabilities.contains("EAP") -> TRUSTED
            capabilities.contains("WEP") -> UNTRUSTED
            !capabilities.contains("WPA") &&
                    !capabilities.contains("WEP") &&
                    !capabilities.contains("EAP") -> UNTRUSTED
            else -> UNKNOWN
        }
    }

    @Suppress("DEPRECATION")
    private fun analyzeLegacySecurity(wifiManager: WifiManager): String {
        return try {
            val connectionInfo = wifiManager.connectionInfo
            val configuredNetworks = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) null
                else wifiManager.configuredNetworks
            } catch (e: SecurityException) {
                null
            }

            val currentNetwork = configuredNetworks?.firstOrNull {
                it.networkId == connectionInfo.networkId
            }

            when {
                currentNetwork == null -> UNKNOWN
                currentNetwork.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA2_PSK) -> TRUSTED
                // Use SAE instead of WPA3_SAE
                currentNetwork.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.SAE) -> TRUSTED
                currentNetwork.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X) -> TRUSTED
                currentNetwork.wepKeys?.any { it != null } == true -> UNTRUSTED
                else -> UNKNOWN
            }
        } catch (se: SecurityException) {
            PERMISSION_DENIED
        }
    }


    // Helper function to check required permissions
    private fun hasWifiPermissions(context: Context): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE
        )

        // Location permissions required for Wi-Fi scanning on older devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Android 13+ requires NEARBY_WIFI_DEVICES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        return requiredPermissions.all { permission ->
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
