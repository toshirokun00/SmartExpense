package com.portfolio.smartexpense

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.smartexpense.navigation.AuthRoute
import com.portfolio.smartexpense.navigation.DashboardRoute
import com.smartexpense.domain.usecase.AuthUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class StartupViewModel(
    private val authUseCase: AuthUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            Log.d("StartupViewModel", "Checking authentication session")

            try {
                val userId = withTimeoutOrNull(5_000L.milliseconds) {
                    authUseCase.getCurrentUserId()
                }

                val destination = if (userId != null) {
                    DashboardRoute
                } else {
                    AuthRoute
                }

                Log.d(
                    "StartupViewModel",
                    "Resolved destination: $destination"
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        startDestination = destination
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    "StartupViewModel",
                    "Session check failed",
                    exception
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        startDestination = AuthRoute
                    )
                }
            }
        }
    }
}