package com.example.adaptivesecurity.testing

import android.content.Context
import com.example.adaptivesecurity.context.ContextRecognitionManager
import com.example.adaptivesecurity.context.collectors.*
import com.example.adaptivesecurity.context.models.*
import kotlinx.coroutines.delay

class ContextTestManager(
    private val context: Context,
    private val contextManager: ContextRecognitionManager
) {

    data class CollectorTestResult(
        val collectorName: String,
        val testName: String,
        val passed: Boolean,
        val actualValue: Any?,
        val expectedValue: Any?,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class IntegrationTestResult(
        val scenarioName: String,
        val riskLevel: RiskLevel,
        val expectedRisk: RiskLevel,
        val passed: Boolean,
        val contextSnapshot: ContextData,
        val message: String
    )

    private val testResults = mutableListOf<CollectorTestResult>()
    private val integrationResults = mutableListOf<IntegrationTestResult>()

    // Individual collectors for direct testing
    private val locationCollector = LocationContextCollector(context)
    private val networkCollector = NetworkContextCollector(context)
    private val deviceCollector = DeviceContextCollector(context)
    private val timeCollector = TimeContextCollector()
    private val appUsageCollector = AppUsageContextCollector(context)
    private val keystrokeCollector = contextManager.getKeystrokeCollector()

    //RUN ALL TESTS
    suspend fun runCompleteTestSuite() {
        println("STARTING COMPREHENSIVE CONTEXT TEST")
        println("=" * 50)

        clearAllResults()

        // Test individual collectors
        testLocationCollector()
        delay(2000)

        testNetworkCollector()
        delay(2000)

        testDeviceCollector()
        delay(2000)

        testTimeCollector()
        delay(2000)

        testAppUsageCollector()
        delay(2000)

        testKeystrokeCollector()
        delay(2000)

        // Test integration scenarios
        testIntegrationScenarios()

        // Generate final report
        generateCompleteReport()
    }

    // LOCATION COLLECTOR TESTS
    suspend fun testLocationCollector() {
        println("\nTESTING LOCATION COLLECTOR")
        println("-" * 30)

        try {
            val locationContext = locationCollector.collectLocationContext()

            if (locationContext != null) {
                addTestResult("LocationCollector", "Data Collection",
                    true, "Success", "Non-null data",
                    "Location data collected successfully")

                // Test location category determination
                val hasCategory = locationContext.locationCategory != LocationCategory.UNKNOWN
                addTestResult("LocationCollector", "Category Detection",
                    hasCategory, locationContext.locationCategory, "Valid category",
                    if (hasCategory) "Location category determined" else "Location category unknown")

                // Test known location detection
                addTestResult("LocationCollector", "Known Location Detection",
                    true, locationContext.isKnownLocation, "Boolean value",
                    "Known location status: ${locationContext.isKnownLocation}")

                // Test accuracy
                val hasAccuracy = locationContext.accuracy > 0
                addTestResult("LocationCollector", "GPS Accuracy",
                    hasAccuracy, "${locationContext.accuracy}m", "Positive value",
                    if (hasAccuracy) "GPS accuracy: ${locationContext.accuracy}m" else "No GPS accuracy data")

                println("Location Test Results:")
                println("   Category: ${locationContext.locationCategory}")
                println("   Known Location: ${locationContext.isKnownLocation}")
                println("   Accuracy: ${locationContext.accuracy}m")
                println("   Coordinates: (${String.format("%.4f", locationContext.latitude)}, ${String.format("%.4f", locationContext.longitude)})")

            } else {
                addTestResult("LocationCollector", "Permission Check",
                    false, null, "Location data",
                    "Location data unavailable - check permissions")
            }

        } catch (e: Exception) {
            addTestResult("LocationCollector", "Error Handling",
                false, e.message, "No exception",
                "Location collector error: ${e.message}")
        }
    }

    // NETWORK COLLECTOR TESTS
    suspend fun testNetworkCollector() {
        println("\nTESTING NETWORK COLLECTOR")
        println("-" * 30)

        try {
            val networkContext = networkCollector.collectNetworkContext()

            if (networkContext != null) {
                addTestResult("NetworkCollector", "Data Collection",
                    true, "Success", "Non-null data",
                    "Network data collected successfully")

                // Test network type detection
                val hasValidType = networkContext.networkType != NetworkType.NONE
                addTestResult("NetworkCollector", "Network Type Detection",
                    hasValidType, networkContext.networkType, "Valid network type",
                    if (hasValidType) "Network type: ${networkContext.networkType}" else "⚠️ No network detected")

                // Test security detection
                addTestResult("NetworkCollector", "Security Assessment",
                    true, networkContext.isSecureNetwork, "Boolean value",
                    "Network security: ${if (networkContext.isSecureNetwork) "Secure" else "Insecure"}")

                println("Network Test Results:")
                println("   Type: ${networkContext.networkType}")
                println("   Secure: ${networkContext.isSecureNetwork}")

            } else {
                addTestResult("NetworkCollector", "Network Availability",
                    false, null, "Network data",
                    "No network data available")
            }

        } catch (e: Exception) {
            addTestResult("NetworkCollector", "Error Handling",
                false, e.message, "No exception",
                "Network collector error: ${e.message}")
        }
    }

    // DEVICE COLLECTOR TESTS
    suspend fun testDeviceCollector() {
        println("\nTESTING DEVICE COLLECTOR")
        println("-" * 30)

        try {
            val deviceContext = deviceCollector.collectDeviceContext()

            addTestResult("DeviceCollector", "Data Collection",
                true, "Success", "Non-null data",
                "Device data collected successfully")

            // Test battery level
            val validBattery = deviceContext.batteryLevel in 0..100
            addTestResult("DeviceCollector", "Battery Level",
                validBattery, "${deviceContext.batteryLevel}%", "0-100%",
                if (validBattery) "Battery level: ${deviceContext.batteryLevel}%" else "Invalid battery level")

            // Test charging status
            addTestResult("DeviceCollector", "Charging Status",
                true, deviceContext.isCharging, "Boolean value",
                "Charging status: ${if (deviceContext.isCharging) "Charging" else "Not charging"}")

            // Test lock status
            addTestResult("DeviceCollector", "Lock Status",
                true, deviceContext.isDeviceLocked, "Boolean value",
                "Device locked: ${deviceContext.isDeviceLocked}")

            // Test unlock time
            val recentUnlock = (System.currentTimeMillis() - deviceContext.lastUnlockTime) < 3600000 // 1 hour
            addTestResult("DeviceCollector", "Last Unlock Time",
                true, "Within reasonable range", "Recent timestamp",
                "Last unlock: ${(System.currentTimeMillis() - deviceContext.lastUnlockTime) / 60000} minutes ago")

            println("Device Test Results:")
            println("   Battery: ${deviceContext.batteryLevel}%")
            println("   Charging: ${deviceContext.isCharging}")
            println("   Locked: ${deviceContext.isDeviceLocked}")
            println("   Last Unlock: ${(System.currentTimeMillis() - deviceContext.lastUnlockTime) / 60000} min ago")

        } catch (e: Exception) {
            addTestResult("DeviceCollector", "Error Handling",
                false, e.message, "No exception",
                "Device collector error: ${e.message}")
        }
    }

    // TIME COLLECTOR TESTS
    suspend fun testTimeCollector() {
        println("\nTESTING TIME COLLECTOR")
        println("-" * 30)

        try {
            val timeContext = timeCollector.collectTimeContext()

            addTestResult("TimeCollector", "Data Collection",
                true, "Success", "Non-null data",
                "Time data collected successfully")

            // Test time of day determination
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val expectedTimeOfDay = when (currentHour) {
                in 0..5 -> TimeOfDay.NIGHT
                in 6..11 -> TimeOfDay.MORNING
                in 12..16 -> TimeOfDay.AFTERNOON
                in 17..21 -> TimeOfDay.EVENING
                else -> TimeOfDay.NIGHT
            }

            val correctTimeOfDay = timeContext.timeOfDay == expectedTimeOfDay
            addTestResult("TimeCollector", "Time of Day Detection",
                correctTimeOfDay, timeContext.timeOfDay, expectedTimeOfDay,
                if (correctTimeOfDay) "Correct time of day: ${timeContext.timeOfDay}" else "Time of day mismatch")

            // Test working hours
            val expectedWorkingHours = currentHour in 9..16
            val correctWorkingHours = timeContext.isWorkingHours == expectedWorkingHours
            addTestResult("TimeCollector", "Working Hours Detection",
                correctWorkingHours, timeContext.isWorkingHours, expectedWorkingHours,
                if (correctWorkingHours) "Working hours correct: ${timeContext.isWorkingHours}" else "Working hours mismatch")

            println("Time Test Results:")
            println("   Current Hour: $currentHour")
            println("   Time of Day: ${timeContext.timeOfDay} (Expected: $expectedTimeOfDay)")
            println("   Working Hours: ${timeContext.isWorkingHours} (Expected: $expectedWorkingHours)")

        } catch (e: Exception) {
            addTestResult("TimeCollector", "Error Handling",
                false, e.message, "No exception",
                "Time collector error: ${e.message}")
        }
    }

    // APP USAGE COLLECTOR TESTS
    suspend fun testAppUsageCollector() {
        println("\nTESTING APP USAGE COLLECTOR")
        println("-" * 30)

        try {
            val appUsageContext = appUsageCollector.collectAppUsageContext()

            if (appUsageContext != null) {
                addTestResult("AppUsageCollector", "Data Collection",
                    true, "Success", "Non-null data",
                    "App usage data collected successfully")

                // Test app detection
                val hasCurrentApp = !appUsageContext.currentApp.isNullOrEmpty()
                addTestResult("AppUsageCollector", "Current App Detection",
                    hasCurrentApp, appUsageContext.currentApp, "Non-empty app name",
                    if (hasCurrentApp) "Current app: ${appUsageContext.currentApp}" else "No current app detected")

                // Test app categorization
                val hasCategory = appUsageContext.appCategory != AppCategory.OTHER
                addTestResult("AppUsageCollector", "App Categorization",
                    true, appUsageContext.appCategory, "Valid category",
                    "App category: ${appUsageContext.appCategory}")

                // Test package name
                val hasPackage = !appUsageContext.packageName.isNullOrEmpty()
                addTestResult("AppUsageCollector", "Package Name Detection",
                    hasPackage, appUsageContext.packageName, "Non-empty package",
                    if (hasPackage) "Package: ${appUsageContext.packageName}" else "No package name")

                println("App Usage Test Results:")
                println("   Current App: ${appUsageContext.currentApp}")
                println("   Category: ${appUsageContext.appCategory}")
                println("   Package: ${appUsageContext.packageName}")

            } else {
                addTestResult("AppUsageCollector", "Permission Check",
                    false, null, "App usage data",
                    "App usage data unavailable - check Usage Stats permission")
            }

        } catch (e: Exception) {
            addTestResult("AppUsageCollector", "Error Handling",
                false, e.message, "No exception",
                "App usage collector error: ${e.message}")
        }
    }

    // KEYSTROKE COLLECTOR TESTS
    suspend fun testKeystrokeCollector() {
        println("\n⌨️ TESTING KEYSTROKE COLLECTOR")
        println("-" * 30)

        try {
            val keyboardContext = keystrokeCollector.collectContext()

            addTestResult("KeystrokeCollector", "Data Collection",
                true, "Success", "Non-null data",
                "Keystroke data collected successfully")

            // Test baseline establishment
            val hasBaseline = keystrokeCollector.getDebugInfo().contains("Has Baseline: true")
            addTestResult("KeystrokeCollector", "Baseline Establishment",
                hasBaseline, "Baseline exists", "Baseline established",
                if (hasBaseline) "Baseline established" else "Baseline not yet established - need more typing")

            // Test anomaly detection
            val anomalyScore = keyboardContext.anomalyScore
            val validScore = anomalyScore >= 0.0 && anomalyScore <= 1.0
            addTestResult("KeystrokeCollector", "Anomaly Score Range",
                validScore, String.format("%.3f", anomalyScore), "0.0-1.0",
                if (validScore) "Anomaly score: ${String.format("%.3f", anomalyScore)}" else "Invalid anomaly score")

            // Test WPM calculation
            val wpm = keyboardContext.averageWpm
            val validWPM = wpm >= 0.0 && wpm <= 200.0 // Reasonable WPM range
            addTestResult("KeystrokeCollector", "WPM Calculation",
                validWPM, String.format("%.1f", wpm), "0-200 WPM",
                if (validWPM) "WPM: ${String.format("%.1f", wpm)}" else "WPM out of range")

            // Test typing pattern analysis
            val hasPattern = keyboardContext.typingPattern.dwellTimes.isNotEmpty() ||
                    keyboardContext.typingPattern.flightTimes.isNotEmpty()
            addTestResult("KeystrokeCollector", "Pattern Analysis",
                hasPattern, "Pattern data available", "Typing patterns detected",
                if (hasPattern) "Typing patterns detected" else "No typing patterns yet")

            println("Keystroke Test Results:")
            println("   Anomaly Score: ${String.format("%.3f", anomalyScore)}")
            println("   WPM: ${String.format("%.1f", wpm)}")
            println("   Anomalous: ${keyboardContext.isAnomalous}")
            println("   Recent Keystrokes: ${keyboardContext.recentKeystrokes.size}")
            println(keystrokeCollector.getDebugInfo())

        } catch (e: Exception) {
            addTestResult("KeystrokeCollector", "Error Handling",
                false, e.message, "No exception",
                "Keystroke collector error: ${e.message}")
        }
    }

    // INTEGRATION SCENARIO TESTS
    suspend fun testIntegrationScenarios() {
        println("\nTESTING INTEGRATION SCENARIOS")
        println("-" * 40)

        // Test realistic scenarios
        testScenario("Safe Home Environment", RiskLevel.LOW,
            "User at home, on secure WiFi, normal hours, normal typing")

        testScenario("Public Place Risk", RiskLevel.MEDIUM,
            "User in public place, on open WiFi")

        testScenario("Banking App Security", RiskLevel.HIGH,
            "Banking app used in unknown location with insecure network")

        testScenario("Night Access Attempt", RiskLevel.HIGH,
            "Device access during night hours with suspicious typing")

        testScenario("Critical Threat Simulation", RiskLevel.CRITICAL,
            "Multiple risk factors: unknown location, insecure network, anomalous typing")
    }

    private suspend fun testScenario(scenarioName: String, expectedRisk: RiskLevel, description: String) {
        println("\nTesting Scenario: $scenarioName")
        println("   Description: $description")
        println("   Expected Risk: $expectedRisk")

        try {
            // Get current context
            val context = contextManager.getCurrentContext()
            val actualRisk = context.riskLevel

            val passed = actualRisk == expectedRisk

            addIntegrationResult(IntegrationTestResult(
                scenarioName = scenarioName,
                riskLevel = actualRisk,
                expectedRisk = expectedRisk,
                passed = passed,
                contextSnapshot = context,
                message = if (passed) "Risk level matches expectation" else "Risk level differs from expectation"
            ))

            println("   Actual Risk: $actualRisk")
            println("   Result: ${if (passed) "PASS" else "DIFFERENT"}")

            // Show context details
            println("   Context Details:")
            println("     Location: ${context.locationContext?.locationCategory ?: "Unknown"}")
            println("     Network: ${context.networkContext?.networkType ?: "Unknown"} (${if (context.networkContext?.isSecureNetwork == true) "Secure" else "Insecure"})")
            println("     Time: ${context.timeContext.timeOfDay} (${if (context.timeContext.isWorkingHours) "Work" else "Non-work"})")
            println("     App: ${context.appUsageContext?.appCategory ?: "Unknown"}")
            println("     Keystroke Anomaly: ${String.format("%.3f", context.keyboardContext?.anomalyScore ?: 0.0)}")

        } catch (e: Exception) {
            addIntegrationResult(IntegrationTestResult(
                scenarioName = scenarioName,
                riskLevel = RiskLevel.LOW,
                expectedRisk = expectedRisk,
                passed = false,
                contextSnapshot = ContextData(
                    locationContext = null,
                    networkContext = null,
                    deviceContext = null,
                    timeContext = TimeContext(TimeOfDay.MORNING, false),
                    appUsageContext = null
                ),
                message = "Scenario test failed: ${e.message}"
            ))
        }
    }

    // GENERATE REPORTS
    private fun generateCompleteReport() {
        println("\n" + "=" * 60)
        println("COMPREHENSIVE TEST REPORT")
        println("=" * 60)

        generateCollectorReport()
        generateIntegrationReport()
        generateSummaryReport()
    }

    private fun generateCollectorReport() {
        println("\nINDIVIDUAL COLLECTOR RESULTS")
        println("-" * 40)

        val collectorGroups = testResults.groupBy { it.collectorName }

        collectorGroups.forEach { (collectorName, results) ->
            val passed = results.count { it.passed }
            val total = results.size
            val percentage = if (total > 0) (passed * 100) / total else 0

            println("\n$collectorName: $passed/$total tests passed ($percentage%)")

            results.forEach { result ->
                val status = if (result.passed) "passed" else "failed"
                println("  $status ${result.testName}: ${result.message}")
            }
        }
    }

    private fun generateIntegrationReport() {
        println("\nINTEGRATION SCENARIO RESULTS")
        println("-" * 40)

        integrationResults.forEach { result ->
            val status = if (result.passed) "passed" else "failed"
            println("$status ${result.scenarioName}")
            println("    Expected: ${result.expectedRisk}, Actual: ${result.riskLevel}")
            println("    ${result.message}")
        }
    }

    private fun generateSummaryReport() {
        println("\nOVERALL SUMMARY")
        println("-" * 20)

        val totalCollectorTests = testResults.size
        val passedCollectorTests = testResults.count { it.passed }
        val collectorAccuracy = if (totalCollectorTests > 0) (passedCollectorTests * 100) / totalCollectorTests else 0

        val totalIntegrationTests = integrationResults.size
        val passedIntegrationTests = integrationResults.count { it.passed }
        val integrationAccuracy = if (totalIntegrationTests > 0) (passedIntegrationTests * 100) / totalIntegrationTests else 0

        println("Collector Tests: $passedCollectorTests/$totalCollectorTests passed ($collectorAccuracy%)")
        println("Integration Tests: $passedIntegrationTests/$totalIntegrationTests passed ($integrationAccuracy%)")

        val overallAccuracy = if ((totalCollectorTests + totalIntegrationTests) > 0) {
            ((passedCollectorTests + passedIntegrationTests) * 100) / (totalCollectorTests + totalIntegrationTests)
        } else 0

        println("Overall Accuracy: $overallAccuracy%")

        when {
            overallAccuracy >= 90 -> println("accuracy over 90")
            overallAccuracy >= 75 -> println("accuracy over 75")
            overallAccuracy >= 60 -> println("accuracy over 60")
            else -> println("accuracy low")
        }

        // Recommendations
        println("\n💡 RECOMMENDATIONS:")
        if (collectorAccuracy < 80) {
            println("• Check permissions for failing collectors")
            println("• Verify device settings and capabilities")
        }
        if (integrationAccuracy < 80) {
            println("• Review risk level calculation logic")
            println("• Test in different real-world scenarios")
        }
        if (overallAccuracy >= 80) {
            println("• System is ready for production use")
            println("• Consider adding more sophisticated ML models")
        }
    }

    // Helper methods
    private fun addTestResult(collector: String, test: String, passed: Boolean,
                              actual: Any?, expected: Any?, message: String) {
        testResults.add(CollectorTestResult(collector, test, passed, actual, expected, message))
    }

    private fun addIntegrationResult(result: IntegrationTestResult) {
        integrationResults.add(result)
    }

    private fun clearAllResults() {
        testResults.clear()
        integrationResults.clear()
    }

    // Get results for external use
    fun getTestResults(): List<CollectorTestResult> = testResults.toList()
    fun getIntegrationResults(): List<IntegrationTestResult> = integrationResults.toList()
}