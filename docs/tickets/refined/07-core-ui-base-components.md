# 🧩 Core UI - Base Components

**Ticket:** 22
**Module:** `:core:ui`
**Priority:** HIGH
**Effort:** Medium
**Status:** Ready for Implementation
**Blocked by:** 21 (Theme)
**Blocks:** 23, 25, 26

---

## Objective

Create reusable base UI components for TradeFlow app:
- StatusCard (container)
- LoadingButton (button with loading state)
- PriceDisplay (price with color coding)
- ModeIndicator (visual mode display)
- ErrorDisplay (error states)

---

## Context

After establishing the theme (Ticket 06), we need reusable components that can be used across all feature screens. These components should:
- Follow Material 3 design patterns
- Use TradeFlowTheme colors and typography
- Be stateless and reusable
- Include preview annotations

---

## Files to Create

```
core/ui/src/main/kotlin/com/tradeflow/core/ui/
├── component/
│   ├── StatusCard.kt        # Reusable card container
│   ├── LoadingButton.kt     # Button with loading spinner
│   ├── PriceDisplay.kt      # Price with +/- color coding
│   ├── ModeIndicator.kt     # Trading mode visual indicator
│   └── ErrorDisplay.kt      # Error state display
└── extension/
    └── BigDecimalExt.kt     # Formatting extensions
```

---

## Implementation

### 1. StatusCard (StatusCard.kt)

**Purpose:** Reusable card container for dashboard and settings.

```kotlin
package com.tradeflow.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

/**
 * Reusable card component for displaying status information
 *
 * @param title Card title (optional)
 * @param modifier Modifier
 * @param content Card content
 */
@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(TradeFlowSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusCardPreview() {
    TradeFlowTheme {
        StatusCard(
            title = "Portfolio",
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Content goes here")
        }
    }
}
```

---

### 2. LoadingButton (LoadingButton.kt)

**Purpose:** Button that shows loading spinner when action is in progress.

```kotlin
package com.tradeflow.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Button with loading state
 *
 * @param text Button text
 * @param onClick Click handler
 * @param modifier Modifier
 * @param loading Whether button is in loading state
 * @param enabled Whether button is enabled (ignored when loading)
 */
@Composable
fun LoadingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading
    ) {
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(text)
            }
        } else {
            Text(text)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingButtonPreview() {
    TradeFlowTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LoadingButton(text = "Normal", onClick = {})
            LoadingButton(text = "Loading", onClick = {}, loading = true)
            LoadingButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
```

---

### 3. PriceDisplay (PriceDisplay.kt)

**Purpose:** Display price with color coding based on change (green=up, red=down).

```kotlin
package com.tradeflow.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import com.tradeflow.core.ui.theme.TradeFlowTheme

/**
 * Display price with optional change indicator
 *
 * @param price Current price
 * @param previousPrice Previous price for comparison (optional)
 * @param modifier Modifier
 * @param style Text style (defaults to headlineLarge)
 */
@Composable
fun PriceDisplay(
    price: BigDecimal,
    modifier: Modifier = Modifier,
    previousPrice: BigDecimal? = null,
    style: TextStyle = MaterialTheme.typography.headlineLarge
) {
    val color = when {
        previousPrice == null -> MaterialTheme.colorScheme.onSurface
        price > previousPrice -> MaterialTheme.colorScheme.primary  // Green (profit)
        price < previousPrice -> MaterialTheme.colorScheme.secondary // Red (loss)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val changeSymbol = when {
        previousPrice == null -> ""
        price > previousPrice -> "↗"
        price < previousPrice -> "↘"
        else -> ""
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$${String.format("%,.2f", price)}",
            style = style,
            color = color
        )
        if (changeSymbol.isNotEmpty()) {
            Text(
                text = changeSymbol,
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PriceDisplayPreview() {
    TradeFlowTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PriceDisplay(
                price = BigDecimal("61500.00"),
                previousPrice = BigDecimal("61000.00")
            )
            PriceDisplay(
                price = BigDecimal("61500.00"),
                previousPrice = BigDecimal("62000.00")
            )
            PriceDisplay(price = BigDecimal("61500.00"))
        }
    }
}
```

---

### 4. ModeIndicator (ModeIndicator.kt)

**Purpose:** Visual indicator for current trading mode (DEFENSE/TREND/RANGE).

```kotlin
package com.tradeflow.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

/**
 * Display trading mode with color and icon
 *
 * Modes:
 * - DEFENSE: Red shield (price below SMA, preserve capital)
 * - TREND: Green trending up (strong trend, use bracket orders)
 * - RANGE: Blue swap (weak trend, use grid)
 * - WAIT: Gray hourglass (transitioning between modes)
 *
 * @param mode Mode string (DEFENSE/TREND/RANGE/WAIT)
 * @param modifier Modifier
 */
@Composable
fun ModeIndicator(
    mode: String,
    modifier: Modifier = Modifier
) {
    val (color, icon, description) = when (mode.uppercase()) {
        "DEFENSE" -> Triple(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.Shield,
            "Defense Mode"
        )
        "TREND" -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.TrendingUp,
            "Trend Mode"
        )
        "RANGE" -> Triple(
            Color(0xFF2196F3), // Blue
            Icons.Default.SwapVert,
            "Range Mode"
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline,
            Icons.Default.HourglassEmpty,
            "Wait"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = TradeFlowSpacing.md, vertical = TradeFlowSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = mode.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModeIndicatorPreview() {
    TradeFlowTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeIndicator(mode = "DEFENSE")
            ModeIndicator(mode = "TREND")
            ModeIndicator(mode = "RANGE")
            ModeIndicator(mode = "WAIT")
        }
    }
}
```

---

### 5. ErrorDisplay (ErrorDisplay.kt)

**Purpose:** Consistent error state display with optional retry.

```kotlin
package com.tradeflow.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

/**
 * Display error state with optional retry button
 *
 * @param message Error message
 * @param modifier Modifier
 * @param onRetry Retry callback (if null, button is hidden)
 */
@Composable
fun ErrorDisplay(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(TradeFlowSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.md)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (onRetry != null) {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorDisplayPreview() {
    TradeFlowTheme {
        ErrorDisplay(
            message = "Failed to load portfolio data. Check your connection and try again.",
            onRetry = {}
        )
    }
}
```

---

### 6. BigDecimal Extensions (BigDecimalExt.kt)

**Purpose:** Formatting helpers for BigDecimal (currency, percentages).

```kotlin
package com.tradeflow.core.ui.extension

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Format BigDecimal as currency with $ and 2 decimal places
 * Example: BigDecimal("1234.567").toCurrencyString() -> "$1,234.57"
 */
fun BigDecimal.toCurrencyString(): String {
    return "$${String.format("%,.2f", this.setScale(2, RoundingMode.HALF_UP))}"
}

/**
 * Format BigDecimal as percentage with + or - sign
 * Example: BigDecimal("0.025").toPercentageString() -> "+2.50%"
 */
fun BigDecimal.toPercentageString(): String {
    val percent = this.multiply(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
    val sign = if (percent >= BigDecimal.ZERO) "+" else ""
    return "$sign$percent%"
}

/**
 * Format BigDecimal as crypto amount (8 decimal places, no trailing zeros)
 * Example: BigDecimal("0.12345678").toCryptoString() -> "0.12345678"
 */
fun BigDecimal.toCryptoString(): String {
    return this.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
```

---

## Testing

### Manual Testing Checklist

1. **StatusCard:**
   - [ ] Renders with title
   - [ ] Renders without title
   - [ ] Content is properly padded
   - [ ] Card elevation is visible

2. **LoadingButton:**
   - [ ] Normal state clickable
   - [ ] Loading state shows spinner
   - [ ] Disabled state not clickable
   - [ ] Loading state not clickable

3. **PriceDisplay:**
   - [ ] Shows green when price increases
   - [ ] Shows red when price decreases
   - [ ] Shows neutral when no previous price
   - [ ] Arrow indicator appears correctly

4. **ModeIndicator:**
   - [ ] DEFENSE shows red shield
   - [ ] TREND shows green trending up
   - [ ] RANGE shows blue swap
   - [ ] WAIT shows gray hourglass

5. **ErrorDisplay:**
   - [ ] Message displays clearly
   - [ ] Retry button appears when callback provided
   - [ ] Retry button hidden when no callback

---

## Acceptance Criteria

- [ ] All components compile without errors
- [ ] All components have @Preview annotations
- [ ] Previews render correctly in Android Studio
- [ ] Components use TradeFlowTheme colors
- [ ] Components use TradeFlowSpacing for padding
- [ ] No hardcoded colors or spacing values
- [ ] All components are stateless (take parameters)
- [ ] BigDecimal extensions work correctly

---

## Notes

- **Stateless design:** All components take data as parameters, no internal state
- **Reusability:** These will be used in Dashboard, Settings, and Login screens
- **Preview-driven:** Use @Preview to iterate on design without running app
- **Material 3:** Using latest Material components for consistency

---

## Dependencies

```kotlin
// Already in core:ui/build.gradle.kts
implementation(project(":core:domain"))  // For domain types if needed
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.compose.ui.tooling.preview)
```

---

## Related Tickets

- **Blocked by:** 21 (Theme must exist first)
- **Blocks:** 23 (Login), 25 (Dashboard), 26 (Settings)
- **Reference:** 20 (UI Design Overview)
