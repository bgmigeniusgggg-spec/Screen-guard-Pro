package com.guard.screen.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guard.screen.ui.components.PermissionStatusRow
import com.guard.screen.ui.components.PrimaryButton
import com.guard.screen.ui.viewmodel.PermissionUiState

@Composable
fun PermissionScreen(
    uiState: PermissionUiState,
    onGrantAll: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "${uiState.grantedCount}/${uiState.totalCount} permissions granted",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                PermissionStatusRow(
                    title = "Runtime Permissions",
                    description = "Camera, Microphone, Location, Notifications",
                    isGranted = uiState.runtimeGranted
                )

                PermissionStatusRow(
                    title = "Device Admin",
                    description = "Required for remote lock/wipe",
                    isGranted = uiState.adminGranted
                )

                PermissionStatusRow(
                    title = "Battery Optimization",
                    description = "Disable for background service",
                    isGranted = uiState.batteryOptimized
                )

                PermissionStatusRow(
                    title = "Accessibility Service",
                    description = "For app tracking & auto-restart",
                    isGranted = uiState.accessibilityGranted
                )

                PermissionStatusRow(
                    title = "Overlay Permission",
                    description = "For background popups",
                    isGranted = uiState.overlayGranted
                )

                PermissionStatusRow(
                    title = "Alarms & Reminders",
                    description = "For scheduled tasks",
                    isGranted = uiState.exactAlarmGranted
                )
            }

            Column(modifier = Modifier.padding(24.dp)) {
                PrimaryButton(
                    text = "Grant All Permissions",
                    onClick = onGrantAll
                )
            }
        }
    }
}
