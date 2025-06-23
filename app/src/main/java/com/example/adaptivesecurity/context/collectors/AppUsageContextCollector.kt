package com.example.adaptivesecurity.context.collectors

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import com.example.adaptivesecurity.context.models.AppCategory
import com.example.adaptivesecurity.context.models.AppUsageContext
import java.util.concurrent.TimeUnit

class AppUsageContextCollector(private val context: Context) {

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    fun collectAppUsageContext(): AppUsageContext? {
        if (!hasUsageStatsPermission()) return null

        val currentAppPackage = getForegroundAppPackage() ?: return null
        val appName = getAppName(currentAppPackage)

        return AppUsageContext(
            currentApp = appName,
            appCategory = categorizeApp(currentAppPackage),
            packageName = currentAppPackage
        )
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        if (appOps == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false
        }

        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getForegroundAppPackage(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - TimeUnit.MINUTES.toMillis(1) // 1 minute window

        return usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            beginTime,
            endTime
        )?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    private fun categorizeApp(packageName: String): AppCategory {
        return when {
            isBankingApp(packageName) -> AppCategory.BANKING
            isSocialMediaApp(packageName) -> AppCategory.SOCIAL
            isCommunicationApp(packageName) -> AppCategory.COMMUNICATION
            else -> AppCategory.OTHER
        }
    }

    private fun isBankingApp(packageName: String): Boolean {
        val bankingKeywords = listOf("bank", "finance", "payment", "wallet", "invest")
        return bankingKeywords.any { packageName.contains(it, ignoreCase = true) }
    }

    private fun isSocialMediaApp(packageName: String): Boolean {
        val socialKeywords = listOf("social", "chat", "message", "media", "facebook", "instagram", "twitter", "whatsapp")
        return socialKeywords.any { packageName.contains(it, ignoreCase = true) }
    }

    private fun isCommunicationApp(packageName: String): Boolean {
        val commKeywords = listOf("email", "gmail", "outlook", "messenger", "sms", "mms", "telegram")
        return commKeywords.any { packageName.contains(it, ignoreCase = true) }
    }
}
