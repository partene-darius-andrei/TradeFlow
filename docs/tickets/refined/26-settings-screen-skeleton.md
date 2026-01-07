# ⚙️ Settings Screen (Skeleton)

**Ticket:** 26
**Module:** `:app` (feature:settings in future)
**Priority:** MEDIUM
**Effort:** Medium
**Status:** Ready for Implementation
**Blocked by:** 21 (Theme), 22 (Base Components)
**Blocks:** 24 (Navigation)

---

## Objective

Create Settings screen UI skeleton with:
- Credentials section (view masked credentials, logout option)
- Trading parameters display (readonly for now)
- Notification preferences (toggles)
- About section (version, logs, privacy policy)

**Note:** This ticket focuses on UI ONLY. Actual settings persistence and logic come later.

---

## Context

Settings is the secondary screen accessed via bottom navigation. It allows users to:
1. **View/manage credentials** - See which API key is configured, logout option
2. **View trading parameters** - Max position size, drawdown limit (readonly for Phase 1)
3. **Toggle notifications** - Order filled, mode changed, emergency stop
4. **Access app info** - Version, logs, privacy policy

**Reference:** See wireframe in [20-ui-design-overview.md](20-ui-design-overview.md)

---

## Files to Create

```
app/src/main/java/com/dpart/tradeflow/
└── presentation/
    └── settings/
        ├── SettingsScreen.kt        # Main UI composable
        └── components/
            ├── CredentialsSection.kt  # API key display + logout
            ├── TradingSection.kt      # Trading parameters
            ├── NotificationsSection.kt # Notification toggles
            └── AboutSection.kt        # Version, logs, privacy
```

---

## Implementation

### 1. Settings Screen (SettingsScreen.kt)

```kotlin
package com.dpart.tradeflow.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dpart.tradeflow.presentation.settings.components.*
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.lg)
        ) {
            Spacer(modifier = Modifier.height(TradeFlowSpacing.sm))

            // Section headers with content
            SectionHeader(title = "Account")
            CredentialsSection(onLogout = onLogout)

            SectionHeader(title = "Trading")
            TradingSection()

            SectionHeader(title = "Notifications")
            NotificationsSection()

            SectionHeader(title = "About")
            AboutSection()

            Spacer(modifier = Modifier.height(TradeFlowSpacing.md))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    TradeFlowTheme {
        SettingsScreen()
    }
}
```

---

### 2. Credentials Section (CredentialsSection.kt)

```kotlin
package com.dpart.tradeflow.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
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
fun CredentialsSection(
    onLogout: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    StatusCard {
        // API Credentials row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO: Navigate to credentials detail */ }
                .padding(vertical = TradeFlowSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TradeFlowSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API Credentials",
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "API Credentials",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "organizations/abc-123...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider()

        // Logout row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLogoutDialog = true }
                .padding(vertical = TradeFlowSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(TradeFlowSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Logout",
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Logout",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout? You'll need to enter your credentials again to use the app.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CredentialsSectionPreview() {
    TradeFlowTheme {
        CredentialsSection()
    }
}
```

---

### 3. Trading Section (TradingSection.kt)

```kotlin
package com.dpart.tradeflow.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tradeflow.core.ui.component.StatusCard
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun TradingSection() {
    StatusCard {
        // Max Position Size
        SettingRow(
            label = "Max Position Size",
            value = "5% of portfolio",
            onClick = { /* TODO: Navigate to edit */ }
        )

        Divider()

        // Max Drawdown Limit
        SettingRow(
            label = "Max Drawdown Limit",
            value = "15%",
            onClick = { /* TODO: Navigate to edit */ }
        )

        Divider()

        // Emergency Stop
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TradeFlowSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Emergency Stop",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Auto-liquidate at drawdown limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = true,
                onCheckedChange = { /* TODO: Update setting */ },
                enabled = false // Readonly for Phase 1
            )
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = TradeFlowSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TradingSectionPreview() {
    TradeFlowTheme {
        TradingSection()
    }
}
```

---

### 4. Notifications Section (NotificationsSection.kt)

```kotlin
package com.dpart.tradeflow.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tradeflow.core.ui.component.StatusCard
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun NotificationsSection() {
    var orderFilled by remember { mutableStateOf(true) }
    var modeChanged by remember { mutableStateOf(true) }
    var emergencyStop by remember { mutableStateOf(true) }

    StatusCard {
        // Order Filled
        NotificationRow(
            label = "Order Filled",
            description = "When buy or sell orders complete",
            checked = orderFilled,
            onCheckedChange = { orderFilled = it }
        )

        Divider()

        // Mode Changed
        NotificationRow(
            label = "Mode Changed",
            description = "When trading mode switches",
            checked = modeChanged,
            onCheckedChange = { modeChanged = it }
        )

        Divider()

        // Emergency Stop
        NotificationRow(
            label = "Emergency Stop",
            description = "Critical: drawdown limit hit",
            checked = emergencyStop,
            onCheckedChange = { emergencyStop = it }
        )
    }
}

@Composable
private fun NotificationRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TradeFlowSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsSectionPreview() {
    TradeFlowTheme {
        NotificationsSection()
    }
}
```

---

### 5. About Section (AboutSection.kt)

```kotlin
package com.dpart.tradeflow.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tradeflow.core.ui.component.StatusCard
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun AboutSection() {
    StatusCard {
        // Version
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TradeFlowSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Version",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "1.0.0-alpha",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider()

        // Logs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO: Navigate to logs */ }
                .padding(vertical = TradeFlowSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Logs",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View logs",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider()

        // Privacy Policy
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO: Open privacy policy */ }
                .padding(vertical = TradeFlowSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View privacy policy",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutSectionPreview() {
    TradeFlowTheme {
        AboutSection()
    }
}
```

---

## Testing

### Manual Testing Checklist

1. **Layout:**
   - [ ] All sections render correctly
   - [ ] Scrolling works smoothly
   - [ ] Section headers visible
   - [ ] Top bar displays correctly

2. **Credentials Section:**
   - [ ] API key displays (masked)
   - [ ] Logout button shows
   - [ ] Logout dialog appears on click
   - [ ] Dialog has cancel and confirm options

3. **Trading Section:**
   - [ ] Max position size displays
   - [ ] Max drawdown displays
   - [ ] Emergency stop switch visible (disabled)
   - [ ] All values are readonly

4. **Notifications Section:**
   - [ ] All notification toggles work
   - [ ] Descriptions are clear
   - [ ] Switches have correct initial state

5. **About Section:**
   - [ ] Version number displays
   - [ ] Logs row clickable
   - [ ] Privacy policy row clickable

---

## Acceptance Criteria

- [ ] Settings screen renders without errors
- [ ] All sections display correctly
- [ ] Logout confirmation dialog works
- [ ] Notification toggles are interactive
- [ ] Trading parameters are readonly
- [ ] Screen is scrollable
- [ ] All previews render correctly
- [ ] Dark theme looks professional
- [ ] No hardcoded strings (use string resources)

---

## Notes

### Readonly Parameters

Trading parameters (max position size, drawdown limit) are **readonly** in Phase 1. These are hardcoded in the trading logic and shown here for transparency.

**Future enhancement:** Allow editing with validation:
- Max position size: 1-10% (slider)
- Max drawdown: 5-25% (slider)
- Changes require service restart

### Logout Behavior

Logout should:
1. **Clear credentials** from `CredentialStore`
2. **Stop trading service** if running
3. **Navigate to Login screen**
4. **Clear back stack** (can't go back)

Implementation in ViewModel (future ticket):
```kotlin
fun logout() {
    viewModelScope.launch {
        // Stop service if running
        tradingService.stop()

        // Clear credentials
        credentialStore.clearCredentials()

        // Navigate handled by UI callback
    }
}
```

### Future Enhancements

**When Settings ViewModel is added:**
- Load actual API key from CredentialStore (masked)
- Persist notification preferences to DataStore
- Show app version from BuildConfig
- Add "Check for updates" button
- Add "Clear cache" option

**Additional settings (Phase 2+):**
- Dark theme toggle (if light theme added)
- Trading pair selection (multi-asset support)
- Backtest configuration
- Export data (CSV, JSON)

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
- **Integrates with:** 23 (Login - logout returns here)
- **Future:** Settings ViewModel ticket (not yet created)
- **Reference:** 20 (UI Design Overview)
