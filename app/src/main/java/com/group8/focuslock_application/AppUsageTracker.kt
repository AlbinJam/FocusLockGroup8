package com.group8.focuslock_application

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class AppUsageTracker(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_usage", Context.MODE_PRIVATE)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    companion object {
        const val PREF_USAGE_PREFIX = "usage_"
        const val PREF_WARNING_SHOWN = "warning_shown_"
    }

    fun getAppUsageToday(packageName: String): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            System.currentTimeMillis()
        )

        var totalUsage = 0L
        for (stat in stats) {
            if (stat.packageName == packageName) {
                totalUsage = stat.totalTimeInForeground
                break
            }
        }

        return totalUsage
    }

    fun getAppUsageTodayMinutes(packageName: String): Int {
        return (getAppUsageToday(packageName) / 1000 / 60).toInt()
    }

    fun setWarningShown(packageName: String, shown: Boolean) {
        sharedPreferences.edit().putBoolean("$PREF_WARNING_SHOWN$packageName", shown).apply()
    }

    fun isWarningShown(packageName: String): Boolean {
        return sharedPreferences.getBoolean("$PREF_WARNING_SHOWN$packageName", false)
    }

    fun resetDailyWarnings() {
        val editor = sharedPreferences.edit()
        sharedPreferences.all.keys.filter { it.startsWith(PREF_WARNING_SHOWN) }.forEach {
            editor.remove(it)
        }
        editor.apply()
    }

    fun getRemainingMinutes(packageName: String, limitMinutes: Int): Int {
        val usedMinutes = getAppUsageTodayMinutes(packageName)
        return maxOf(0, limitMinutes - usedMinutes)
    }

    fun hasReachedLimit(packageName: String, limitMinutes: Int): Boolean {
        return getRemainingMinutes(packageName, limitMinutes) <= 0
    }

    fun getWarningState(packageName: String): Boolean {
        return sharedPreferences.getBoolean("$PREF_WARNING_SHOWN$packageName", false)
    }
}