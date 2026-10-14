package com.smartexpense.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.auth.effect.AuthEffect
import com.smartexpense.auth.intent.AuthIntent
import com.smartexpense.auth.viewmodel.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToEmailConfirmation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthEffect.NavigateToHome -> onNavigateToHome()
                AuthEffect.NavigateToEmailConfirmation ->
                    onNavigateToEmailConfirmation()

                AuthEffect.NavigateToLogin -> Unit
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (state.isSignUp) "Create account" else "Welcome back",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (state.isSignUp) {
                "Create your SmartExpense account"
            } else {
                "Sign in to manage your expenses"
            },
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = {
                viewModel.onIntent(AuthIntent.EmailChanged(it))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = {
                viewModel.onIntent(AuthIntent.PasswordChanged(it))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            enabled = !state.isLoading,
            visualTransformation = if (state.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(
                            AuthIntent.TogglePasswordVisibility
                        )
                    },
                    enabled = !state.isLoading
                ) {
                    Text(if (state.isPasswordVisible) "Hide" else "Show")
                }
            }
        )

        if (state.isSignUp) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = {
                    viewModel.onIntent(
                        AuthIntent.ConfirmPasswordChanged(it)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm password") },
                singleLine = true,
                enabled = !state.isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                )
            )
        }

        state.errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        state.successMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.onIntent(
                    if (state.isSignUp) {
                        AuthIntent.SignUp
                    } else {
                        AuthIntent.SignIn
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Text(if (state.isSignUp) "Create account" else "Sign in")
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = {
                viewModel.onIntent(AuthIntent.ToggleAuthMode)
            },
            enabled = !state.isLoading
        ) {
            Text(
                if (state.isSignUp) {
                    "Already have an account? Sign in"
                } else {
                    "Don't have an account? Sign up"
                }
            )
        }
    }
}