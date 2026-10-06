package com.guard.screen.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.services.GuardService

class PackageReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action ?: return
        val pkg = intent.data?.schemeSpecificPart ?: return

        // Apni app skip karo
        if (pkg == ctx.packageName) return

        try {
            // Setup done check karo
            val prefs = ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)

            if (!setupDone) return

            val event = when (action) {
                Intent.ACTION_PACKAGE_ADDED -> "installed"
                Intent.ACTION_PACKAGE_REMOVED -> "uninstalled"
                else -> return
            }

            // Update skip karo
            val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (replacing) return

            Logger.d("PackageReceiver", "$event: $pkg")

            // Service ensure karo — safety check
            ensureServiceRunning(ctx)

        } catch (e: Exception) {
            Logger.e("PackageReceiver", "Failed", e)
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
            Logger.e("PackageReceiver", "Service start failed", e)
        }
    }
}
