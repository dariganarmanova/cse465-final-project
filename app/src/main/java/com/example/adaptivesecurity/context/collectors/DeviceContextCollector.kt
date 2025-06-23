package com.example.adaptivesecurity.context.collectors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.adaptivesecurity.context.models.DeviceContext
import java.util.concurrent.TimeUnit

class DeviceContextCollector(private val context: Context) {

    private val powerManager: PowerManager? by lazy {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    fun collectDeviceContext(): DeviceContext {
        return DeviceContext(
            batteryLevel = getBatteryLevel(),
            isCharging = isCharging(),
            isDeviceLocked = isDeviceLocked(),
            lastUnlockTime = getLastUnlockTime()
        )
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    }

    private fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isDeviceLocked(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isInteractive == false
        } else {
            @Suppress("DEPRECATION")
            powerManager?.isScreenOn == false
        }
    }

    private fun getLastUnlockTime(): Long {
        return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)
    }
}
