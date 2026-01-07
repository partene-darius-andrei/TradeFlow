# ⚙️ FEATURE: Settings ViewModel (Logic)

Effort level: Small
Priority: Medium
Status: Not started
Blocked by: FEATURE: Settings Screen (UI Only), EXCHANGE-API: Repository Interfaces
Module: :feature:settings

## Objective

Implement Settings ViewModel for credential management.

## Module

`:feature:settings`

## ViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val credentialStore: CredentialStore,
    private val exchangeRepository: ExchangeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadCredentials()
    }
    
    private fun loadCredentials() {
        viewModelScope.launch {
            val hasCredentials = credentialStore.hasCredentials()
            _uiState.update { it.copy(
                hasCredentials = hasCredentials,
                apiKeyId = if (hasCredentials) "********" else ""
            )}
        }
    }
    
    fun updateApiKey(value: String) {
        _uiState.update { state ->
            state.copy(
                apiKeyId = value,
                apiKeyError = if (value.isNotEmpty() && !validateApiKeyFormat(value))
                    "Invalid format. Expected: organizations/.../apiKeys/..."
                else null
            )
        }
    }
    
    fun saveCredentials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                credentialStore.saveCredentials(
                    apiKey = _uiState.value.apiKeyId,
                    secret = _uiState.value.privateKey
                )
                _uiState.update { it.copy(isSaving = false, hasCredentials = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, apiKeyError = e.message) }
            }
        }
    }
    
    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, connectionStatus = ConnectionStatus.TESTING) }
            exchangeRepository.getBalances()
                .onSuccess {
                    _uiState.update { it.copy(isTesting = false, connectionStatus = ConnectionStatus.CONNECTED) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isTesting = false, connectionStatus = ConnectionStatus.FAILED) }
                }
        }
    }
    
    fun clearCredentials() {
        viewModelScope.launch {
            credentialStore.clearCredentials()
            _uiState.update { SettingsUiState() }
        }
    }
}
```

## Dependencies

- `:exchange:api` - CredentialStore, ExchangeRepository

## File Structure

```
feature/settings/src/main/kotlin/com/tradeflow/feature/settings/
└── viewmodel/
    └── SettingsViewModel.kt
```

## Acceptance Criteria

- [ ]  Credentials never logged
- [ ]  Test connection validates API works
- [ ]  Clear removes all stored data
- [ ]  Unit testable with fake CredentialStore