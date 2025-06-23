package com.example.adaptivesecurity.context
import android.content.Context
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

    private val locationCollector = LocationContextCollector(context)
    private val networkCollector = NetworkContextCollector(context)
    private val deviceCollector = DeviceContextCollector(context)
    private val timeCollector = TimeContextCollector()
    private val appUsageCollector = AppUsageContextCollector(context)

    override suspend fun getCurrentContext(): ContextData {
        return withContext(Dispatchers.IO) {
            val locationCtx = locationCollector.collectLocationContext()
            val networkCtx = networkCollector.collectNetworkContext()
            val deviceCtx = deviceCollector.collectDeviceContext()
            val timeCtx = timeCollector.collectTimeContext()
            val appUsageCtx = appUsageCollector.collectAppUsageContext()

            val contextData = ContextData(
                locationContext = locationCtx,
                networkContext = networkCtx,
                deviceContext = deviceCtx,
                timeContext = timeCtx,
                appUsageContext = appUsageCtx,
                riskLevel = calculateRiskLevel(locationCtx, networkCtx, deviceCtx, timeCtx, appUsageCtx)
            )

            currentContext = contextData
            contextData
        }
    }

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

    private fun calculateRiskLevel(
        location: LocationContext?,
        network: NetworkContext?,
        device: DeviceContext?,
        time: TimeContext,
        appUsage: AppUsageContext?
    ): RiskLevel {
        var riskScore = 0

        // Location-based risk
        location?.let {
            when (it.locationCategory) {
                LocationCategory.HOME -> riskScore += 0
                LocationCategory.WORK -> riskScore += 1
                LocationCategory.PUBLIC -> riskScore += 3
                LocationCategory.UNKNOWN -> riskScore += 2
            }
            if (!it.isKnownLocation) riskScore += 2
        }

        // Network-based risk
        network?.let {
            if (!it.isSecureNetwork) riskScore += 3
            when (it.networkType) {
                NetworkType.WIFI -> if (!it.isSecureNetwork) riskScore += 2
                NetworkType.CELLULAR_4G, NetworkType.CELLULAR_5G -> riskScore += 1
                else -> riskScore += 2
            }
        }

        // Device-based risk
        device?.let {
            if (it.batteryLevel < 20) riskScore += 1
            if (!it.isDeviceLocked && System.currentTimeMillis() - it.lastUnlockTime > 300000) {
                riskScore += 2
            }
        }

        // Time-based risk
        if (!time.isWorkingHours && time.timeOfDay == TimeOfDay.NIGHT) {
            riskScore += 1
        }

        // App-based risk
        appUsage?.let {
            when (it.appCategory) {
                AppCategory.BANKING -> riskScore += 0
                AppCategory.SOCIAL -> riskScore += 1
                AppCategory.OTHER -> riskScore += 1
                else -> riskScore += 0
            }
        }

        return when {
            riskScore <= 2 -> RiskLevel.LOW
            riskScore <= 5 -> RiskLevel.MEDIUM
            riskScore <= 8 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }
    }
}
