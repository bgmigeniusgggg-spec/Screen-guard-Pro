package com.guard.screen.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.guard.screen.core.Constants
import com.guard.screen.services.GuardService
import com.guard.screen.ui.screen.MainScreen
import com.guard.screen.ui.theme.ScreenGuardTheme
import com.guard.screen.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Guard service check aur start karo (agar setup done hai)
        checkAndStartService()

        setContent {
            ScreenGuardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: MainViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    MainScreen(
                        uiState = uiState,
                        onEnableAdmin = { openAdminScreen() },
                        onGrantPermissions = { openPermissionScreen() },
                        onStartSetup = { openSetupScreen() },
                        onHideAndStart = { viewModel.hideAndStart() },
                        onCopyKey = { viewModel.copyDeviceKey() },
                        onShowIcon = { viewModel.showIcon() }
                    )
                }
            }
        }
    }

    private fun checkAndStartService() {
        try {
            val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)
            if (setupDone) {
                val intent = Intent(this, GuardService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        } catch (_: Exception) {}
    }

    private fun openAdminScreen() {
        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        val adminComp = android.content.ComponentName(
            this,
            com.guard.screen.receivers.AdminReceiver::class.java
        )
        intent.putExtra(
            android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            adminComp
        )
        try {
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun openPermissionScreen() {
        startActivity(Intent(this, PermissionActivity::class.java))
    }

    private fun openSetupScreen() {
        startActivity(Intent(this, SetupActivity::class.java))
    }
}
