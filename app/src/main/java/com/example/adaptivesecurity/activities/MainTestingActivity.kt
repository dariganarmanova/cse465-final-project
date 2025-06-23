package com.example.adaptivesecurity.activities

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.adaptivesecurity.R
import com.example.adaptivesecurity.context.ContextRecognitionManager
import com.example.adaptivesecurity.context.listeners.ContextListener
import com.example.adaptivesecurity.context.models.*
import com.example.adaptivesecurity.services.AdaptiveSecurityService
import com.example.adaptivesecurity.testing.ContextTestManager
import com.example.adaptivesecurity.testing.KeystrokeTestManager
import kotlinx.coroutines.launch

class MainTestingActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tvOverallStatus: TextView
    private lateinit var tvCurrentRisk: TextView
    private lateinit var tvLocationStatus: TextView
    private lateinit var tvNetworkStatus: TextView
    private lateinit var tvDeviceStatus: TextView
    private lateinit var tvTimeStatus: TextView
    private lateinit var tvAppStatus: TextView
    private lateinit var tvKeystrokeStatus: TextView
    private lateinit var btnRunAllTests: Button
    private lateinit var btnTestIndividual: Button
    private lateinit var btnTestKeystrokes: Button
    private lateinit var btnTestScenarios: Button
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var svScrollView: ScrollView

    // Core components
    private lateinit var contextManager: ContextRecognitionManager
    private lateinit var comprehensiveTestManager: ContextTestManager
    private lateinit var keystrokeTestManager: KeystrokeTestManager

    // State
    private var isServiceRunning = false
    private var isTestRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_testing)

        initializeComponents()
        setupUI()
        registerServiceListener()
        startContextMonitoring()
    }

    private fun initializeComponents() {
        contextManager = ContextRecognitionManager(this)
        comprehensiveTestManager = ContextTestManager(this, contextManager)
        keystrokeTestManager = KeystrokeTestManager(contextManager.getKeystrokeCollector())

        // Register context listener for real-time updates
        contextManager.registerContextListener(object : ContextListener {
            override fun onContextChanged(context: ContextData) {
                runOnUiThread { updateContextDisplay(context) }
            }

            override fun onRiskLevelChanged(newRisk: RiskLevel, previousRisk: RiskLevel) {
                runOnUiThread {
                    updateRiskLevelDisplay(newRisk)
                    showRiskChangeAlert(newRisk, previousRisk)
                }
            }

            override fun onKeyboardAnomalyDetected(keyboardContext: KeyboardContext) {
                runOnUiThread {
                    showKeystrokeAnomalyAlert(keyboardContext)
                }
            }
        })
    }

    private fun setupUI() {
        // Initialize UI components
        tvOverallStatus = findViewById(R.id.tvOverallStatus)
        tvCurrentRisk = findViewById(R.id.tvCurrentRisk)
        tvLocationStatus = findViewById(R.id.tvLocationStatus)
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus)
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus)
        tvTimeStatus = findViewById(R.id.tvTimeStatus)
        tvAppStatus = findViewById(R.id.tvAppStatus)
        tvKeystrokeStatus = findViewById(R.id.tvKeystrokeStatus)
        btnRunAllTests = findViewById(R.id.btnRunAllTests)
        btnTestIndividual = findViewById(R.id.btnTestIndividual)
        btnTestKeystrokes = findViewById(R.id.btnTestKeystrokes)
        btnTestScenarios = findViewById(R.id.btnTestScenarios)
        btnStartService = findViewById(R.id.btnStartService)
        btnStopService = findViewById(R.id.btnStopService)
        progressBar = findViewById(R.id.progressBar)
        svScrollView = findViewById(R.id.svScrollView)

        // Set button click listeners
        btnRunAllTests.setOnClickListener { runComprehensiveTests() }
        btnTestIndividual.setOnClickListener { showIndividualTestMenu() }
        btnTestKeystrokes.setOnClickListener { runKeystrokeTests() }
        btnTestScenarios.setOnClickListener { runScenarioTests() }
        btnStartService.setOnClickListener { startSecurityService() }
        btnStopService.setOnClickListener { stopSecurityService() }

        // Initial UI state
        updateOverallStatus("Initializing adaptive security system...")
        updateRiskLevelDisplay(RiskLevel.LOW)
        btnStopService.isEnabled = false
    }

    private fun registerServiceListener() {
        val filter = IntentFilter().apply {
            addAction("ADAPTIVE_SECURITY_ALERT")
            addAction("ADAPTIVE_SECURITY_WARNING")
            addAction("ADAPTIVE_SECURITY_URGENT")
            addAction("SECURITY_STATUS_UPDATE")
            addAction("KEYSTROKE_STATUS_UPDATE")
        }
        registerReceiver(serviceReceiver, filter)
    }

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ADAPTIVE_SECURITY_ALERT" -> {
                    val message = intent.getStringExtra("message")
                    runOnUiThread { showSecurityAlert("CRITICAL ALERT", message ?: "Security threat detected!") }
                }
                "ADAPTIVE_SECURITY_WARNING" -> {
                    val message = intent.getStringExtra("message")
                    runOnUiThread { showSecurityWarning("Security Warning", message ?: "Security warning") }
                }
                "ADAPTIVE_SECURITY_URGENT" -> {
                    val message = intent.getStringExtra("message")
                    runOnUiThread { showUrgentAlert("URGENT", message ?: "Urgent security alert!") }
                }
                "SECURITY_STATUS_UPDATE" -> {
                    runOnUiThread { handleServiceStatusUpdate(intent) }
                }
                "KEYSTROKE_STATUS_UPDATE" -> {
                    runOnUiThread { handleKeystrokeUpdate(intent) }
                }
            }
        }
    }

    private fun startContextMonitoring() {
        lifecycleScope.launch {
            contextManager.startContextMonitoring()
            updateOverallStatus("✅ Context monitoring active")
        }
    }

    // 🧪 TESTING METHODS
    private fun runComprehensiveTests() {
        if (isTestRunning) return

        isTestRunning = true
        updateUIForTestStart("Running comprehensive test suite...")

        lifecycleScope.launch {
            try {
                comprehensiveTestManager.runCompleteTestSuite()
                runOnUiThread {
                    updateOverallStatus("Comprehensive tests completed!")
                    showTestResultsDialog()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    updateOverallStatus("Test suite failed: ${e.message}")
                }
            } finally {
                runOnUiThread { updateUIForTestEnd() }
            }
        }
    }

    private fun showIndividualTestMenu() {
        val options = arrayOf(
            "Test Location Collector",
            "Test Network Collector",
            "Test Device Collector",
            "Test Time Collector",
            "Test App Usage Collector",
            "Test Keystroke Collector"
        )

        AlertDialog.Builder(this)
            .setTitle("Individual Collector Tests")
            .setItems(options) { _, which ->
                runIndividualTest(which)
            }
            .show()
    }

    private fun runIndividualTest(testIndex: Int) {
        updateUIForTestStart("Running individual test...")

        lifecycleScope.launch {
            try {
                when (testIndex) {
                    0 -> comprehensiveTestManager.testLocationCollector()
                    1 -> comprehensiveTestManager.testNetworkCollector()
                    2 -> comprehensiveTestManager.testDeviceCollector()
                    3 -> comprehensiveTestManager.testTimeCollector()
                    4 -> comprehensiveTestManager.testAppUsageCollector()
                    5 -> comprehensiveTestManager.testKeystrokeCollector()
                }
                runOnUiThread {
                    updateOverallStatus("Individual test completed!")
                    showIndividualTestResults(testIndex)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    updateOverallStatus("Individual test failed: ${e.message}")
                }
            } finally {
                runOnUiThread { updateUIForTestEnd() }
            }
        }
    }

    private fun runKeystrokeTests() {
        updateUIForTestStart("Running keystroke tests...")

        lifecycleScope.launch {
            try {
                keystrokeTestManager.runFullTestSuite()
                runOnUiThread {
                    updateOverallStatus("Keystroke tests completed!")
                    showKeystrokeTestResults()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    updateOverallStatus("Keystroke tests failed: ${e.message}")
                }
            } finally {
                runOnUiThread { updateUIForTestEnd() }
            }
        }
    }

    private fun runScenarioTests() {
        updateUIForTestStart("Running scenario tests...")

        lifecycleScope.launch {
            try {
                comprehensiveTestManager.testIntegrationScenarios()
                runOnUiThread {
                    updateOverallStatus("Scenario tests completed!")
                    showScenarioTestResults()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    updateOverallStatus("Scenario tests failed: ${e.message}")
                }
            } finally {
                runOnUiThread { updateUIForTestEnd() }
            }
        }
    }

    // 🔧 SERVICE MANAGEMENT
    private fun startSecurityService() {
        val intent = Intent(this, AdaptiveSecurityService::class.java)
        startService(intent)

        isServiceRunning = true
        btnStartService.isEnabled = false
        btnStopService.isEnabled = true

        updateOverallStatus("Adaptive Security Service started")

        Toast.makeText(this, "Security service started -", Toast.LENGTH_LONG).show()
    }

    private fun stopSecurityService() {
        val intent = Intent(this, AdaptiveSecurityService::class.java)
        stopService(intent)

        isServiceRunning = false
        btnStartService.isEnabled = true
        btnStopService.isEnabled = false

        updateOverallStatus("Service stopped")

        Toast.makeText(this, "Security service stopped", Toast.LENGTH_SHORT).show()
    }

    // 📊 UI UPDATES
    private fun updateContextDisplay(context: ContextData) {
        // Update location status
        context.locationContext?.let { location ->
            tvLocationStatus.text = "${location.locationCategory} ${if (location.isKnownLocation) "(Known)" else "(Unknown)"}"
        } ?: run {
            tvLocationStatus.text = "Location unavailable"
        }

        // Update network status
        context.networkContext?.let { network ->
            val security = if (network.isSecureNetwork) "Secure" else "Insecure"
            tvNetworkStatus.text = "${network.networkType} $security"
        } ?: run {
            tvNetworkStatus.text = "Network unavailable"
        }

        // Update device status
        context.deviceContext?.let { device ->
            val charging = if (device.isCharging) "⚡" else "🔋"
            tvDeviceStatus.text = "${device.batteryLevel}% $charging"
        } ?: run {
            tvDeviceStatus.text = "Device info unavailable"
        }

        // Update time status
        val timeStatus = "${context.timeContext.timeOfDay} ${if (context.timeContext.isWorkingHours) "(Work)" else "(Non-work)"}"
        tvTimeStatus.text = "$timeStatus"

        // Update app status
        context.appUsageContext?.let { app ->
            tvAppStatus.text = "${app.currentApp} (${app.appCategory})"
        } ?: run {
            tvAppStatus.text = "App info unavailable"
        }

        // Update keystroke status
        context.keyboardContext?.let { keyboard ->
            val anomaly = if (keyboard.isAnomalous) "⚠️ Anomalous" else "✅ Normal"
            tvKeystrokeStatus.text = "$anomaly (${String.format("%.3f", keyboard.anomalyScore)})"
        } ?: run {
            tvKeystrokeStatus.text = "No keystroke data"
        }
    }

    private fun updateRiskLevelDisplay(riskLevel: RiskLevel) {
        val (icon, color) = when (riskLevel) {
            RiskLevel.LOW -> "🟢" to android.graphics.Color.GREEN
            RiskLevel.MEDIUM -> "🟡" to android.graphics.Color.rgb(255, 165, 0)
            RiskLevel.HIGH -> "🟠" to android.graphics.Color.rgb(255, 69, 0)
            RiskLevel.CRITICAL -> "🔴" to android.graphics.Color.RED
        }

        tvCurrentRisk.text = "$icon Risk Level: $riskLevel"
        tvCurrentRisk.setTextColor(color)
    }

    private fun updateOverallStatus(status: String) {
        tvOverallStatus.text = status
    }

    private fun updateUIForTestStart(message: String) {
        isTestRunning = true
        updateOverallStatus(message)
        progressBar.visibility = android.view.View.VISIBLE
        btnRunAllTests.isEnabled = false
        btnTestIndividual.isEnabled = false
        btnTestKeystrokes.isEnabled = false
        btnTestScenarios.isEnabled = false
    }

    private fun updateUIForTestEnd() {
        isTestRunning = false
        progressBar.visibility = android.view.View.GONE
        btnRunAllTests.isEnabled = true
        btnTestIndividual.isEnabled = true
        btnTestKeystrokes.isEnabled = true
        btnTestScenarios.isEnabled = true
    }

    // ALERT HANDLERS
    private fun showRiskChangeAlert(newRisk: RiskLevel, previousRisk: RiskLevel) {
        if (newRisk > previousRisk) {
            Toast.makeText(this, "⬆️ Risk escalated to $newRisk", Toast.LENGTH_LONG).show()
        } else if (newRisk < previousRisk) {
            Toast.makeText(this, "⬇️ Risk reduced to $newRisk", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showKeystrokeAnomalyAlert(keyboardContext: KeyboardContext) {
        val message = """
            Keystroke Anomaly Detected!
            
            Anomaly Score: ${String.format("%.3f", keyboardContext.anomalyScore)}
            WPM: ${String.format("%.1f", keyboardContext.averageWpm)}
            
            This could indicate unauthorized access.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Keystroke Anomaly")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Reset Baseline") { _, _ ->
                contextManager.resetKeystrokeBaseline()
            }
            .show()
    }

    private fun showSecurityAlert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle("$title")
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show()
    }

    private fun showSecurityWarning(title: String, message: String) {
        Toast.makeText(this, "⚠️ $title: $message", Toast.LENGTH_LONG).show()
    }

    private fun showUrgentAlert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle("$title")
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Acknowledge", null)
            .setCancelable(false)
            .show()
    }

    // 📊 TEST RESULT DISPLAYS
    private fun showTestResultsDialog() {
        val results = comprehensiveTestManager.getTestResults()
        val integrationResults = comprehensiveTestManager.getIntegrationResults()

        val totalTests = results.size + integrationResults.size
        val passedTests = results.count { it.passed } + integrationResults.count { it.passed }
        val accuracy = if (totalTests > 0) (passedTests * 100) / totalTests else 0

        val message = """
            Test Suite Results
            
            Total Tests: $totalTests
            Passed: $passedTests
            Accuracy: $accuracy%
            
            Status: ${when {
            accuracy >= 90 -> "EXCELLENT!"
            accuracy >= 75 -> "GOOD"
            accuracy >= 60 -> "MODERATE"
            else -> "🔧 NEEDS WORK"
        }}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Test Results")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("View Details") { _, _ ->
                showDetailedResults()
            }
            .show()
    }

    private fun showDetailedResults() {
        // Create a detailed results activity or dialog
        // For now, show in a simple dialog
        val results = comprehensiveTestManager.getTestResults()
        val sb = StringBuilder()

        results.groupBy { it.collectorName }.forEach { (collector, tests) ->
            sb.append("\n$collector:\n")
            tests.forEach { test ->
                val status = if (test.passed) "passed" else "failed"
                sb.append("  $status ${test.testName}\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Detailed Test Results")
            .setMessage(sb.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showIndividualTestResults(testIndex: Int) {
        val testNames = arrayOf("Location", "Network", "Device", "Time", "App Usage", "Keystroke")
        val testName = testNames[testIndex]

        Toast.makeText(this, "$testName test completed - check console for details", Toast.LENGTH_LONG).show()
    }

    private fun showKeystrokeTestResults() {
        val results = keystrokeTestManager.getTestResults()
        val accuracy = if (results.isNotEmpty()) {
            (results.count { it.expectedResult == TestExpectation.NORMAL && !it.isAnomalous ||
                    it.expectedResult == TestExpectation.ANOMALOUS && it.isAnomalous } * 100) / results.size
        } else 0

        AlertDialog.Builder(this)
            .setTitle("Keystroke Test Results")
            .setMessage("Keystroke detection accuracy: $accuracy%")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showScenarioTestResults() {
        Toast.makeText(this, "Scenario tests completed - check console for details", Toast.LENGTH_LONG).show()
    }

    // Service status updates
    private fun handleServiceStatusUpdate(intent: Intent) {
        val action = intent.getStringExtra("action")
        val riskLevel = intent.getStringExtra("risk_level")

        updateOverallStatus("Service Update: $action (Risk: $riskLevel)")
    }

    private fun handleKeystrokeUpdate(intent: Intent) {
        val anomalyScore = intent.getDoubleExtra("anomaly_score", 0.0)
        val isAnomalous = intent.getBooleanExtra("is_anomalous", false)

        if (isAnomalous) {
            tvKeystrokeStatus.text = "Anomalous (${String.format("%.3f", anomalyScore)})"
            tvKeystrokeStatus.setTextColor(android.graphics.Color.RED)
        } else {
            tvKeystrokeStatus.text = "Normal (${String.format("%.3f", anomalyScore)})"
            tvKeystrokeStatus.setTextColor(android.graphics.Color.GREEN)
        }
    }

    // Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.testing_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset_baseline -> {
                contextManager.resetKeystrokeBaseline()
                Toast.makeText(this, "Keystroke baseline reset", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_show_debug -> {
                showDebugInfo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDebugInfo() {
        val debugInfo = contextManager.getKeystrokeDebugInfo()
        AlertDialog.Builder(this)
            .setTitle("Debug Information")
            .setMessage(debugInfo)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serviceReceiver)
        lifecycleScope.launch {
            contextManager.stopContextMonitoring()
        }
    }
}

/*
Layout file: res/layout/activity_main_testing.xml

<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/svScrollView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <!-- Header -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🛡️ Adaptive Security Testing"
            android:textSize="24sp"
            android:textStyle="bold"
            android:gravity="center"
            android:layout_marginBottom="16dp" />

        <!-- Overall Status -->
        <TextView
            android:id="@+id/tvOverallStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="System Status: Initializing..."
            android:textSize="16sp"
            android:padding="12dp"
            android:background="#E8F5E8"
            android:layout_marginBottom="16dp" />

        <!-- Current Risk Level -->
        <TextView
            android:id="@+id/tvCurrentRisk"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🟢 Risk Level: LOW"
            android:textSize="20sp"
            android:textStyle="bold"
            android:gravity="center"
            android:padding="12dp"
            android:background="#F0F8F0"
            android:layout_marginBottom="24dp" />

        <!-- Context Status Section -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📊 Current Context Status"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <TextView
            android:id="@+id/tvLocationStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📍 Location: Unknown"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tvNetworkStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🌐 Network: Unknown"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tvDeviceStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📱 Device: Unknown"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tvTimeStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="⏰ Time: Unknown"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tvAppStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📱 App: Unknown"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tvKeystrokeStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="⌨️ Keystroke: No data"
            android:textSize="14sp"
            android:layout_marginBottom="24dp" />

        <!-- Service Control -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🔧 Service Control"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="24dp">

            <Button
                android:id="@+id/btnStartService"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="🚀 Start Service"
                android:layout_marginEnd="8dp" />

            <Button
                android:id="@+id/btnStopService"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="⏹️ Stop Service"
                android:layout_marginStart="8dp" />

        </LinearLayout>

        <!-- Testing Section -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🧪 Testing Controls"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <Button
            android:id="@+id/btnRunAllTests"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🧪 Run Complete Test Suite"
            android:layout_marginBottom="8dp" />

        <Button
            android:id="@+id/btnTestIndividual"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🔍 Test Individual Collectors"
            android:layout_marginBottom="8dp" />

        <Button
            android:id="@+id/btnTestKeystrokes"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="⌨️ Test Keystroke Detection"
            android:layout_marginBottom="8dp" />

        <Button
            android:id="@+id/btnTestScenarios"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="🎯 Test Integration Scenarios"
            android:layout_marginBottom="16dp" />

        <!-- Progress Indicator -->
        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:visibility="gone"
            android:layout_marginBottom="16dp" />

        <!-- Instructions -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📋 Instructions:\n• Start the service for 24/7 protection\n• Run tests to validate all components\n• Watch real-time context updates above\n• Check console output for detailed results"
            android:textSize="12sp"
            android:background="#F0F8FF"
            android:padding="12dp" />

    </LinearLayout>
</ScrollView>

Menu file: res/menu/testing_menu.xml

<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/action_reset_baseline"
        android:title="Reset Keystroke Baseline"
        android:icon="@android:drawable/ic_menu_revert" />
    <item
        android:id="@+id/action_show_debug"
        android:title="Show Debug Info"
        android:icon="@android:drawable/ic_menu_info_details" />
</menu>
*/