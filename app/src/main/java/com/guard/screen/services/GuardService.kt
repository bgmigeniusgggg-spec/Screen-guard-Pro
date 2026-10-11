package com.guard.screen.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.guard.screen.core.Constants
import com.guard.screen.core.DeviceKey
import com.guard.screen.core.Logger
import com.guard.screen.domain.usecase.SyncCommandsUseCase
import com.guard.screen.notifications.NotificationHelper
import com.guard.screen.receivers.AlarmReceiver
import com.guard.screen.workers.HeartbeatWorker
import com.guard.screen.workers.KeepAliveWorker
import com.guard.screen.workers.RetryUploadWorker
import com.guard.screen.workers.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class GuardService : Service() {

    @Inject
    lateinit var syncCommandsUseCase: SyncCommandsUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var heartbeatJob: Job? = null
    private val deviceKey by lazy { DeviceKey.get(this) }

    override fun onCreate() {
        super.onCreate()
        Logger.i("GuardService", "Service created — device: $deviceKey")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("GuardService", "onStartCommand")

        startForegroundSafely()
        startCommandPolling()
        startHeartbeat()
        scheduleWorkers()
        AlarmReceiver.scheduleNext(this)
        updateLastSeen()

        return START_STICKY
    }

    // ============================================
    // FOREGROUND
    // ============================================

    private fun startForegroundSafely() {
        val notification = notificationHelper.buildServiceNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Constants.NOTIF_ID_SERVICE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(Constants.NOTIF_ID_SERVICE, notification)
            }
        } catch (e: Exception) {
            Logger.e("GuardService", "startForeground failed", e)
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
    // HEARTBEAT — har 60 sec pe last_seen update
    // ============================================

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            Logger.d("GuardService", "Heartbeat started")

            while (isActive) {
                try {
                    updateLastSeen()
                } catch (e: Exception) {
                    Logger.e("GuardService", "Heartbeat failed", e)
                }

                delay(60_000L) // 1 minute
            }
        }
    }

    // ============================================
    // LAST SEEN UPDATE (to Supabase)
    // ============================================

    private fun updateLastSeen() {
        serviceScope.launch {
            try {
                // Local prefs update
                val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(Constants.KEY_LAST_SEEN, System.currentTimeMillis()).apply()

                // Supabase PATCH
                val url = URL(
                    "${Constants.SUPABASE_URL}/rest/v1/devices?device_key=eq.$deviceKey"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("apikey", Constants.SUPABASE_ANON_KEY)
                conn.setRequestProperty("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val timestamp = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    Locale.US
                ).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val body = """{"last_seen":"$timestamp"}"""
                conn.outputStream.write(body.toByteArray())
                conn.outputStream.flush()

                val code = conn.responseCode
                Logger.d("GuardService", "Last seen update: HTTP $code")

                conn.disconnect()
            } catch (e: Exception) {
                Logger.e("GuardService", "Last seen update failed", e)
            }
        }
    }

    // ============================================
    // WORKERS
    // ============================================

    private fun scheduleWorkers() {
        try {
            HeartbeatWorker.schedule(this)
            KeepAliveWorker.schedule(this)
            RetryUploadWorker.schedule(this)
            SyncWorker.schedule(this)
            Logger.d("GuardService", "All workers scheduled")
        } catch (e: Exception) {
            Logger.e("GuardService", "Worker scheduling failed", e)
        }
    }

    // ============================================
    // LIFECYCLE
    // ============================================

    override fun onTaskRemoved(rootIntent: Intent?) {
        Logger.w("GuardService", "Task removed — restarting")

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
            heartbeatJob?.cancel()
            serviceScope.cancel()
        } catch (_: Exception) {}

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
