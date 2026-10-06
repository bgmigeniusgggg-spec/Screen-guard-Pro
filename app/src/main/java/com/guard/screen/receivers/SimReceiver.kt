package com.guard.screen.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.services.GuardService

class SimReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED"
        private const val KEY_OLD_CARRIER = "old_carrier"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != ACTION_SIM_STATE_CHANGED) return

        try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val carrier = tm.networkOperatorName ?: "unknown"
            val simState = intent.getStringExtra("ss") ?: "unknown"

            // Setup done check karo
            val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)

            if (!setupDone) return

            // SIM change detect
            val oldCarrier = prefs.getString(KEY_OLD_CARRIER, null)
            if (oldCarrier != null && oldCarrier != carrier && carrier != "unknown") {
                Logger.w("SimReceiver", "SIM changed! Old=$oldCarrier New=$carrier")
                prefs.edit().putString(KEY_OLD_CARRIER, carrier).apply()

                // GuardService ensure karo
                ensureServiceRunning(ctx)
            } else if (oldCarrier == null) {
                // Pehli baar — save karo
                prefs.edit().putString(KEY_OLD_CARRIER, carrier).apply()
            }

            Logger.d("SimReceiver", "SIM event: $simState / $carrier")

        } catch (e: Exception) {
            Logger.e("SimReceiver", "Failed", e)
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
            Logger.e("SimReceiver", "Service start failed", e)
        }
    }
}
