package com.guard.screen.ui

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
import com.guard.screen.ui.screen.PermissionScreen
import com.guard.screen.ui.theme.ScreenGuardTheme
import com.guard.screen.ui.viewmodel.PermissionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScreenGuardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: PermissionViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    PermissionScreen(
                        uiState = uiState,
                        onGrantAll = { viewModel.grantAllPermissions(this) },
                        onBack = { finish() },
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permissions state
    }
}
