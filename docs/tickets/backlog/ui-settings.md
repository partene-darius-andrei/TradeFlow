# 📱 UI - Settings Screen (Pure Compose)

Effort level: Small
Priority: Low

## Objective

Settings UI for API credentials and configuration. Pure Compose, no business logic.

## File

`presentation/ui/settings/SettingsScreen.kt`

## UI Components

### 1. Credentials Section

```kotlin
@Composable
fun CredentialsSection(
    apiKeyId: String,
    privateKey: String,
    isObscured: Boolean,
    onApiKeyChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onToggleObscure: () -> Unit
)
```

- API Key ID field (single line)
- Private Key field (multiline, obscured by default)
- Show/hide toggle

### 2. Exchange Selector (Future)

```kotlin
@Composable
fun ExchangeSelector(
    selected: Exchange,
    onSelect: (Exchange) -> Unit
)

enum class Exchange {
    COINBASE, KRAKEN, BINANCE
}
```

- Dropdown or radio buttons
- Currently only Coinbase enabled

### 3. Product Selector

```kotlin
@Composable
fun ProductSelector(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
)
```

- BTC-USD, ETH-USD options

### 4. Action Buttons

```kotlin
@Composable
fun SettingsActions(
    isSaving: Boolean,
    isTesting: Boolean,
    testResult: TestResult?,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onClear: () -> Unit
)

sealed class TestResult {
    object Success : TestResult()
    data class Error(val message: String) : TestResult()
}
```

- Save button
- Test Connection button with result indicator
- Clear Credentials button (with confirmation)

## Acceptance Criteria

- [ ]  All fields have proper input validation UI
- [ ]  Private key field obscured by default
- [ ]  Test result shown inline
- [ ]  Works with mock SettingsUiState