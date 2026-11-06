package com.smartexpense.auth.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
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
    ) {
        // ── Gradient hero section ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFFA855F7),
                            Color(0xFFD946EF)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Spendora",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (state.isSignUp) {
                        "Create your account and start tracking"
                    } else {
                        "Sign in to manage your expenses"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Mode toggle pill ───────────────────────────────────────────────
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModePill(
                label = "Sign in",
                icon = Icons.Default.Login,
                selected = !state.isSignUp,
                onClick = {
                    if (state.isSignUp) {
                        viewModel.onIntent(AuthIntent.ToggleAuthMode)
                    }
                }
            )

            ModePill(
                label = "Sign up",
                icon = Icons.Default.AccountCircle,
                selected = state.isSignUp,
                onClick = {
                    if (!state.isSignUp) {
                        viewModel.onIntent(AuthIntent.ToggleAuthMode)
                    }
                }
            )
        }

        // ── Form card ──────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AuthTextField(
                    value = state.email,
                    onValueChange = { viewModel.onIntent(AuthIntent.EmailChanged(it)) },
                    label = "Email address",
                    placeholder = "Enter your email",
                    icon = Icons.Default.Email,
                    singleLine = true,
                    enabled = !state.isLoading,
                    keyboardType = KeyboardType.Email
                )

                AuthTextField(
                    value = state.password,
                    onValueChange = { viewModel.onIntent(AuthIntent.PasswordChanged(it)) },
                    label = "Password",
                    placeholder = "Enter your password",
                    supportingText = if (state.isSignUp) {
                        "Use at least 8 characters"
                    } else {
                        null
                    },
                    icon = Icons.Default.Lock,
                    singleLine = true,
                    enabled = !state.isLoading,
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    isPasswordVisible = state.isPasswordVisible,
                    onTogglePasswordVisibility = {
                        viewModel.onIntent(AuthIntent.TogglePasswordVisibility)
                    }
                )

                AnimatedVisibility(
                    visible = state.isSignUp,
                    enter = fadeIn(animationSpec = tween(250)),
                    exit = fadeOut(animationSpec = tween(250)),
                    label = "ConfirmPasswordField"
                ) {
                    AuthTextField(
                        value = state.confirmPassword,
                        onValueChange = {
                            viewModel.onIntent(AuthIntent.ConfirmPasswordChanged(it))
                        },
                        label = "Confirm password",
                        placeholder = "Re-enter your password",
                        icon = Icons.Default.Lock,
                        singleLine = true,
                        enabled = !state.isLoading,
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        isPasswordVisible = state.isPasswordVisible,
                        onTogglePasswordVisibility = {
                            viewModel.onIntent(AuthIntent.TogglePasswordVisibility)
                        }
                    )
                }

                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200)),
                    label = "ErrorMessage"
                ) {
                    state.errorMessage?.let { message ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.successMessage != null,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200)),
                    label = "SuccessMessage"
                ) {
                    state.successMessage?.let { message ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.onIntent(
                            if (state.isSignUp) AuthIntent.SignUp else AuthIntent.SignIn
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Please wait...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (state.isSignUp) {
                                    Icons.Default.AccountCircle
                                } else {
                                    Icons.Default.Login
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isSignUp) "Create account" else "Sign in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                TextButton(
                    onClick = {
                        viewModel.onIntent(AuthIntent.ToggleAuthMode)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    Text(
                        text = if (state.isSignUp) {
                            "Already have an account?  Sign in"
                        } else {
                            "Don't have an account?  Sign up"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ── Reusable components ─────────────────────────────────────────────────────────

@Composable
private fun RowScope.ModePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            colorScheme.primary
        } else {
            colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(250),
        label = "ModePillBg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            colorScheme.onPrimary
        } else {
            colorScheme.onSurfaceVariant
        },
        animationSpec = tween(250),
        label = "ModePillContent"
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.95f,
        animationSpec = tween(250),
        label = "ModePillScale"
    )

    Surface(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = !selected, onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    singleLine: Boolean,
    enabled: Boolean,
    keyboardType: KeyboardType,
    placeholder: String? = null,
    supportingText: String? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        placeholder = placeholder?.let { hint ->
            {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        supportingText = supportingText?.let { hint ->
            {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        visualTransformation = when {
            isPassword && !isPasswordVisible -> PasswordVisualTransformation()
            else -> VisualTransformation.None
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (onTogglePasswordVisibility != null) {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = if (isPasswordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.outlineVariant,
            focusedLabelColor = colorScheme.primary
        )
    )
}
