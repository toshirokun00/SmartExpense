package com.smartexpense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.smartexpense.domain.model.AppTheme

// ---------------------------------------------------------------------------
// Color scheme built on the modern extended palette defined in Color.kt.
// This is the single source of truth for theme colors; screens must not
// redefine hardcoded palettes that duplicate these values.
// ---------------------------------------------------------------------------
private val DarkColorScheme = darkColorScheme(
    primary = ModernPrimary,
    onPrimary = ModernOnPrimary,
    primaryContainer = ModernPrimaryContainer,
    onPrimaryContainer = Color(0xFF312E81),

    secondary = ModernSecondary,
    onSecondary = ModernOnPrimary,
    secondaryContainer = ModernSecondaryContainer,

    tertiary = ModernTertiary,
    onTertiary = Color(0xFF000000),

    background = Color(0xFF141022),
    surface = Color(0xFF1F1A33),
    onSurface = SurfaceLight,
    onSurfaceVariant = Color(0xFFC7BED9),

    outline = Color(0xFF4A4166),

    error = Error,
    errorContainer = Color(0xFF4A1824)
)

private val LightColorScheme = lightColorScheme(
    primary = ModernPrimary,
    onPrimary = ModernOnPrimary,
    primaryContainer = ModernPrimaryContainer,
    onPrimaryContainer = Color(0xFF312E81),

    secondary = ModernSecondary,
    onSecondary = ModernOnPrimary,
    secondaryContainer = ModernSecondaryContainer,

    tertiary = ModernTertiary,
    onTertiary = Color(0xFF000000),

    background = SurfaceLight,
    surface = SurfaceCard,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,

    outline = OutlineLight,

    error = Error,
    errorContainer = ErrorContainer
)

@Composable
fun SmartExpenseTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
