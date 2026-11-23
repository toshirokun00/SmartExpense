package com.smartexpense.auth.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.auth.effect.AuthEffect
import com.smartexpense.auth.intent.AuthIntent
import com.smartexpense.auth.viewmodel.AuthViewModel
import com.smartexpense.designsystems.LivelyToast
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToEmailConfirmation: () -> Unit,
    modifier: Modifier = Modifier,
    logoRes: Int? = null
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

    // Errors and success messages are presented as a single lively toast.
    // Keying on the ViewModel's event id means an identical message from a
    // second button tap still produces a fresh toast.
    val toastEvent = state.errorMessage?.let {
        AuthToastEvent(state.messageEventId, it, isError = true)
    } ?: state.successMessage?.let {
        AuthToastEvent(state.messageEventId, it, isError = false)
    }
    var dismissedToastId by remember { mutableStateOf<Long?>(null) }
    val visibleToast = toastEvent?.takeIf { it.id != dismissedToastId }

    LaunchedEffect(toastEvent?.id) {
        toastEvent?.let {
            delay(ToastDurationMillis)
            dismissedToastId = it.id
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                                Color(0xFF2563EB),
                                Color(0xFF1D4ED8),
                                Color(0xFF60A5FA)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 40.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (logoRes != null) {
                        // The logo asset already carries its own rounded
                        // background, so no extra framed Surface is needed.
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = "Spendora logo",
                            modifier = Modifier.size(88.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Fallback for previews/tests that do not supply branding.
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Spendora",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedContent(
                        targetState = state.isSignUp,
                        transitionSpec = {
                            (
                                fadeIn(tween(durationMillis = 220)) +
                                    slideInVertically(tween(durationMillis = 260)) { it / 8 }
                                ) togetherWith (
                                fadeOut(tween(durationMillis = 160)) +
                                    slideOutVertically(tween(durationMillis = 200)) { -it / 8 }
                                )
                        },
                        label = "AuthSubtitle"
                    ) { isSignUp ->
                        Text(
                            text = if (isSignUp) {
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
            }

            // ── Mode toggle pill ───────────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))

            AuthModeToggle(
                isSignUp = state.isSignUp,
                enabled = !state.isLoading,
                onSignIn = {
                    if (state.isSignUp) {
                        viewModel.onIntent(AuthIntent.ToggleAuthMode)
                    }
                },
                onSignUp = {
                    if (!state.isSignUp) {
                        viewModel.onIntent(AuthIntent.ToggleAuthMode)
                    }
                }
            )

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
                        placeholder = "Enter your email",
                        icon = Icons.Default.Email,
                        singleLine = true,
                        enabled = !state.isLoading,
                        keyboardType = KeyboardType.Email
                    )

                    AuthTextField(
                        value = state.password,
                        onValueChange = { viewModel.onIntent(AuthIntent.PasswordChanged(it)) },
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
                        enter = fadeIn(animationSpec = tween(220)) +
                            expandVertically(
                                animationSpec = tween(260),
                                expandFrom = Alignment.Top
                            ),
                        exit = fadeOut(animationSpec = tween(150)) +
                            shrinkVertically(
                                animationSpec = tween(220),
                                shrinkTowards = Alignment.Top
                            ),
                        label = "ConfirmPasswordField"
                    ) {
                        AuthTextField(
                            value = state.confirmPassword,
                            onValueChange = {
                                viewModel.onIntent(AuthIntent.ConfirmPasswordChanged(it))
                            },
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

                    Button(
                        onClick = {
                            viewModel.onIntent(
                                if (state.isSignUp) AuthIntent.SignUp else AuthIntent.SignIn
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("authSubmitButton"),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !state.isLoading
                    ) {
                        AnimatedContent(
                            targetState = state.isSignUp,
                            transitionSpec = {
                                (
                                    fadeIn(tween(durationMillis = 200)) +
                                        slideInVertically(tween(durationMillis = 240)) { it / 3 }
                                    ) togetherWith (
                                    fadeOut(tween(durationMillis = 140)) +
                                        slideOutVertically(tween(durationMillis = 180)) { -it / 3 }
                                    )
                            },
                            label = "AuthSubmitLabel"
                        ) { isSignUp ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSignUp) {
                                        Icons.Default.AccountCircle
                                    } else {
                                        Icons.Default.Login
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSignUp) "Create account" else "Sign in",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
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
                        AnimatedContent(
                            targetState = state.isSignUp,
                            transitionSpec = {
                                fadeIn(tween(durationMillis = 200)) togetherWith
                                    fadeOut(tween(durationMillis = 140))
                            },
                            label = "AuthModePrompt"
                        ) { isSignUp ->
                            Text(
                                text = if (isSignUp) {
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
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Lively toast sits above the form; the loading dialog above it.
        AnimatedVisibility(
            visible = visibleToast != null,
            enter = fadeIn(tween(durationMillis = 180)) +
                slideInVertically(
                    spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { it / 4 } +
                scaleIn(
                    spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialScale = 0.92f
                ),
            exit = fadeOut(tween(durationMillis = 150)) +
                slideOutVertically(tween(durationMillis = 180)) { it / 4 } +
                scaleOut(tween(durationMillis = 180), targetScale = 0.96f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            label = "AuthToast"
        ) {
            visibleToast?.let { toast ->
                LivelyToast(
                    message = toast.message,
                    isError = toast.isError,
                    onDismiss = { dismissedToastId = toast.id }
                )
            }
        }

        AuthLoadingOverlay(
            isLoading = state.isLoading,
            isSignUp = state.isSignUp,
            logoRes = logoRes
        )
    }
}

private const val ToastDurationMillis = 3_400L

private data class AuthToastEvent(
    val id: Long,
    val message: String,
    val isError: Boolean
)

// ── Reusable components ─────────────────────────────────────────────────────────

/** Centered, dialog-style loader that replaces the old spinner in the button. */
@Composable
private fun AuthLoadingOverlay(
    isLoading: Boolean,
    isSignUp: Boolean,
    logoRes: Int?
) {
    val colorScheme = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(tween(durationMillis = 200)),
        exit = fadeOut(tween(durationMillis = 160)),
        label = "AuthLoadingOverlay"
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                // Consume taps while the modal loader is up.
                .clickable(enabled = true, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .widthIn(min = 260.dp, max = 340.dp)
                    .padding(24.dp)
            ) {
                val transition = rememberInfiniteTransition(label = "AuthLoading")
                val rotation by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        tween(durationMillis = 1_500, easing = LinearEasing)
                    ),
                    label = "LoaderRotation"
                )
                val pulse by transition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        tween(
                            durationMillis = 950,
                            easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "LoaderPulse"
                )
                val sweep by transition.animateFloat(
                    initialValue = -1f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(durationMillis = 1_250, easing = LinearEasing)
                    ),
                    label = "LoaderSweep"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { 0.72f },
                            modifier = Modifier
                                .size(96.dp)
                                .graphicsLayer { rotationZ = rotation },
                            color = colorScheme.primary,
                            trackColor = colorScheme.primaryContainer,
                            strokeWidth = 4.dp
                        )
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(62.dp)
                                .graphicsLayer {
                                    scaleX = pulse
                                    scaleY = pulse
                                }
                        ) {
                            if (logoRes != null) {
                                Image(
                                    painter = painterResource(id = logoRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AnimatedContent(
                        targetState = isSignUp,
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 200)) togetherWith
                                fadeOut(tween(durationMillis = 140))
                        },
                        label = "AuthLoaderLabel"
                    ) { signUp ->
                        Text(
                            text = if (signUp) {
                                "Creating your account"
                            } else {
                                "Signing you in"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Just a moment...",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colorScheme.primary.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    translationX = sweep * size.width * 2.5f
                                }
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            colorScheme.primary,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeToggle(
    isSignUp: Boolean,
    enabled: Boolean,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    // 0f = Sign in, 1f = Sign up. Animating this one value slides the active
    // background across the segmented control instead of cross-fading it.
    val indicatorPosition by animateFloatAsState(
        targetValue = if (isSignUp) 1f else 0f,
        animationSpec = tween(
            durationMillis = 320,
            easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
        ),
        label = "AuthModeIndicator"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(48.dp)
            .drawBehind {
                val gap = 8.dp.toPx()
                val itemWidth = (size.width - gap) / 2f
                val radius = CornerRadius(size.height / 2f)
                val trackColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)

                drawRoundRect(
                    color = trackColor,
                    size = Size(itemWidth, size.height),
                    cornerRadius = radius
                )
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(itemWidth + gap, 0f),
                    size = Size(itemWidth, size.height),
                    cornerRadius = radius
                )
                drawRoundRect(
                    color = colorScheme.primary,
                    topLeft = Offset(indicatorPosition * (itemWidth + gap), 0f),
                    size = Size(itemWidth, size.height),
                    cornerRadius = radius
                )
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AuthModeTab(
            label = "Sign in",
            icon = Icons.Default.Login,
            selected = !isSignUp,
            enabled = enabled,
            onClick = onSignIn,
            modifier = Modifier.weight(1f)
        )
        AuthModeTab(
            label = "Sign up",
            icon = Icons.Default.AccountCircle,
            selected = isSignUp,
            enabled = enabled,
            onClick = onSignUp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthModeTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            colorScheme.onPrimary
        } else {
            colorScheme.onSurfaceVariant
        },
        animationSpec = tween(250),
        label = "AuthModeTabContent"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AuthModeTabIconScale"
    )

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled && !selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
            tint = contentColor
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
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

    // The default leading-icon slot reserves a full 48 dp interactive box,
    // which leaves a large visual gap next to the text. The leading icon here
    // is decorative (not clickable), so opt out of the touch-target padding
    // locally and use explicit, tighter insets. The password toggle keeps its
    // 48 dp touch target below.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            ),
            placeholder = placeholder?.let { hint ->
                {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        )
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
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(20.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (onTogglePasswordVisibility != null) {
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides 48.dp
                    ) {
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
                                contentDescription = if (isPasswordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                },
                                modifier = Modifier.size(20.dp),
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
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
}
