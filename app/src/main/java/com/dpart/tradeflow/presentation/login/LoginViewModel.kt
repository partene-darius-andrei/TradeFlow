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

    private fun validateForm() {
        val form = _formState.value
        val isValid = form.apiKey.isNotBlank() &&
                form.apiSecret.isNotBlank() &&
                form.apiKey.startsWith("organizations/")
        _formState.value = form.copy(isValid = isValid)
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
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

    fun saveCredentials() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val form = _formState.value

                credentialStore.saveCredentials(
                    apiKey = form.apiKey,
                    secret = form.apiSecret
                )

                Timber.i("Credentials saved successfully")
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                Timber.e(e, "Failed to save credentials")
                _uiState.value = LoginUiState.Error(e.message ?: "Failed to save credentials")
            }
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Initial
        }
    }
}
