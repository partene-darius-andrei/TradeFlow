# 🎨 CORE-UI: Shared Components & Theme

Effort level: Medium
Priority: Medium
Blocked by: MODULE: Project Modularization Setup
Module: :core:ui

## Objective

Create shared UI components and theme for feature modules.

## Module

`:core:ui`

## Theme

```kotlin
@Composable
fun TradeFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TradeFlowTypography,
        content = content
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),      // Green for profits
    secondary = Color(0xFFF44336),    // Red for losses
    tertiary = Color(0xFFFF9800),     // Orange for warnings
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)
```

## Shared Components

### PriceDisplay

```kotlin
@Composable
fun PriceDisplay(
    price: BigDecimal,
    previousPrice: BigDecimal? = null,
    modifier: Modifier = Modifier
) {
    val color = when {
        previousPrice == null -> MaterialTheme.colorScheme.onSurface
        price > previousPrice -> MaterialTheme.colorScheme.primary  // Green
        price < previousPrice -> MaterialTheme.colorScheme.secondary // Red
        else -> MaterialTheme.colorScheme.onSurface
    }
    // ...
}
```

### ModeIndicator

```kotlin
@Composable
fun ModeIndicator(
    mode: String,  // WAIT, DEFENSE, TREND, RANGE
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (mode) {
        "DEFENSE" -> [Color.Red](http://Color.Red) to Icons.Default.Shield
        "TREND" -> [Color.Green](http://Color.Green) to Icons.Default.TrendingUp
        "RANGE" -> [Color.Blue](http://Color.Blue) to Icons.Default.SwapVert
        else -> Color.Gray to Icons.Default.HourglassEmpty
    }
    // ...
}
```

### PortfolioCard

```kotlin
@Composable
fun PortfolioCard(
    portfolio: PortfolioUiState,
    modifier: Modifier = Modifier
)
```

### OrderItem

```kotlin
@Composable
fun OrderItem(
    order: OrderUiState,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

### LoadingButton

```kotlin
@Composable
fun LoadingButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
```

## File Structure

```
core/ui/src/main/kotlin/com/tradeflow/core/ui/
├── theme/
│   ├── Theme.kt
│   ├── Color.kt
│   └── Typography.kt
├── component/
│   ├── PriceDisplay.kt
│   ├── ModeIndicator.kt
│   ├── PortfolioCard.kt
│   ├── OrderItem.kt
│   └── LoadingButton.kt
└── extension/
    └── BigDecimalFormat.kt
```

## Acceptance Criteria

- [ ]  Theme supports dark/light mode
- [ ]  All components use Material 3
- [ ]  Reusable across feature modules
- [ ]  Preview annotations for all components