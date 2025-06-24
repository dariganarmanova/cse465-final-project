package com.example.adaptivesecurity.network

import com.example.adaptivesecurity.network.collectors.WifiCollector
import com.example.adaptivesecurity.network.models.SecurityStatus
import com.example.adaptivesecurity.network.models.SecurityType
import com.example.adaptivesecurity.network.listeners.SecurityListener

class SecurityAnalyzer(private val wifiCollector: WifiCollector) {

    private val listeners = mutableListOf<SecurityListener>()

    fun analyzeCurrentNetwork() {
        val network = wifiCollector.getCurrentNetwork()
        val status = if (network != null) {
            classifySecurity(network.securityType)
        } else {
            SecurityStatus.UNKNOWN
        }
        listeners.forEach { it.onSecurityAnalysis(status) }
    }

    fun scanAndAnalyzeNetworks() {
        val networks = wifiCollector.scanNetworks()
        listeners.forEach { it.onNetworkScanComplete(networks) }
    }

    private fun classifySecurity(type: SecurityType): SecurityStatus {
        return when (type) {
            SecurityType.WPA3_SAE, SecurityType.EAP, SecurityType.WPA2_PSK -> SecurityStatus.TRUSTED
            SecurityType.OPEN, SecurityType.WEP -> SecurityStatus.UNTRUSTED
            else -> SecurityStatus.UNKNOWN
        }
    }

    fun addListener(listener: SecurityListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: SecurityListener) {
        listeners.remove(listener)
    }
}