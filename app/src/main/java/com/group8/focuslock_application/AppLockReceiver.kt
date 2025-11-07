package com.group8.focuslock_application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppLockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == TimeLimitNotificationService.ACTION_LOCK_APP) {
            val packageName = intent.getStringExtra(TimeLimitNotificationService.EXTRA_PACKAGE_NAME)
            if (packageName != null && context != null) {
                AppBlocker.blockApp(context, packageName)
            }
        }
    }
}