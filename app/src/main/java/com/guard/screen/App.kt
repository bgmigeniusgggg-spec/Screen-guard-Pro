package com.guard.screen

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.guard.screen.core.Constants
import com.guard.screen.core.DeviceKey
import com.guard.screen.core.Logger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Logger initialize
        Logger.init()

        // Notification channel banao (Android 8+)
        createNotificationChannels()

        // Device key ensure karo
        DeviceKey.get(this)

        Logger.i("App initialized - v${BuildConfig.VERSION_NAME}")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Main service channel
            val serviceChannel = NotificationChannel(
                Constants.CHANNEL_SERVICE,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(serviceChannel)

            // Recording channel
            val recordingChannel = NotificationChannel(
                Constants.CHANNEL_RECORDING,
                "Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            manager.createNotificationChannel(recordingChannel)

            // Live channel
            val liveChannel = NotificationChannel(
                Constants.CHANNEL_LIVE,
                "Live Stream",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            manager.createNotificationChannel(liveChannel)
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
