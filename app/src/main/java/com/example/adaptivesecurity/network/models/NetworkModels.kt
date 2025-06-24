package com.example.adaptivesecurity.network.models
enum class SecurityStatus { TRUSTED, UNTRUSTED, UNKNOWN }

enum class SecurityType { WPA3_SAE, EAP, WPA2_PSK, OPEN, WEP, UNKNOWN }

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val securityType: SecurityType,
    val signalStrength: Int,
    val isCurrent: Boolean = false
)