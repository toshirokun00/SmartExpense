package com.smartexpense.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Existing brand palette (kept for backward compatibility)
// ---------------------------------------------------------------------------
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ---------------------------------------------------------------------------
// Modern extended palette
// ---------------------------------------------------------------------------
// Primary brand (indigo-violet)
val ModernPrimary = Color(0xFF6366F1)
val ModernPrimaryContainer = Color(0xFFE0E7FF)
val ModernOnPrimary = Color(0xFFFFFFFF)

// Secondary accent (electric blue)
val ModernSecondary = Color(0xFF3B82F6)
val ModernSecondaryContainer = Color(0xFFDBEAFE)

// Tertiary accent (warm orange)
val ModernTertiary = Color(0xFFFB923C)
val ModernTertiaryContainer = Color(0xFFFED7AA)

// Surfaces
val SurfaceLight = Color(0xFFFAF9FE)     // near-white tinted background
val SurfaceCard = Color(0xFFFFFFFF)
val SurfaceElevated = Color(0xFFF3F0FB)  // subtle lifted surface
val OnSurface = Color(0xFF1F1A33)        // deep ink for headings
val OnSurfaceVariant = Color(0xFF6B6280) // muted body text
val OutlineLight = Color(0xFFE9E4F2)

// ---------------------------------------------------------------------------
// Dark theme palette
//
// Layered near-black surfaces with a violet tint, paired with brighter brand
// tones so text, icons and status colours stay legible in dark mode.
// ---------------------------------------------------------------------------
val DarkPrimary = Color(0xFFA5B4FC)
val DarkOnPrimary = Color(0xFF1E1B4B)
val DarkPrimaryContainer = Color(0xFF3730A3)
val DarkOnPrimaryContainer = Color(0xFFE0E7FF)

val DarkSecondary = Color(0xFF93C5FD)
val DarkOnSecondary = Color(0xFF0B2545)
val DarkSecondaryContainer = Color(0xFF1E3A8A)
val DarkOnSecondaryContainer = Color(0xFFDBEAFE)

val DarkTertiary = Color(0xFFFDBA74)
val DarkOnTertiary = Color(0xFF3B1D00)
val DarkTertiaryContainer = Color(0xFF7C2D12)
val DarkOnTertiaryContainer = Color(0xFFFFEDD5)

val DarkBackground = Color(0xFF0F0D18)
val DarkSurface = Color(0xFF171426)
val DarkSurfaceVariant = Color(0xFF2E2844)

val DarkSurfaceContainerLowest = Color(0xFF0B0912)
val DarkSurfaceContainerLow = Color(0xFF141020)
val DarkSurfaceContainer = Color(0xFF1A1629)
val DarkSurfaceContainerHigh = Color(0xFF211C33)
val DarkSurfaceContainerHighest = Color(0xFF29233D)

val DarkOnSurface = Color(0xFFF2EEFA)
val DarkOnSurfaceVariant = Color(0xFFBDB4D4)
val DarkOutline = Color(0xFF6B6288)
val DarkOutlineVariant = Color(0xFF3A3354)

val DarkInverseSurface = Color(0xFFF2EEFA)
val DarkInverseOnSurface = Color(0xFF1F1A33)
val DarkInversePrimary = Color(0xFF4F46E5)

val DarkError = Color(0xFFFDA4AF)
val DarkOnError = Color(0xFF4C0519)
val DarkErrorContainer = Color(0xFF881337)
val DarkOnErrorContainer = Color(0xFFFFE4E6)

// Semantic / status colors
val Success = Color(0xFF10B981)
val SuccessContainer = Color(0xFFECFDF5)
val Warning = Color(0xFFFB923C)
val WarningContainer = Color(0xFFFFF7ED)
val Error = Color(0xFFF43F5E)
val ErrorContainer = Color(0xFFFEEDFA)

// Gradient stops used for the hero balance card
val GradientStart = Color(0xFF6366F1) // indigo
val GradientMid = Color(0xFFA855F7)   // purple
val GradientEnd = Color(0xFFFB923C)   // orange
