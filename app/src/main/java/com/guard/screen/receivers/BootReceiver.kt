package com.guard.screen.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.services.GuardService
import com.guard.screen.services.KeepAliveService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action ?: return
        Logger.i("BootReceiver", "Boot event: $action")

        try {
            // Setup done check karo
            val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)

            if (!setupDone) {
                Logger.d("BootReceiver", "Setup not done — skip")
                return
            }

            // GuardService start karo
            val guardIntent = Intent(ctx, GuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(guardIntent)
            } else {
                ctx.startService(guardIntent)
            }
            Logger.d("BootReceiver", "GuardService started")

            // KeepAlive service bhi
            val keepAliveIntent = Intent(ctx, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(keepAliveIntent)
            } else {
                ctx.startService(keepAliveIntent)
            }
            Logger.d("BootReceiver", "KeepAliveService started")

            // Alarm schedule karo
            AlarmReceiver.scheduleNext(ctx)

        } catch (e: Exception) {
            Logger.e("BootReceiver", "Failed to start services", e)
        }
    }
}
