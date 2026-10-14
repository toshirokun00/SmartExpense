package com.smartexpense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
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
            SmartExpenseTheme(appTheme = appTheme) {
                AppNavHost()
            }
        }
    }
}
