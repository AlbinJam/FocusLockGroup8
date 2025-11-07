package com.group8.focuslock_application

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent

class AccessibilityHandler : AccessibilityService() {

    companion object {
        private lateinit var blockedAppsPreference: SharedPreferences
        const val BLOCKED_APPS_PREF = "blocked_apps"
    }

    override fun onCreate() {
        super.onCreate()
        blockedAppsPreference = getSharedPreferences(BLOCKED_APPS_PREF, Context.MODE_PRIVATE)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            if (isAppBlocked(packageName)) {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        setServiceInfo(info)
    }

    private fun isAppBlocked(packageName: String): Boolean {
        return blockedAppsPreference.getBoolean(packageName, false)
    }
}

object AppBlocker {
    private const val BLOCKED_APPS_PREF = "blocked_apps"

    fun blockApp(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(BLOCKED_APPS_PREF, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(packageName, true).apply()
    }

    fun unblockApp(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(BLOCKED_APPS_PREF, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(packageName, false).apply()
    }

    fun isAppBlocked(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(BLOCKED_APPS_PREF, Context.MODE_PRIVATE)
        return prefs.getBoolean(packageName, false)
    }
}