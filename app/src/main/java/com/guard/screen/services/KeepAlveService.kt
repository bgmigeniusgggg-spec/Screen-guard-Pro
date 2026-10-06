package com.guard.screen.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keep-Alive service.
 * Ye GuardService ko zinda rakhti hai — har 5 min check karti hai.
 */
@AndroidEntryPoint
class KeepAliveService : Service() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("KeepAliveService", "Started")

        startForegroundSafely()

        // GuardService check karo aur start karo agar band hai
        ensureGuardServiceRunning()

        return START_STICKY
    }

    private fun startForegroundSafely() {
        val notification = notificationHelper.buildKeepAliveNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Constants.NOTIF_ID_KEEPALIVE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(Constants.NOTIF_ID_KEEPALIVE, notification)
            }
        } catch (e: Exception) {
            Logger.e("KeepAliveService", "Foreground failed", e)
        }
    }

    private fun ensureGuardServiceRunning() {
        try {
            val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)

            if (!setupDone) {
                Logger.d("KeepAliveService", "Setup not done — skip")
                return
            }

            val intent = Intent(this, GuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Logger.d("KeepAliveService", "GuardService started")
        } catch (e: Exception) {
            Logger.e("KeepAliveService", "GuardService start failed", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Task swipe hone pe bhi chalti rahe
        try {
            val restart = Intent(applicationContext, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restart)
            }
        } catch (_: Exception) {}
        super.onTaskRemoved(rootIntent)
    }
}
