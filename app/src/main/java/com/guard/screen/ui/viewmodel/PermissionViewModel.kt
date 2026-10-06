package com.guard.screen.ui.viewmodel

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.receivers.AdminReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PermissionUiState(
    val runtimeGranted: Boolean = false,
    val adminGranted: Boolean = false,
    val batteryOptimized: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val exactAlarmGranted: Boolean = false
) {
    val totalCount: Int = 6
    val grantedCount: Int
        get() = listOf(
            runtimeGranted,
            adminGranted,
            batteryOptimized,
            accessibilityGranted,
            overlayGranted,
            exactAlarmGranted
        ).count { it }
}

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = PermissionUiState(
            runtimeGranted = hasRuntimePermissions(),
            adminGranted = isAdminActive(),
            batteryOptimized = isBatteryOptimizationIgnored(),
            accessibilityGranted = isAccessibilityEnabled(),
            overlayGranted = canDrawOverlays(),
            exactAlarmGranted = canScheduleExactAlarms()
        )
    }

    fun grantAllPermissions(activity: Activity) {
        // Runtime permissions
        val permissions = getRuntimePermissions()
        ActivityCompat.requestPermissions(activity, permissions, 1001)
    }

    // ============================================
    // CHECKS
    // ============================================

    private fun hasRuntimePermissions(): Boolean {
        return getRuntimePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRuntimePermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            list.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        return list.toTypedArray()
    }

    private fun isAdminActive(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.isAdminActive(ComponentName(context, AdminReceiver::class.java))
        } catch (_: Exception) { false }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        return try {
            val pw = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pw.isIgnoringBatteryOptimizations(context.packageName)
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

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                am.canScheduleExactAlarms()
            } catch (_: Exception) { false }
        } else true
    }
}
