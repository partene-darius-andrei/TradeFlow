# ⚙️ FEATURE: Settings Screen (UI Only)

Effort level: Medium
Priority: Medium
Status: Not started
Blocked by: CORE-UI: Shared Components & Theme
Module: :feature:settings

## Objective

Create Settings UI for credential management and configuration.

## Module

`:feature:settings`

## Screen Layout

```
┌────────────────────────────────────┐
│  ← Settings                      │
├────────────────────────────────────┤
│  Exchange Credentials            │
│  ┌────────────────────────────────┐  │
│  │ API Key ID                    │  │
│  │ organizations/.../apiKeys/... │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │ Private Key (PEM)             │  │
│  │ -----BEGIN EC PRIVATE KEY--- │  │
│  │ ****************************  │  │
│  └────────────────────────────────┘  │
├────────────────────────────────────┤
│  Trading Pair                    │
│  ┌────────────────────────────────┐  │
│  │ BTC-USD                   ▼  │  │
│  └────────────────────────────────┘  │
├────────────────────────────────────┤
│  Connection Status: ✅ Connected  │
├────────────────────────────────────┤
│ [Save] [Test Connection] [Clear]│
└────────────────────────────────────┘
```

## UI State

```kotlin
data class SettingsUiState(
    val apiKeyId: String = "",
    val privateKey: String = "",
    val selectedProduct: String = "BTC-USD",
    val availableProducts: List<String> = listOf("BTC-USD", "ETH-USD"),
    val connectionStatus: ConnectionStatus = ConnectionStatus.UNKNOWN,
    val apiKeyError: String? = null,
    val privateKeyError: String? = null,
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val hasCredentials: Boolean = false
)

enum class ConnectionStatus { UNKNOWN, TESTING, CONNECTED, FAILED }
```

## Screen Composable

```kotlin
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onApiKeyChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onProductChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onClearCredentials: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

## Validation (UI level)

```kotlin
fun validateApiKeyFormat(key: String): Boolean =
    key.startsWith("organizations/") && key.contains("/apiKeys/")

fun validatePrivateKeyFormat(pem: String): Boolean =
    pem.contains("PRIVATE KEY") || pem.matches("^[A-Za-z0-9+/=\\n]+$".toRegex())
```

## File Structure

```
feature/settings/src/main/kotlin/com/tradeflow/feature/settings/
├── ui/
│   ├── SettingsScreen.kt
│   ├── CredentialsSection.kt
│   ├── ProductSelector.kt
│   └── ConnectionStatus.kt
└── model/
    └── SettingsUiState.kt
```

## Acceptance Criteria

- [ ]  Password field for private key (masked)
- [ ]  Input validation with error messages
- [ ]  All components have @Preview
- [ ]  No business logic in composables