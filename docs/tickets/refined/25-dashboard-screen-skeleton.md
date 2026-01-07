# 📊 Dashboard Screen (Skeleton)

**Ticket:** 25
**Module:** `:app` (feature:dashboard in future)
**Priority:** HIGH
**Effort:** Medium
**Status:** Ready for Implementation
**Blocked by:** 21 (Theme), 22 (Base Components)
**Blocks:** 24 (Navigation)

---

## Objective

Create Dashboard screen UI skeleton with:
- Portfolio card (mock data)
- Trading mode indicator (mock data)
- Service control buttons (start/stop)
- Recent orders list (empty state)

**Note:** This ticket focuses on UI ONLY with mock data. ViewModels and real data integration come later.

---

## Context

Dashboard is the main screen showing:
1. **Portfolio summary** - Total value, balances
2. **Current mode** - DEFENSE/TREND/RANGE indicator
3. **Service status** - Trading service state + controls
4. **Recent orders** - Last 5 orders or empty state

**Reference:** See wireframe in [20-ui-design-overview.md](20-ui-design-overview.md)

---

## Files to Create

```
app/src/main/java/com/dpart/tradeflow/
└── presentation/
    └── dashboard/
        ├── DashboardScreen.kt       # Main UI composable
        └── components/
            ├── PortfolioCard.kt     # Portfolio display
            ├── ModeCard.kt          # Trading mode display
            ├── ServiceCard.kt       # Service controls
            └── OrdersList.kt        # Recent orders list
```

---

## Implementation

### 1. Dashboard Screen (DashboardScreen.kt)

```kotlin
package com.dpart.tradeflow.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dpart.tradeflow.presentation.dashboard.components.*
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun DashboardScreen() {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TradeFlow") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = TradeFlowSpacing.md)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(TradeFlowSpacing.sm))

            // Portfolio card
            PortfolioCard()

            // Trading mode card
            ModeCard()

            // Service control card
            ServiceCard()

            // Recent orders
            OrdersList()

            Spacer(modifier = Modifier.height(TradeFlowSpacing.md))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    TradeFlowTheme {
        DashboardScreen()
    }
}
```

---

### 2. Portfolio Card (PortfolioCard.kt)

```kotlin
package com.dpart.tradeflow.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.component.PriceDisplay
import com.tradeflow.core.ui.component.StatusCard
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme
import java.math.BigDecimal

@Composable
fun PortfolioCard() {
    StatusCard(title = "Portfolio") {
        // Total portfolio value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PriceDisplay(
                price = BigDecimal("10245.30"),
                previousPrice = BigDecimal("10000.00"),
                style = MaterialTheme.typography.displayMedium
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+$245.30",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "+2.45%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(TradeFlowSpacing.md))

        // Asset breakdown
        AssetRow(
            asset = "BTC",
            amount = "0.15420000",
            value = "$9,500.00"
        )
        AssetRow(
            asset = "USD",
            amount = "—",
            value = "$745.30"
        )
    }
}

@Composable
private fun AssetRow(
    asset: String,
    amount: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$asset: $amount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioCardPreview() {
    TradeFlowTheme {
        PortfolioCard()
    }
}
```

---

### 3. Mode Card (ModeCard.kt)

```kotlin
package com.dpart.tradeflow.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.component.ModeIndicator
import com.tradeflow.core.ui.component.PriceDisplay
import com.tradeflow.core.ui.component.StatusCard
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme
import java.math.BigDecimal

@Composable
fun ModeCard() {
    StatusCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Mode",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            ModeIndicator(mode = "TREND")
        }

        Spacer(modifier = Modifier.height(TradeFlowSpacing.md))

        // Current price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BTC-USD",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Above SMA(200)  •  ADX: 32",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                PriceDisplay(
                    price = BigDecimal("61582.00"),
                    previousPrice = BigDecimal("61000.00"),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "+1.2%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModeCardPreview() {
    TradeFlowTheme {
        ModeCard()
    }
}
```

---

### 4. Service Card (ServiceCard.kt)

```kotlin
package com.dpart.tradeflow.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.component.StatusCard
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun ServiceCard() {
    var isRunning by remember { mutableStateOf(false) }

    StatusCard(title = "Service Status") {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.md)
        ) {
            // Status indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isRunning) "Running" else "Paused",
                    tint = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = if (isRunning) "RUNNING" else "PAUSED",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            // Control button
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "Stop Trading Service" else "Start Trading Service")
            }

            // Info text
            if (isRunning) {
                Text(
                    text = "Service running • Last check: 2 min ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ServiceCardPreview() {
    TradeFlowTheme {
        ServiceCard()
    }
}
```

---

### 5. Orders List (OrdersList.kt)

```kotlin
package com.dpart.tradeflow.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tradeflow.core.ui.component.StatusCard
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun OrdersList() {
    StatusCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Orders",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = { /* TODO: Navigate to orders screen */ }) {
                Text("View All")
            }
        }

        Spacer(modifier = Modifier.height(TradeFlowSpacing.sm))

        // Empty state (replace with real orders later)
        EmptyOrdersState()
    }
}

@Composable
private fun EmptyOrdersState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TradeFlowSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm)
    ) {
        Text(
            text = "📊",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = "No orders yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Start the trading service to begin",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OrdersListPreview() {
    TradeFlowTheme {
        OrdersList()
    }
}
```

---

## Testing

### Manual Testing Checklist

1. **Layout:**
   - [ ] All cards render correctly
   - [ ] Scrolling works smoothly
   - [ ] Spacing is consistent
   - [ ] Top bar displays correctly

2. **Portfolio Card:**
   - [ ] Total value displays
   - [ ] Percentage change shows correct color
   - [ ] Asset rows show correctly

3. **Mode Card:**
   - [ ] Mode indicator displays correctly
   - [ ] Price and change display
   - [ ] Indicators (SMA, ADX) visible

4. **Service Card:**
   - [ ] Status displays correctly
   - [ ] Button toggles between start/stop
   - [ ] Colors change based on state

5. **Orders List:**
   - [ ] Empty state displays
   - [ ] "View All" button present

---

## Acceptance Criteria

- [ ] Dashboard screen renders without errors
- [ ] All cards display with mock data
- [ ] Service start/stop button toggles state
- [ ] Screen is scrollable
- [ ] All previews render correctly
- [ ] UI matches design wireframe
- [ ] Dark theme looks professional
- [ ] No hardcoded strings (use string resources)

---

## Notes

### Mock Data

This ticket uses hardcoded mock data for rapid UI development. When ViewModels are implemented:
- Portfolio data from `PortfolioRepository`
- Mode data from `DecisionEngine`
- Service status from `TradingService`
- Orders from `OrderRepository`

### Future Enhancements

**When ViewModel is added (later ticket):**
- Real-time portfolio updates
- Live price ticker
- Order status updates
- Pull-to-refresh
- Loading states
- Error handling

**When charts are added:**
- Price chart in Mode card
- Portfolio value chart

---

## Dependencies

```kotlin
// app/build.gradle.kts
implementation(project(":core:ui"))      // Theme & components
implementation(libs.androidx.compose.material3)
```

---

## Related Tickets

- **Blocked by:** 21 (Theme), 22 (Base Components)
- **Blocks:** 24 (Navigation)
- **Future:** Dashboard ViewModel ticket (not yet created)
- **Reference:** 20 (UI Design Overview)
