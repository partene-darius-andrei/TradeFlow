package com.dpart.tradeflow.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.usecase.UpdatePortfolioUseCase
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
    private val updatePortfolioUseCase: UpdatePortfolioUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            updatePortfolioUseCase.execute()
                .onSuccess { portfolio ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        btcBalance = portfolio.getBtcBalance(),
                        usdBalance = portfolio.getUsdBalance(),
                        totalValue = portfolio.totalEquityUsd
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load portfolio"
                    ) }
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
