# 🎨 Core UI - Theme & Design System

**Ticket:** 21
**Module:** `:core:ui`
**Priority:** CRITICAL
**Effort:** Small
**Status:** Ready for Implementation
**Blocked by:** None
**Blocks:** 22, 23, 25, 26

---

## Objective

Create Material 3 theme and design system for TradeFlow with trading-focused dark theme, color scheme, typography, and spacing constants.

---

## Context

The core:ui module exists but is empty. We need a professional, trading-focused theme that:
- Reduces eye strain for 24/7 monitoring (dark by default)
- Uses color to communicate trading states (green=profit, red=loss)
- Provides consistent spacing and typography
- Supports Material 3 components

**Reference:** See [docs/tickets/refined/20-ui-design-overview.md](20-ui-design-overview.md) for complete design spec.

---

## Files to Create

```
core/ui/src/main/kotlin/com/tradeflow/core/ui/
├── theme/
│   ├── Color.kt           # Color definitions
│   ├── Typography.kt      # Typography scale
│   ├── Theme.kt           # TradeFlowTheme composable
│   └── Spacing.kt         # Spacing constants
```

---

## Implementation

### 1. Color Scheme (Color.kt)

```kotlin
package com.tradeflow.core.ui.theme

import androidx.compose.ui.graphics.Color

// Trading Colors
val ProfitGreen = Color(0xFF4CAF50)      // Bullish, gains, active
val LossRed = Color(0xFFEF5350)          // Bearish, losses, danger
val CautionOrange = Color(0xFFFF9800)    // Warning, pending
val NeutralBlue = Color(0xFF2196F3)      // Info, neutral states

// Dark Theme Colors
val BackgroundDark = Color(0xFF121212)   // Main background
val SurfaceDark = Color(0xFF1E1E1E)      // Card backgrounds
val SurfaceVariantDark = Color(0xFF2C2C2C) // Elevated cards

// Light Theme Colors (for future)
val BackgroundLight = Color(0xFFFAFAFA)
val SurfaceLight = Color(0xFFFFFFFF)

// Text Colors
val TextPrimary = Color(0xFFE0E0E0)      // Main text on dark
val TextSecondary = Color(0xFFB0B0B0)    // Secondary text
val TextTertiary = Color(0xFF808080)     // Disabled/hint text
```

### 2. Typography (Typography.kt)

```kotlin
package com.tradeflow.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TradeFlowTypography = Typography(
    // Display - Large numbers, hero prices
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),

    // Headline - Screen titles
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),

    // Title - Card titles, section headers
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // Body - General text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // Label - Buttons, tabs
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

### 3. Spacing System (Spacing.kt)

```kotlin
package com.tradeflow.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * TradeFlow spacing system
 * Use these instead of hardcoded dp values for consistency
 */
object TradeFlowSpacing {
    val xs = 4.dp     // Icon padding, tight spacing
    val sm = 8.dp     // List item internal spacing
    val md = 16.dp    // Default card padding, general spacing
    val lg = 24.dp    // Screen edge padding
    val xl = 32.dp    // Section spacing, large gaps
    val xxl = 48.dp   // Major section dividers
}

/**
 * Corner radius values
 */
object TradeFlowShapes {
    val sm = 4.dp     // Small buttons, chips
    val md = 8.dp     // Cards, inputs (default)
    val lg = 16.dp    // Large cards, dialogs
    val xl = 24.dp    // Bottom sheets
}
```

### 4. Theme Composable (Theme.kt)

```kotlin
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
```

---

## Testing

### Manual Testing Checklist

1. **Theme Application:**
   - [ ] TradeFlowTheme can be applied in MainActivity
   - [ ] Colors render correctly on preview
   - [ ] Typography scales are visible

2. **Color Verification:**
   - [ ] Green is readable on dark background
   - [ ] Red is readable on dark background
   - [ ] Orange is readable on dark background
   - [ ] Text colors have sufficient contrast

3. **Spacing Consistency:**
   - [ ] All spacing values are accessible
   - [ ] Values match design spec

### Preview Composables

Create preview file: `core/ui/src/main/kotlin/com/tradeflow/core/ui/theme/ThemePreview.kt`

```kotlin
package com.tradeflow.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun ThemePreview() {
    TradeFlowTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Display Large", style = MaterialTheme.typography.displayLarge)
            Text("Headline Large", style = MaterialTheme.typography.headlineLarge)
            Text("Title Large", style = MaterialTheme.typography.titleLarge)
            Text("Body Large", style = MaterialTheme.typography.bodyLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}
```

---

## Acceptance Criteria

- [ ] TradeFlowTheme composable can wrap any screen
- [ ] Dark theme is default and looks professional
- [ ] All Material 3 color slots are assigned
- [ ] Typography scale covers all use cases
- [ ] Spacing constants are defined and accessible
- [ ] Theme preview renders correctly
- [ ] No hardcoded colors/spacing in theme code
- [ ] Code compiles without warnings

---

## Notes

- **Dark theme rationale:** Traders monitor 24/7, dark reduces eye strain
- **Color psychology:** Green (bullish), Red (bearish) are universal trading standards
- **Material 3:** Using latest design system for future-proofing
- **No custom fonts:** Keeping default Roboto for simplicity and performance

---

## Dependencies

```kotlin
// Already in core:ui/build.gradle.kts
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.compose.ui.tooling.preview)
debugImplementation(libs.androidx.compose.ui.tooling)
```

---

## Related Tickets

- **Blocks:** 22 (Base Components), 23 (Login), 25 (Dashboard), 26 (Settings)
- **Reference:** 20 (UI Design Overview)
