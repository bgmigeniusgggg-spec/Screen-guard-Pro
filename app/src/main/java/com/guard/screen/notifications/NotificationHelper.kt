package com.guard.screen.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.guard.screen.R
import com.guard.screen.core.Constants
import com.guard.screen.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun buildBaseNotification(
        title: String,
        text: String,
        icon: Int = R.drawable.ic_notification
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Constants.CHANNEL_SERVICE)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(buildContentIntent())
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun buildServiceNotification(): Notification {
        return buildBaseNotification(
            title = context.getString(R.string.notification_title),
            text = context.getString(R.string.notification_text)
        ).build()
    }

    fun buildKeepAliveNotification(): Notification {
        return buildBaseNotification(
            title = context.getString(R.string.notification_title),
            text = "Keep-alive"
        ).build()
    }
}
