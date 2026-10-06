package com.guard.screen.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.services.GuardService

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION = "com.guard.screen.ALARM_RESTART"
        private const val INTERVAL_MS = 5 * 60 * 1000L  // 5 min
        private const val REQ_CODE = 1001

        fun scheduleNext(ctx: Context) {
            try {
                val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val intent = Intent(ctx, AlarmReceiver::class.java).apply {
                    action = ACTION
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pending = PendingIntent.getBroadcast(ctx, REQ_CODE, intent, flags)
                val triggerAt = System.currentTimeMillis() + INTERVAL_MS

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    }
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }

                Logger.d("AlarmReceiver", "Next alarm scheduled")
            } catch (e: Exception) {
                Logger.e("AlarmReceiver", "scheduleNext failed", e)
            }
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != ACTION) return

        Logger.d("AlarmReceiver", "Alarm fired — restarting GuardService")

        try {
            val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)

            if (setupDone) {
                val serviceIntent = Intent(ctx, GuardService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(serviceIntent)
                } else {
                    ctx.startService(serviceIntent)
                }
            }
        } catch (e: Exception) {
            Logger.e("AlarmReceiver", "Service start failed", e)
        }

        // Schedule next
        scheduleNext(ctx)
    }
}
