package com.group8.focuslock_application

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class TimeLimitNotificationService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var dbHelper: TimeLimitDatabase
    private lateinit var usageTracker: AppUsageTracker
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 60000L

    companion object {
        const val CHANNEL_ID = "focuslock_limits"
        const val NOTIFICATION_ID_WARNING = 1001
        const val NOTIFICATION_ID_LOCKED = 1002
        const val ACTION_LOCK_APP = "com.group8.focuslock_application.LOCK_APP"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        dbHelper = TimeLimitDatabase(this)
        usageTracker = AppUsageTracker(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FocusLock App Limits",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for app usage limits"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                checkAppLimits()
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun checkAppLimits() {
        val limits = dbHelper.getChildAppLimits("current_child")

        for (limit in limits) {
            if (!limit.enabled) continue

            val usageMinutes = usageTracker.getAppUsageTodayMinutes(limit.packageName)
            val remainingMinutes = limit.dailyLimitMinutes - usageMinutes

            when {
                remainingMinutes <= 0 -> {
                    showLockedNotification(limit.appName, limit.packageName)
                    sendBroadcast(Intent(ACTION_LOCK_APP).apply {
                        putExtra(EXTRA_PACKAGE_NAME, limit.packageName)
                    })
                    usageTracker.setWarningShown(limit.packageName, false)
                }
                remainingMinutes <= 5 && !usageTracker.isWarningShown(limit.packageName) -> {
                    showWarningNotification(limit.appName, remainingMinutes)
                    usageTracker.setWarningShown(limit.packageName, true)
                }
                remainingMinutes > 5 && usageTracker.isWarningShown(limit.packageName) -> {
                    usageTracker.setWarningShown(limit.packageName, false)
                }
            }
        }
    }

    private fun showWarningNotification(appName: String, remainingMinutes: Int) {
        val warningText = if (remainingMinutes == 1) {
            "$appName will lock in 1 minute!"
        } else {
            "$appName will lock in $remainingMinutes minutes!"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time Limit Warning")
            .setContentText(warningText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()

        notificationManager.notify(NOTIFICATION_ID_WARNING, notification)
    }

    private fun showLockedNotification(appName: String, packageName: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("App Locked")
            .setContentText("$appName has reached its daily limit")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_LOCKED, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}