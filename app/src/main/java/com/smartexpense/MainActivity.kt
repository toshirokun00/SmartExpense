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
import androidx.lifecycle.lifecycleScope
import com.smartexpense.domain.usecase.ProcessRecurringExpensesUseCase
import com.smartexpense.navigation.AppNavHost
import com.smartexpense.ui.theme.SmartExpenseTheme
import com.smartexpense.viewmodel.AppThemeViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MainActivity : ComponentActivity(), KoinComponent {

    private val processRecurringExpenses: ProcessRecurringExpensesUseCase
        by lazy { get() }

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

    override fun onResume() {
        super.onResume()

        // Catch up due recurring occurrences whenever the app comes to the
        // foreground. The use case serialises calls and processing is
        // idempotent, so this is safe alongside the periodic worker.
        lifecycleScope.launch {
            try {
                processRecurringExpenses()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // The periodic worker retries; never crash the UI over a
                // background catch-up failure.
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
