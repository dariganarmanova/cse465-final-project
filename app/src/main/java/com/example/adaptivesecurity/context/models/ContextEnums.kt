package com.example.adaptivesecurity.context.models

enum class LocationCategory {
    HOME, WORK, PUBLIC, UNKNOWN
}

enum class NetworkType {
    NONE,
    WIFI,
    CELLULAR_4G,
    CELLULAR_5G,
    ETHERNET,
    OTHER
}

enum class TimeOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT
}

enum class AppCategory {
    BANKING, SOCIAL, COMMUNICATION, OTHER
}

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class LocationContext(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val isKnownLocation: Boolean = false,
    val locationCategory: LocationCategory = LocationCategory.UNKNOWN,
    val speed: Float = 0f
)

data class NetworkContext(
    val networkType: NetworkType,
    val isSecureNetwork: Boolean,
    val networkName: String? = null,
    val signalStrength: Int = 0
)

data class DeviceContext(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val isDeviceLocked: Boolean,
    val lastUnlockTime: Long,
    val screenBrightness: Int = 50
)

data class TimeContext(
    val timeOfDay: TimeOfDay,
    val isWorkingHours: Boolean
)

data class AppUsageContext(
    val currentApp: String?,
    val appCategory: AppCategory,
    val packageName: String?
)
data class KeyboardContext(
    val typingPattern: TypingPattern,
    val anomalyScore: Double,
    val isAnomalous: Boolean,
    val recentKeystrokes: List<Keystroke>,
    val sessionDuration: Long,
    val averageWpm: Double
)

data class TypingPattern(
    val dwellTimes: List<Double>,
    val flightTimes: List<Double>,
    val rhythm: TypingRhythm,
    val pressure: List<Float>,
    val accuracy: Double
)

data class TypingRhythm(
    val averageDwellTime: Double,
    val averageFlightTime: Double,
    val dwellTimeVariance: Double,
    val flightTimeVariance: Double,
    val rhythmConsistency: Double
)

data class Keystroke(
    val keyCode: Int,
    val pressTime: Long,
    val releaseTime: Long,
    val pressure: Float = 0f,
    val x: Float = 0f,
    val y: Float = 0f
) {
    val dwellTime: Long get() = releaseTime - pressTime
}

// Update your existing ContextData to include keystroke:
data class ContextData(
    val timestamp: Long = System.currentTimeMillis(),
    val locationContext: LocationContext?,
    val networkContext: NetworkContext?,
    val deviceContext: DeviceContext?,
    val timeContext: TimeContext,
    val appUsageContext: AppUsageContext?,
    val keyboardContext: KeyboardContext?, // ADD THIS LINE
    val riskLevel: RiskLevel = RiskLevel.MEDIUM
)