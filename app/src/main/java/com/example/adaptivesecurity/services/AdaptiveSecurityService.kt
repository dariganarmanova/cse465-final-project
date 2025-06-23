// File: services/AdaptiveSecurityService.kt
package com.example.adaptivesecurity.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.lifecycleScope
import com.example.adaptivesecurity.context.ContextRecognitionManager
import com.example.adaptivesecurity.context.listeners.ContextListener
import com.example.adaptivesecurity.context.models.*
import kotlinx.coroutines.*

class AdaptiveSecurityService : AccessibilityService() {

    private var contextManager: ContextRecognitionManager? = null
    private var isActive = false
    private var monitoringJob: Job? = null

    // Security response settings
    private val criticalThreshold = RiskLevel.CRITICAL
    private val highThreshold = RiskLevel.HIGH
    private val checkIntervalMs = 30000L // 30 seconds

    //SERVICE STARTS
    override fun onServiceConnected() {
        super.onServiceConnected()

        println("ADAPTIVE SECURITY SERVICE STARTED")

        setupAccessibilityService()
        initializeContextMonitoring()
        startContinuousMonitoring()

        showServiceNotification("Adaptive Security Active")
    }

    private fun setupAccessibilityService() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    private fun initializeContextMonitoring() {
        contextManager = ContextRecognitionManager(applicationContext)

        // Register security response listener
        contextManager?.registerContextListener(object : ContextListener {
            override fun onContextChanged(context: ContextData) {
                handleContextUpdate(context)
            }

            override fun onRiskLevelChanged(newRisk: RiskLevel, previousRisk: RiskLevel) {
                handleRiskLevelChange(newRisk, previousRisk)
            }

            override fun onKeyboardAnomalyDetected(keyboardContext: KeyboardContext) {
                handleKeystrokeAnomaly(keyboardContext)
            }
        })

        lifecycleScope.launch {
            contextManager?.startContextMonitoring()
        }
    }

    private fun startContinuousMonitoring() {
        isActive = true
        monitoringJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    performSecurityCheck()
                    delay(checkIntervalMs)
                } catch (e: Exception) {
                    println("Monitoring error: ${e.message}")
                    delay(60000) // Wait longer on error
                }
            }
        }
    }

    //PERIODIC SECURITY CHECK
    private suspend fun performSecurityCheck() {
        val context = contextManager?.getCurrentContext() ?: return

        logSecurityStatus(context)

        // Check for immediate threats
        when (context.riskLevel) {
            RiskLevel.CRITICAL -> handleCriticalThreat(context)
            RiskLevel.HIGH -> handleHighRisk(context)
            RiskLevel.MEDIUM -> handleMediumRisk(context)
            RiskLevel.LOW -> handleLowRisk(context)
        }
    }

    // SECURITY RESPONSES
    private fun handleCriticalThreat(context: ContextData) {
        println("CRITICAL THREAT DETECTED!")

        // Immediate response
        lockDevice()
        sendCriticalAlert(generateThreatReport(context))
        logSecurityIncident("CRITICAL", context)

        // Notify activities
        broadcastSecurityUpdate("CRITICAL_THREAT", context)
    }

    private fun handleHighRisk(context: ContextData) {
        println("HIGH RISK DETECTED")

        sendSecurityWarning(generateRiskReport(context))
        logSecurityIncident("HIGH", context)

        broadcastSecurityUpdate("HIGH_RISK", context)
    }

    private fun handleMediumRisk(context: ContextData) {
        println("Medium risk logged")
        logSecurityIncident("MEDIUM", context)

        broadcastSecurityUpdate("MEDIUM_RISK", context)
    }

    private fun handleLowRisk(context: ContextData) {
        println("Security status: Normal")
        broadcastSecurityUpdate("NORMAL", context)
    }

    private fun handleKeystrokeAnomaly(keyboardContext: KeyboardContext) {
        println("KEYSTROKE ANOMALY: Score ${keyboardContext.anomalyScore}")

        if (keyboardContext.anomalyScore > 0.8) {
            // Immediate high-severity keystroke threat
            sendUrgentAlert("Suspicious typing pattern detected! Possible unauthorized access.")
        }

        broadcastKeystrokeUpdate(keyboardContext)
    }

    //DEVICE CONTROL
    private fun lockDevice() {
        try {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            println("Device locked due to security threat")
        } catch (e: Exception) {
            println("Could not lock device: ${e.message}")
        }
    }

    // THREAT ANALYSIS
    private fun generateThreatReport(context: ContextData): String {
        val threats = mutableListOf<String>()

        context.locationContext?.let { location ->
            if (!location.isKnownLocation) {
                threats.add("Unknown location (${location.locationCategory})")
            }
        }

        context.networkContext?.let { network ->
            if (!network.isSecureNetwork) {
                threats.add("Insecure network (${network.networkType})")
            }
        }

        context.keyboardContext?.let { keyboard ->
            if (keyboard.isAnomalous) {
                threats.add("Suspicious typing pattern (Score: ${keyboard.anomalyScore})")
            }
        }

        context.appUsageContext?.let { app ->
            if (app.appCategory == AppCategory.BANKING) {
                threats.add("Banking app accessed during high risk")
            }
        }

        return """
            SECURITY THREAT DETECTED
            
            Risk Level: ${context.riskLevel}
            Threats Identified:
            ${threats.joinToString("\n") { "• $it" }}
            
            Time: ${java.util.Date()}
            Location: ${context.locationContext?.locationCategory ?: "Unknown"}
            Network: ${context.networkContext?.networkType ?: "Unknown"}
            App: ${context.appUsageContext?.currentApp ?: "Unknown"}
        """.trimIndent()
    }

    private fun generateRiskReport(context: ContextData): String {
        return """
            Security Alert
            
            Risk Level: ${context.riskLevel}
            
            Current Context:
            • Location: ${context.locationContext?.locationCategory ?: "Unknown"}
            • Network: ${context.networkContext?.networkType ?: "Unknown"} 
              (${if (context.networkContext?.isSecureNetwork == true) "Secure" else "Insecure"})
            • Time: ${context.timeContext.timeOfDay} 
              (${if (context.timeContext.isWorkingHours) "Work hours" else "Non-work hours"})
            • Device: ${context.deviceContext?.batteryLevel ?: "Unknown"}% battery
            • App: ${context.appUsageContext?.currentApp ?: "Unknown"}
            • Keystroke Anomaly: ${context.keyboardContext?.anomalyScore ?: 0.0}
            
            Recommendation: Monitor activity closely
        """.trimIndent()
    }

    // NOTIFICATIONS & BROADCASTS
    private fun sendCriticalAlert(message: String) {
        val intent = Intent("ADAPTIVE_SECURITY_ALERT").apply {
            putExtra("level", "CRITICAL")
            putExtra("message", message)
            putExtra("timestamp", System.currentTimeMillis())
        }
        sendBroadcast(intent)
    }

    private fun sendSecurityWarning(message: String) {
        val intent = Intent("ADAPTIVE_SECURITY_WARNING").apply {
            putExtra("level", "HIGH")
            putExtra("message", message)
            putExtra("timestamp", System.currentTimeMillis())
        }
        sendBroadcast(intent)
    }

    private fun sendUrgentAlert(message: String) {
        val intent = Intent("ADAPTIVE_SECURITY_URGENT").apply {
            putExtra("level", "URGENT")
            putExtra("message", message)
            putExtra("timestamp", System.currentTimeMillis())
        }
        sendBroadcast(intent)
    }

    private fun broadcastSecurityUpdate(action: String, context: ContextData) {
        val intent = Intent("SECURITY_STATUS_UPDATE").apply {
            putExtra("action", action)
            putExtra("risk_level", context.riskLevel.toString())
            putExtra("timestamp", context.timestamp)

            // Include all context data
            context.locationContext?.let {
                putExtra("location_category", it.locationCategory.toString())
                putExtra("location_known", it.isKnownLocation)
            }

            context.networkContext?.let {
                putExtra("network_type", it.networkType.toString())
                putExtra("network_secure", it.isSecureNetwork)
            }

            context.deviceContext?.let {
                putExtra("battery_level", it.batteryLevel)
                putExtra("device_charging", it.isCharging)
                putExtra("device_locked", it.isDeviceLocked)
            }

            putExtra("time_of_day", context.timeContext.timeOfDay.toString())
            putExtra("working_hours", context.timeContext.isWorkingHours)

            context.appUsageContext?.let {
                putExtra("current_app", it.currentApp)
                putExtra("app_category", it.appCategory.toString())
            }

            context.keyboardContext?.let {
                putExtra("anomaly_score", it.anomalyScore)
                putExtra("typing_anomalous", it.isAnomalous)
                putExtra("wpm", it.averageWpm)
            }
        }
        sendBroadcast(intent)
    }

    private fun broadcastKeystrokeUpdate(keyboardContext: KeyboardContext) {
        val intent = Intent("KEYSTROKE_STATUS_UPDATE").apply {
            putExtra("anomaly_score", keyboardContext.anomalyScore)
            putExtra("is_anomalous", keyboardContext.isAnomalous)
            putExtra("wpm", keyboardContext.averageWpm)
            putExtra("accuracy", keyboardContext.typingPattern.accuracy)
            putExtra("session_duration", keyboardContext.sessionDuration)
        }
        sendBroadcast(intent)
    }

    // LOGGING
    private fun logSecurityStatus(context: ContextData) {
        println("""
            Security Check - ${java.util.Date()}
            Risk: ${context.riskLevel}
            Location: ${context.locationContext?.locationCategory ?: "Unknown"} 
                     (${if (context.locationContext?.isKnownLocation == true) "Known" else "Unknown"})
            Network: ${context.networkContext?.networkType ?: "Unknown"} 
                    (${if (context.networkContext?.isSecureNetwork == true) "Secure" else "Insecure"})
            Time: ${context.timeContext.timeOfDay} (${if (context.timeContext.isWorkingHours) "Work" else "Non-work"})
            Device: ${context.deviceContext?.batteryLevel ?: "?"}% battery, 
                   ${if (context.deviceContext?.isCharging == true) "Charging" else "Not charging"}
            App: ${context.appUsageContext?.currentApp ?: "Unknown"} (${context.appUsageContext?.appCategory ?: "Unknown"})
            Keystroke: ${if (context.keyboardContext?.isAnomalous == true) "Anomalous" else "Normal"} 
                      (Score: ${context.keyboardContext?.anomalyScore ?: 0.0})
        """.trimIndent())
    }

    private fun logSecurityIncident(level: String, context: ContextData) {
        // In production, this would write to a secure log file or send to security server
        println("SECURITY LOG [$level] - ${System.currentTimeMillis()}: ${context.riskLevel}")
    }

    private fun showServiceNotification(message: String) {
        // Show persistent notification that service is running
        println("Service Notification: $message")
    }

    // KEYSTROKE CAPTURE
    override fun onKeyEvent(event: KeyEvent): Boolean {
        contextManager?.onKeyEvent(event)
        return false // Don't consume the event
    }

    // ACCESSIBILITY EVENTS
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can monitor app switches, text changes, etc.
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // App switched - trigger immediate context check
                lifecycleScope.launch {
                    delay(1000) // Small delay to let context settle
                    performSecurityCheck()
                }
            }
        }
    }

    // SERVICE MANAGEMENT
    fun getSecurityStatus(): String {
        return """
            🛡️ ADAPTIVE SECURITY STATUS
            
            Service Active: ${if (isActive) "YES" else "NO"}
            Monitoring Since: Service start
            Check Interval: ${checkIntervalMs / 1000}s
            
            Current Context:
            ${contextManager?.let { "Available" } ?: "Not initialized"}
            
            Recent Activity:
            - Service running normally
            - All collectors active
            - Risk monitoring enabled
        """.trimIndent()
    }

    // SERVICE CLEANUP
    override fun onDestroy() {
        super.onDestroy()

        isActive = false
        monitoringJob?.cancel()

        lifecycleScope.launch {
            contextManager?.stopContextMonitoring()
        }

        println("ADAPTIVE SECURITY SERVICE STOPPED")
    }

    override fun onInterrupt() {
        isActive = false
        println("Service interrupted")
    }

    private fun handleContextUpdate(context: ContextData) {
        // This is called every time any context changes
        // Can be used for immediate response to context changes
    }

    private fun handleRiskLevelChange(newRisk: RiskLevel, previousRisk: RiskLevel) {
        println("Risk level changed: $previousRisk → $newRisk")

        // Could trigger additional actions based on risk escalation/de-escalation
        if (newRisk > previousRisk) {
            println("Risk escalation detected")
        } else if (newRisk < previousRisk) {
            println("Risk de-escalation detected")
        }
    }
}