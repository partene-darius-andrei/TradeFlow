package com.dpart.tradeflow.presentation.login

sealed interface LoginUiState {
    data object Initial : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
    data object Success : LoginUiState
}

data class CredentialFormState(
    val name: String = "",
    val apiKey: String = "",
    val apiSecret: String = "",
    val apiSecretVisible: Boolean = false,
    val isValid: Boolean = false
)
