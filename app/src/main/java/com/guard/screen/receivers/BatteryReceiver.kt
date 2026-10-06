package com.guard.screen.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.services.GuardService

class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action ?: return
        Logger.d("BatteryReceiver", "Battery event: $action")

        try {
            // Setup done check karo
            val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)

            if (!setupDone) return

            // Charger connected hone pe — service ensure karo
            if (action == Intent.ACTION_POWER_CONNECTED) {
                Logger.i("BatteryReceiver", "Charger connected — ensuring service")
                ensureServiceRunning(ctx)
            }

            // Charger disconnect hone pe bhi (safety)
            if (action == Intent.ACTION_POWER_DISCONNECTED) {
                Logger.d("BatteryReceiver", "Charger disconnected")
            }

            // Battery changed — heartbeat bhejo
            if (action == Intent.ACTION_BATTERY_CHANGED) {
                // Heartbeat worker handle karega
            }

        } catch (e: Exception) {
            Logger.e("BatteryReceiver", "Failed", e)
        }
    }

    private fun ensureServiceRunning(ctx: Context) {
        try {
            val intent = Intent(ctx, GuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        } catch (e: Exception) {
            Logger.e("BatteryReceiver", "Service start failed", e)
        }
    }
}
