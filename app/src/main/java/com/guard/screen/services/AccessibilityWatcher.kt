package com.guard.screen.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger

/**
 * Accessibility service.
 * Har window change pe GuardService check karta hai — agar band hai toh start karta hai.
 */
class AccessibilityWatcher : AccessibilityService() {

    private var lastCheck = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Logger.i("AccessibilityWatcher", "Service connected")

        // Ensure GuardService is running
        ensureServicesRunning()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        try {
            // Rate limit — 30 sec mein ek baar check
            val now = System.currentTimeMillis()
            if (now - lastCheck < 30_000L) return
            lastCheck = now

            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    ensureServicesRunning()
                }
            }
        } catch (e: Exception) {
            Logger.e("AccessibilityWatcher", "Event error", e)
        }
    }

    private fun ensureServicesRunning() {
        try {
            val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)

            if (!setupDone) return

            // GuardService start karo
            val guardIntent = Intent(this, GuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(guardIntent)
            } else {
                startService(guardIntent)
            }

            // KeepAliveService bhi
            val keepAliveIntent = Intent(this, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(keepAliveIntent)
            } else {
                startService(keepAliveIntent)
            }
        } catch (e: Exception) {
            Logger.e("AccessibilityWatcher", "Ensure services failed", e)
        }
    }

    override fun onInterrupt() {
        Logger.d("AccessibilityWatcher", "Interrupted")
    }

    override fun onDestroy() {
        Logger.d("AccessibilityWatcher", "Destroyed")
        super.onDestroy()
    }
}
