# 📱 [SUPERSEDED] UI - Settings & Credentials Screen

Effort level: Medium
Priority: Low
Blocked by: Replaced by: ⚙️ FEATURE: Settings Screen (UI Only) + ⚙️ FEATURE: Settings ViewModel (Logic)

## Objective

Create UI for entering Coinbase API credentials.

## Files

- `ui/screens/SettingsScreen.kt`
- `ui/viewmodel/SettingsViewModel.kt`

## Fields

1. **API Key ID** (TextField)
    - Format: `organizations/{org}/apiKeys/{key}`
    - Validate format before saving
2. **Private Key PEM** (TextField, multiline)
    - Accept full PEM including headers
    - Or just the base64 content
3. **Product ID** (Dropdown)
    - BTC-USD (default)
    - ETH-USD

## Actions

- **Save Credentials** - Store via SecureKeyStore
- **Clear Credentials** - Wipe stored data
- **Test Connection** - Validate JWT works with API

## Validation

```kotlin
fun validateApiKeyId(id: String): Boolean {
    return id.startsWith("organizations/") && id.contains("/apiKeys/")
}

fun validatePrivateKey(pem: String): Boolean {
    return pem.contains("PRIVATE KEY") || 
           pem.matches("^[A-Za-z0-9+/=]+$".toRegex())
}
```

## Security Notes

- Never log credentials
- Clear clipboard after paste
- Show masked preview only

## Acceptance Criteria

- Can enter and save credentials
- Validation prevents invalid input
- Test connection confirms working setup
- Clear removes all stored data