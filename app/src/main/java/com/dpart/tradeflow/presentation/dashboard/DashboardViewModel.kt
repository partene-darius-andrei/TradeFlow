package com.dpart.tradeflow.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.domain.repository.ExchangeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ExchangeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getBalances()
                .onSuccess { balances ->
                    val btc = balances.find { it.currency == "BTC" }
                    val usd = balances.find { it.currency == "USD" || it.currency == "USDC" }

                    val totalValue = usd?.available ?: BigDecimal.ZERO

                    _uiState.update { it.copy(
                        isLoading = false,
                        btcBalance = btc?.available ?: BigDecimal.ZERO,
                        usdBalance = usd?.available ?: BigDecimal.ZERO,
                        totalValue = totalValue
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load portfolio"
                    )}
                }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val btcBalance: BigDecimal = BigDecimal.ZERO,
    val usdBalance: BigDecimal = BigDecimal.ZERO,
    val totalValue: BigDecimal = BigDecimal.ZERO
)
