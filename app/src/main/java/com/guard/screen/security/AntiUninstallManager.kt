package com.guard.screen.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.guard.screen.core.Logger
import com.guard.screen.receivers.AdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AntiUninstallManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Protection status.
     */
    data class ProtectionStatus(
        val adminActive: Boolean,
        val accessibilityEnabled: Boolean,
        val batteryOptimized: Boolean,
        val notificationListenerEnabled: Boolean,
        val overlayGranted: Boolean,
        val exactAlarmGranted: Boolean
    ) {
        val score: Int
            get() {
                var s = 0
                if (adminActive) s += 25
                if (accessibilityEnabled) s += 25
                if (!batteryOptimized) s += 20
                if (notificationListenerEnabled) s += 10
                if (overlayGranted) s += 10
                if (exactAlarmGranted) s += 10
                return s
            }

            val isFullyProtected: Boolean
                get() = score >= 80
        }

    /**
     * Current protection status lo.
     */
    fun getStatus(): ProtectionStatus {
        return ProtectionStatus(
            adminActive = isAdminActive(),
            accessibilityEnabled = isAccessibilityEnabled(),
            batteryOptimized = isBatteryOptimizationIgnored(),
            notificationListenerEnabled = isNotificationListenerEnabled(),
            overlayGranted = canDrawOverlays(),
            exactAlarmGranted = canScheduleExactAlarms()
        )
    }

    // ============================================
    // CHECKS
    // ============================================

    private fun isAdminActive(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.isAdminActive(ComponentName(context, AdminReceiver::class.java))
        } catch (_: Exception) { false }
    }

    private fun isAccessibilityEnabled(): Boolean {
        return try {
            val service = "${context.packageName}/com.guard.screen.services.AccessibilityWatcher"
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabled.contains(service)
        } catch (_: Exception) { false }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        return try {
            val pw = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pw.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: Exception) { false }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        return try {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            enabled.contains(context.packageName)
        } catch (_: Exception) { false }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE)
                        as android.app.AlarmManager
                am.canScheduleExactAlarms()
            } catch (_: Exception) { false }
        } else true
    }

    /**
     * Warning log karo agar protection kam hai.
     */
    fun logStatus() {
        val status = getStatus()
        Logger.i("AntiUninstall", "Protection score: ${status.score}/100")
        if (!status.isFullyProtected) {
            Logger.w("AntiUninstall", "Protection incomplete — some features may fail")
        }
    }
}
