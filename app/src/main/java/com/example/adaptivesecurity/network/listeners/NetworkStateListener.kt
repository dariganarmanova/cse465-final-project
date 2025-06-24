package com.example.adaptivesecurity.network.listeners

import com.example.adaptivesecurity.network.models.*

interface NetworkStateListener {
    fun onNetworkConnected(network: NetworkInfo)
    fun onNetworkDisconnected(network: NetworkInfo)
    fun onNetworkChanged(oldNetwork: NetworkInfo?, newNetwork: NetworkInfo?)
    fun onSignalStrengthChanged(network: NetworkInfo, newStrength: Int)
    fun onNetworkCapabilitiesChanged(network: NetworkInfo)
}

interface WifiSecurityListener {
    fun onThreatDetected(threat: NetworkThreat)
    fun onTrustLevelChanged(network: WifiNetwork, oldLevel: TrustLevel, newLevel: TrustLevel)
    fun onSuspiciousNetworkDetected(network: WifiNetwork)
    fun onSecurityProfileUpdated(profile: NetworkSecurityProfile)
}

interface NetworkTrafficListener {
    fun onSuspiciousTraffic(networkId: String, trafficData: Map<String, Any>)
    fun onDataLeakDetected(networkId: String, leakData: Map<String, Any>)
    fun onUnusualBandwidthUsage(networkId: String, usage: Long)
}