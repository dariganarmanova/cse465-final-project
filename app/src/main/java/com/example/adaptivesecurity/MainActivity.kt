package com.example.adaptivesecurity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.adaptivesecurity.context.ContextRecognitionManager
import com.example.adaptivesecurity.context.listeners.ContextListener
import com.example.adaptivesecurity.context.models.ContextData
import com.example.adaptivesecurity.context.models.RiskLevel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), ContextListener {

    private lateinit var contextRecognitionManager: ContextRecognitionManager
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var dashboardButton: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }

    private val usageStatsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasUsageStatsPermission()) {
            initializeSecuritySystem()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()

        contextRecognitionManager = ContextRecognitionManager(this)
        requestNecessaryPermissions()
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        dashboardButton = findViewById(R.id.dashboardButton)
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            if (hasAllRequiredPermissions()) {
                initializeSecuritySystem()
            } else {
                requestNecessaryPermissions()
            }
        }

        dashboardButton.setOnClickListener {
            // Dashboard functionality removed
            Toast.makeText(this, "Dashboard not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNecessaryPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.POST_NOTIFICATIONS
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkUsageStatsPermission()
        }
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkUsageStatsPermission()
        } else {
            statusText.text = "Please grant all permissions to continue"
            Toast.makeText(this, "All permissions are required for the app to function", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun hasAllRequiredPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.POST_NOTIFICATIONS
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageStatsPermissionLauncher.launch(intent)
        } else {
            initializeSecuritySystem()
        }
    }

    private fun initializeSecuritySystem() {
        statusText.text = "Initializing Adaptive Security System..."

        contextRecognitionManager.registerContextListener(this)

        lifecycleScope.launch {
            contextRecognitionManager.startContextMonitoring()

            statusText.text = "Security System Active"
            startButton.text = "System Running"
            startButton.isEnabled = false
        }
    }

    override fun onContextChanged(contextData: ContextData) {
        runOnUiThread {
            updateSecurityStatus(contextData)
        }
    }

    override fun onRiskLevelChanged(newRiskLevel: RiskLevel, previousRiskLevel: RiskLevel) {
        runOnUiThread {
            handleRiskLevelChange(newRiskLevel, previousRiskLevel)
        }
    }

    private fun updateSecurityStatus(contextData: ContextData) {
        val riskText = when (contextData.riskLevel) {
            RiskLevel.LOW -> "Security Status: LOW RISK"
            RiskLevel.MEDIUM -> "Security Status: MEDIUM RISK"
            RiskLevel.HIGH -> "Security Status: HIGH RISK"
            RiskLevel.CRITICAL -> "Security Status: CRITICAL RISK"
        }
        statusText.text = riskText
    }

    private fun handleRiskLevelChange(newLevel: RiskLevel, previousLevel: RiskLevel) {
        if (newLevel == RiskLevel.HIGH || newLevel == RiskLevel.CRITICAL) {
            Toast.makeText(this, "Security risk level elevated to $newLevel", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::contextRecognitionManager.isInitialized) {
            contextRecognitionManager.unregisterContextListener(this)
            lifecycleScope.launch {
                contextRecognitionManager.stopContextMonitoring()
            }
        }
    }
}
