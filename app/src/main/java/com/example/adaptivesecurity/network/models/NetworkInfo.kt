package com.example.adaptivesecurity.network.models
import android.net.NetworkCapabilities
import java.util.*

data class NetworkInfo(
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val frequency: Int,
    val capabilities: String,
    val networkType: NetworkType,
    val isConnected: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val frequency: Int,
    val capabilities: String,
    val isOpen: Boolean,
    val isSecure: Boolean,
    val encryptionType: EncryptionType,
    val trustLevel: TrustLevel = TrustLevel.UNKNOWN,
    val lastSeen: Long = System.currentTimeMillis()
)

data class NetworkSecurityProfile(
    val networkId: String,
    val ssid: String,
    val bssid: String,
    val trustLevel: TrustLevel,
    val securityScore: Int, // 0-100
    val riskFactors: List<RiskFactor>,
    val firstSeen: Long,
    val lastConnected: Long?,
    val connectionCount: Int,
    val isWhitelisted: Boolean = false,
    val isBlacklisted: Boolean = false
)

data class NetworkThreat(
    val threatId: String = UUID.randomUUID().toString(),
    val networkId: String,
    val threatType: ThreatType,
    val severity: ThreatSeverity,
    val description: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val evidenceData: Map<String, Any> = emptyMap()
)

enum class NetworkType {
    WIFI, CELLULAR, ETHERNET, VPN, UNKNOWN
}

enum class EncryptionType {
    NONE, WEP, WPA, WPA2, WPA3, UNKNOWN
}

enum class TrustLevel {
    TRUSTED, SEMI_TRUSTED, UNTRUSTED, UNKNOWN
}

enum class RiskFactor {
    OPEN_NETWORK,
    WEAK_ENCRYPTION,
    SUSPICIOUS_NAME,
    CAPTIVE_PORTAL,
    UNUSUAL_BEHAVIOR,
    KNOWN_MALICIOUS,
    HONEYPOT_DETECTED,
    CERTIFICATE_ISSUES
}

enum class ThreatType {
    EVIL_TWIN,
    ROGUE_ACCESS_POINT,
    MAN_IN_THE_MIDDLE,
    DNS_HIJACKING,
    CAPTIVE_PORTAL_ATTACK,
    DEAUTH_ATTACK,
    PACKET_INJECTION,
    SUSPICIOUS_TRAFFIC
}

enum class ThreatSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}
