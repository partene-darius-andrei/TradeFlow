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

@OptIn(ExperimentalMaterial3Api::class)
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

            OutlinedTextField(
                value = formState.name,
                onValueChange = onNameChange,
                label = { Text("API Key Name (optional)") },
                placeholder = { Text("My Trading Bot") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

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

            OutlinedButton(
                onClick = onTestConnection,
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.isValid && !isLoading
            ) {
                Text("Test Connection")
            }

            LoadingButton(
                text = "Save & Continue",
                onClick = onSaveCredentials,
                modifier = Modifier.fillMaxWidth(),
                loading = isLoading,
                enabled = formState.isValid
            )

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
