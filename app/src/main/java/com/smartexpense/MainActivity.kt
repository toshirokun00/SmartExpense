package com.smartexpense

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.navigation.AppNavHost
import com.smartexpense.ui.theme.SmartExpenseTheme
import com.smartexpense.viewmodel.AppThemeViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val themeViewModel: AppThemeViewModel = koinViewModel()
            val appTheme by themeViewModel.theme.collectAsStateWithLifecycle()

            RequestNotificationPermission()

            SmartExpenseTheme(appTheme = appTheme) {
                AppNavHost()
            }
        }
    }
}

/**
 * Requests the POST_NOTIFICATIONS permission required on Android 13+ (API 33).
 *
 * Without this runtime request the permission stays denied and notification
 * delivery silently no-ops, so expense and budget alerts would never appear.
 */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Grant/deny is enforced by the notification manager on delivery. */ }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
