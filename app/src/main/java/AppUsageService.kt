package com.example.focuslock_application

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.group8.focuslock_application.AppsOnTimeout

class AppUsageService : AccessibilityService() {

    // For tracking app usage
    private var currentApp: String? = null
    private var startTime: Long = 0
    private val usageMap = mutableMapOf<String, Long>()
    private val handler = Handler(Looper.getMainLooper())

    // Example: 30 minutes limit (in milliseconds)
    private val usageLimit = 30 * 60 * 1000L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (currentApp != packageName) {
                // When app changes, calculate how long the previous app was used
                currentApp?.let { previousApp ->
                    val duration = System.currentTimeMillis() - startTime
                    usageMap[previousApp] = (usageMap[previousApp] ?: 0) + duration
                    Log.d("AppUsageService", "$previousApp used for $duration ms")
                }

                // Reset tracking for new app
                currentApp = packageName
                startTime = System.currentTimeMillis()
            }

            // Check if the current app exceeded the limit
            checkUsageLimit(packageName)
        }
    }

    private fun checkUsageLimit(packageName: String) {
        val usedTime = usageMap[packageName] ?: 0L

        if (usedTime >= usageLimit) {
            Log.d("AppUsageService", "Limit reached for $packageName")

            // Launch your lock screen (AppsOnTimeout)
            val intent = Intent(this, AppsOnTimeout::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // Required method; called when the service is interrupted
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AppUsageService", "Accessibility Service Connected")
    }
}
