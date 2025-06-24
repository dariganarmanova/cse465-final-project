package com.example.adaptivesecurity.network.models

data class ConnectionState(
    val isConnected: Boolean,
    val currentNetwork: NetworkInfo?,
    val connectionQuality: ConnectionQuality,
    val vpnStatus: VpnStatus,
    val lastStateChange: Long = System.currentTimeMillis()
)

enum class ConnectionQuality {
    EXCELLENT, GOOD, FAIR, POOR, NO_CONNECTION
}

enum class VpnStatus {
    CONNECTED, DISCONNECTED, CONNECTING, ERROR
}