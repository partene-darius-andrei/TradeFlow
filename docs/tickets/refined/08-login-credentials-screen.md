# 🔐 Login/Credentials Screen

**Ticket:** 23
**Module:** `:app` (feature:auth in future)
**Priority:** CRITICAL
**Effort:** Medium
**Status:** Ready for Implementation
**Blocked by:** 21 (Theme), 22 (Base Components)
**Blocks:** 24 (Navigation)

---

## Objective

Create login/credentials entry screen where users enter Coinbase API credentials. Validate, test connection, and save securely using `SecureCredentialStore`.

---

## Context

This is the FIRST screen users see if they haven't entered credentials. Without this, the app cannot authenticate with Coinbase API.

**Requirements:**
- Simple, focused UI (no distractions)
- Input fields for API Key and Secret
- Optional name field (for user reference)
- Test connection button (validate credentials work)
- Save to `SecureCredentialStore` (already implemented in Ticket 04)
- Clear error messages
- Security assurance message

**Reference:** See wireframe in [05-ui-design-overview.md](05-ui-design-overview.md)

---

## Files to Create

```
app/src/main/java/com/dpart/tradeflow/
├── presentation/
│   └── login/
│       ├── LoginScreen.kt       # UI composable
│       ├── LoginViewModel.kt    # Business logic
│       └── LoginUiState.kt      # UI state sealed class
```

---

## Implementation

### 1. UI State (LoginUiState.kt)

```kotlin
package com.dpart.tradeflow.presentation.login

/**
 * UI state for login screen
 */
sealed interface LoginUiState {
    data object Initial : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
    data object Success : LoginUiState
}

/**
 * Form state for credential inputs
 */
data class CredentialFormState(
    val name: String = "",
    val apiKey: String = "",
    val apiSecret: String = "",
    val apiSecretVisible: Boolean = false,
    val isValid: Boolean = false
)
```

---

### 2. ViewModel (LoginViewModel.kt)

```kotlin
package com.dpart.tradeflow.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.domain.auth.CredentialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(CredentialFormState())
    val formState: StateFlow<CredentialFormState> = _formState.asStateFlow()

    /**
     * Update form fields
     */
    fun updateName(name: String) {
        _formState.value = _formState.value.copy(name = name)
        validateForm()
    }

    fun updateApiKey(apiKey: String) {
        _formState.value = _formState.value.copy(apiKey = apiKey.trim())
        validateForm()
    }

    fun updateApiSecret(apiSecret: String) {
        _formState.value = _formState.value.copy(apiSecret = apiSecret.trim())
        validateForm()
    }

    fun toggleSecretVisibility() {
        _formState.value = _formState.value.copy(
            apiSecretVisible = !_formState.value.apiSecretVisible
        )
    }

    /**
     * Validate form inputs
     */
    private fun validateForm() {
        val form = _formState.value
        val isValid = form.apiKey.isNotBlank() &&
                form.apiSecret.isNotBlank() &&
                form.apiKey.startsWith("organizations/") // Basic Coinbase format check
        _formState.value = form.copy(isValid = isValid)
    }

    /**
     * Test connection to Coinbase API
     * TODO: Implement when CoinbaseRepository is ready
     */
    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                // TODO: Call CoinbaseRepository.getAccounts() with credentials
                // For now, just validate format
                if (_formState.value.isValid) {
                    _uiState.value = LoginUiState.Initial
                    Timber.i("Credentials validated (test connection not yet implemented)")
                } else {
                    _uiState.value = LoginUiState.Error("Invalid credentials format")
                }
            } catch (e: Exception) {
                Timber.e(e, "Connection test failed")
                _uiState.value = LoginUiState.Error(e.message ?: "Connection failed")
            }
        }
    }

    /**
     * Save credentials and navigate to dashboard
     */
    fun saveCredentials() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val form = _formState.value

                // Save to secure storage
                credentialStore.saveCredentials(
                    name = form.name.ifBlank { "My Trading Bot" },
                    apiKey = form.apiKey,
                    apiSecret = form.apiSecret
                )

                Timber.i("Credentials saved successfully")
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                Timber.e(e, "Failed to save credentials")
                _uiState.value = LoginUiState.Error(e.message ?: "Failed to save credentials")
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Initial
        }
    }
}
```

---

### 3. UI Screen (LoginScreen.kt)

```kotlin
package com.dpart.tradeflow.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradeflow.core.ui.component.LoadingButton
import com.tradeflow.core.ui.theme.TradeFlowSpacing
import com.tradeflow.core.ui.theme.TradeFlowTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    LoginScreenContent(
        uiState = uiState,
        formState = formState,
        onNameChange = viewModel::updateName,
        onApiKeyChange = viewModel::updateApiKey,
        onApiSecretChange = viewModel::updateApiSecret,
        onToggleSecretVisibility = viewModel::toggleSecretVisibility,
        onTestConnection = viewModel::testConnection,
        onSaveCredentials = viewModel::saveCredentials,
        onDismissError = viewModel::clearError
    )
}

@Composable
private fun LoginScreenContent(
    uiState: LoginUiState,
    formState: CredentialFormState,
    onNameChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApiSecretChange: (String) -> Unit,
    onToggleSecretVisibility: () -> Unit,
    onTestConnection: () -> Unit,
    onSaveCredentials: () -> Unit,
    onDismissError: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isLoading = uiState is LoginUiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login to Coinbase") },
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
                .padding(TradeFlowSpacing.lg)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.md)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(TradeFlowSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm)
                ) {
                    Text(
                        text = "🔑 TradeFlow",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Enter your Coinbase Advanced Trade API credentials",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Name field (optional)
            OutlinedTextField(
                value = formState.name,
                onValueChange = onNameChange,
                label = { Text("API Key Name (optional)") },
                placeholder = { Text("My Trading Bot") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // API Key field
            OutlinedTextField(
                value = formState.apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key *") },
                placeholder = { Text("organizations/abc-123/...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = {
                    Text("Must start with 'organizations/'")
                }
            )

            // API Secret field
            OutlinedTextField(
                value = formState.apiSecret,
                onValueChange = onApiSecretChange,
                label = { Text("API Secret *") },
                placeholder = { Text("-----BEGIN EC PRIVATE KEY-----") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                visualTransformation = if (formState.apiSecretVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = onToggleSecretVisibility) {
                        Icon(
                            imageVector = if (formState.apiSecretVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (formState.apiSecretVisible) {
                                "Hide secret"
                            } else {
                                "Show secret"
                            }
                        )
                    }
                },
                minLines = 3,
                maxLines = 5
            )

            // Error display
            if (uiState is LoginUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(TradeFlowSpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissError) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(TradeFlowSpacing.md))

            // Test connection button
            OutlinedButton(
                onClick = onTestConnection,
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.isValid && !isLoading
            ) {
                Text("Test Connection")
            }

            // Save button
            LoadingButton(
                text = "Save & Continue",
                onClick = onSaveCredentials,
                modifier = Modifier.fillMaxWidth(),
                loading = isLoading,
                enabled = formState.isValid
            )

            // Security note
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(TradeFlowSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(TradeFlowSpacing.sm)
                ) {
                    Text(
                        text = "ⓘ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Credentials are encrypted using AES-256 and stored locally on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    TradeFlowTheme {
        LoginScreenContent(
            uiState = LoginUiState.Initial,
            formState = CredentialFormState(
                apiKey = "organizations/abc-123",
                apiSecret = "***SECRET***"
            ),
            onNameChange = {},
            onApiKeyChange = {},
            onApiSecretChange = {},
            onToggleSecretVisibility = {},
            onTestConnection = {},
            onSaveCredentials = {},
            onDismissError = {}
        )
    }
}
```

---

## Testing

### Manual Testing Checklist

1. **Form Validation:**
   - [ ] Save button disabled when fields empty
   - [ ] Save button enabled when valid credentials entered
   - [ ] API key must start with "organizations/"
   - [ ] Secret field masked by default

2. **User Interactions:**
   - [ ] Name field optional (can be blank)
   - [ ] Toggle visibility shows/hides secret
   - [ ] Test Connection button works
   - [ ] Save button shows loading spinner
   - [ ] Error message displays correctly
   - [ ] Error can be dismissed

3. **Navigation:**
   - [ ] Success state navigates to dashboard
   - [ ] Credentials saved to SecureCredentialStore

4. **UI/UX:**
   - [ ] Screen scrollable when keyboard open
   - [ ] Security note visible
   - [ ] All text readable in dark theme

---

## Acceptance Criteria

- [ ] User can enter API credentials
- [ ] Form validates input (non-empty, correct format)
- [ ] Credentials saved to SecureCredentialStore on success
- [ ] Error states displayed clearly
- [ ] Loading states shown during async operations
- [ ] Secret field masked with toggle
- [ ] Security assurance message visible
- [ ] Navigation to dashboard on success
- [ ] Preview renders correctly

---

## Notes

### Test Connection Implementation

Test connection button is stubbed for now. When `CoinbaseRepository` is implemented (Ticket 08), update `testConnection()` to:
```kotlin
fun testConnection() {
    viewModelScope.launch {
        _uiState.value = LoginUiState.Loading
        try {
            // Create temporary credentials for testing
            val result = coinbaseRepository.getAccounts()
            if (result.isSuccess) {
                _uiState.value = LoginUiState.Initial
                // Show success toast
            } else {
                _uiState.value = LoginUiState.Error("Connection failed")
            }
        } catch (e: Exception) {
            _uiState.value = LoginUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

### Security Best Practices

- ✅ Credentials encrypted via EncryptedSharedPreferences
- ✅ Secret field masked by default
- ✅ No credentials logged (Timber in ViewModel logs events only)
- ✅ HTTPS enforced by Ktor
- ⚠️ User responsible for API key permissions (trade-only recommended)

---

## Dependencies

```kotlin
// app/build.gradle.kts
implementation(project(":core:domain"))      // CredentialStore interface
implementation(project(":core:data"))        // SecureCredentialStore implementation
implementation(project(":core:ui"))          // Theme & components
implementation(libs.hilt.navigation.compose) // hiltViewModel()
```

---

## Related Tickets

- **Blocked by:** 21 (Theme), 22 (Base Components)
- **Blocks:** 24 (Navigation - needs to route to this screen)
- **Uses:** Ticket 04 (SecureCredentialStore)
- **Reference:** 20 (UI Design Overview)
