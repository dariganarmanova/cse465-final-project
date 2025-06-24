// Update your existing ContextRecognitionManager.kt
// Add these changes to your existing file:

package com.example.adaptivesecurity.context

import android.content.Context
import android.view.KeyEvent
import com.example.adaptivesecurity.context.collectors.*
import com.example.adaptivesecurity.context.listeners.ContextListener
import com.example.adaptivesecurity.context.models.*
import kotlinx.coroutines.*

class ContextRecognitionManager(
    private val context: Context
) : ContextRecognitionSystem {

    private val contextListeners = mutableListOf<ContextListener>()
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    private var currentContext: ContextData? = null

    // Your existing collectors
    private val locationCollector = LocationContextCollector(context)
    private val networkCollector = NetworkContextCollector(context)
    private val deviceCollector = DeviceContextCollector(context)
    private val timeCollector = TimeContextCollector()
    private val appUsageCollector = AppUsageContextCollector(context)

    // ADD THIS LINE - New keystroke collector
    private val keyboardCollector = SimpleKeystrokeCollector(context)

    override suspend fun getCurrentContext(): ContextData {
        return withContext(Dispatchers.IO) {
            // Your existing collector calls
            val locationCtx = locationCollector.collectLocationContext()
            val networkCtx = networkCollector.collectNetworkContext()
            val deviceCtx = deviceCollector.collectDeviceContext()
            val timeCtx = timeCollector.collectTimeContext()
            val appUsageCtx = appUsageCollector.collectAppUsageContext()

            // ADD THIS LINE - Collect keystroke context
            val keyboardCtx = keyboardCollector.collectKeystrokeContext()

            val contextData = ContextData(
                locationContext = locationCtx,
                networkContext = networkCtx,
                deviceContext = deviceCtx,
                timeContext = timeCtx,
                appUsageContext = appUsageCtx,
                keyboardContext = keyboardCtx, // ADD THIS LINE
                riskLevel = calculateRiskLevel(locationCtx, networkCtx, deviceCtx, timeCtx, appUsageCtx, keyboardCtx)
            )

            currentContext = contextData
            contextData
        }
    }

    // Your existing startContextMonitoring and other methods stay the same...
    override suspend fun startContextMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring) {
                try {
                    val newContext = getCurrentContext()
                    val previousContext = currentContext

                    contextListeners.forEach { listener ->
                        listener.onContextChanged(newContext)

                        if (previousContext != null &&
                            previousContext.riskLevel != newContext.riskLevel) {
                            listener.onRiskLevelChanged(
                                newContext.riskLevel,
                                previousContext.riskLevel
                            )
                        }

                        // ADD THIS - Keyboard anomaly notifications
                        newContext.keyboardContext?.let { keyboardCtx ->
                            if (keyboardCtx.isAnomalous) {
                                listener.onKeyboardAnomalyDetected(keyboardCtx)
                            }
                        }
                    }

                    delay(30000) // Check every 30 seconds
                } catch (e: Exception) {
                    delay(60000) // Wait longer if there's an error
                }
            }
        }
    }

    override suspend fun stopContextMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    override fun registerContextListener(listener: ContextListener) {
        contextListeners.add(listener)
    }

    override fun unregisterContextListener(listener: ContextListener) {
        contextListeners.remove(listener)
    }

    // UPDATE THIS METHOD - Add keyboard parameter
    private fun calculateRiskLevel(
        location: LocationContext?,
        network: NetworkContext?,
        device: DeviceContext?,
        time: TimeContext,
        appUsage: AppUsageContext?,
        keyboard: KeyboardContext? // ADD THIS PARAMETER
    ): RiskLevel {
        var riskScore = 0

        // Your existing risk calculations (keep these exactly as they are)
        location?.let {
            when (it.locationCategory) {
                LocationCategory.HOME -> riskScore += 0
                LocationCategory.WORK -> riskScore += 1
                LocationCategory.PUBLIC -> riskScore += 3
                LocationCategory.UNKNOWN -> riskScore += 2
            }
            if (!it.isKnownLocation) riskScore += 2
        }

        network?.let {
            if (!it.isSecureNetwork) riskScore += 3
            when (it.networkType) {
                NetworkType.WIFI -> if (!it.isSecureNetwork) riskScore += 2
                NetworkType.CELLULAR_4G, NetworkType.CELLULAR_5G -> riskScore += 1
                else -> riskScore += 2
            }
        }

        device?.let {
            if (it.batteryLevel < 20) riskScore += 1
            if (!it.isDeviceLocked && System.currentTimeMillis() - it.lastUnlockTime > 300000) {
                riskScore += 2
            }
        }

        if (!time.isWorkingHours && time.timeOfDay == TimeOfDay.NIGHT) {
            riskScore += 1
        }

        appUsage?.let {
            when (it.appCategory) {
                AppCategory.BANKING -> riskScore += 0
                AppCategory.SOCIAL -> riskScore += 1
                AppCategory.OTHER -> riskScore += 1
                else -> riskScore += 0
            }
        }

        // ADD THIS - Keyboard-based risk assessment
        keyboard?.let {
            if (it.isAnomalous) {
                when {
                    it.anomalyScore > 0.8 -> riskScore += 6  // Critical keystroke anomaly
                    it.anomalyScore > 0.5 -> riskScore += 4  // High keystroke anomaly
                    it.anomalyScore > 0.3 -> riskScore += 2  // Medium keystroke anomaly
                    else -> riskScore += 1  // Low keystroke anomaly
                }
            }

            // Additional keyboard factors
            if (it.averageWpm > 0 && it.averageWpm < 10) riskScore += 1 // Suspiciously slow
            if (it.averageWpm > 100) riskScore += 1 // Suspiciously fast
            if (it.typingPattern.accuracy < 0.8) riskScore += 1 // Low accuracy
        }

        return when {
            riskScore <= 2 -> RiskLevel.LOW
            riskScore <= 5 -> RiskLevel.MEDIUM
            riskScore <= 8 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }
    }

    // ADD THESE NEW METHODS for keystroke integration

    /**
     * Handle key events from activities or services
     */
    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        keyboardCollector.addKeystroke(keyEvent)
        return false // Don't consume the event
    }

    /**
     * Get the keystroke collector for testing
     */
    fun getKeystrokeCollector(): SimpleKeystrokeCollector {
        return keyboardCollector
    }

    /**
     * Get debug info about keystroke detection
     */
    fun getKeystrokeDebugInfo(): String {
        return keyboardCollector.getDebugInfo()
    }

    /**
     * Reset keystroke baseline (useful for testing)
     */
    fun resetKeystrokeBaseline() {
        keyboardCollector.resetBaseline()
    }
}