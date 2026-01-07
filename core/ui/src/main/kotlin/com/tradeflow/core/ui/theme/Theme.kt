package com.tradeflow.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Dark color scheme (default for TradeFlow)
 */
private val DarkColorScheme = darkColorScheme(
    primary = ProfitGreen,           // Primary actions, active states
    onPrimary = Color.Black,         // Text on primary
    primaryContainer = Color(0xFF1B5E20), // Primary variant
    onPrimaryContainer = Color(0xFFA5D6A7),

    secondary = LossRed,             // Secondary actions, losses
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF8B0000), // Secondary variant
    onSecondaryContainer = Color(0xFFFFCDD2),

    tertiary = CautionOrange,        // Warnings, pending
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFE65100),
    onTertiaryContainer = Color(0xFFFFE0B2),

    error = LossRed,                 // Errors
    onError = Color.White,
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = Color(0xFFFFCDD2),

    background = BackgroundDark,     // Screen background
    onBackground = TextPrimary,      // Text on background

    surface = SurfaceDark,           // Card surfaces
    onSurface = TextPrimary,         // Text on cards
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,

    outline = Color(0xFF404040),     // Borders, dividers
    outlineVariant = Color(0xFF2C2C2C),

    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFF1B5E20),
)

/**
 * Light color scheme (for future use)
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),     // Darker green for light theme
    onPrimary = Color.White,
    secondary = Color(0xFFD32F2F),   // Darker red
    onSecondary = Color.White,
    tertiary = Color(0xFFF57C00),    // Darker orange
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF1C1C1C),
    surface = SurfaceLight,
    onSurface = Color(0xFF1C1C1C),
)

/**
 * Main TradeFlow theme
 *
 * Defaults to dark theme for trading use case.
 * Supports Material 3 dynamic colors on Android 12+.
 */
@Composable
fun TradeFlowTheme(
    darkTheme: Boolean = true, // Default to dark for trading
    dynamicColor: Boolean = false, // Disable dynamic colors by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TradeFlowTypography,
        content = content
    )
}

/**
 * Access spacing in composables
 * Usage: val spacing = LocalSpacing.current
 */
val LocalSpacing = staticCompositionLocalOf { TradeFlowSpacing }
