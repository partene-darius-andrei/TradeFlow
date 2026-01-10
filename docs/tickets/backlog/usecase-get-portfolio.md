# 🎯 USECASE - GetPortfolio

Effort level: Small
Priority: High

## Objective

Domain use case to fetch and calculate portfolio state.

## File

`domain/usecase/GetPortfolioUseCase.kt`

## Implementation

```kotlin
class GetPortfolioUseCase @Inject constructor(
    private val exchangeRepository: ExchangeRepository,  // Interface!
    private val portfolioDao: PortfolioDao
) {
    suspend operator fun invoke(currentPrice: BigDecimal): Result<Portfolio> {
        return exchangeRepository.getAccounts().map { accounts ->
            val usd = accounts.find { it.currency == "USD" }?.available ?: [BigDecimal.ZERO](http://BigDecimal.ZERO)
            val btc = accounts.find { it.currency == "BTC" }?.available ?: [BigDecimal.ZERO](http://BigDecimal.ZERO)
            
            val btcValue = btc * currentPrice
            val totalEquity = usd + btcValue
            
            // Get high water mark from local DB
            val hwm = portfolioDao.getHighWaterMark() ?: totalEquity
            val newHwm = maxOf(hwm, totalEquity)
            
            // Calculate drawdown
            val drawdown = if (newHwm > [BigDecimal.ZERO](http://BigDecimal.ZERO)) {
                ((newHwm - totalEquity) / newHwm * BigDecimal(100)).toDouble()
            } else 0.0
            
            // Save snapshot
            portfolioDao.insertSnapshot(PortfolioSnapshot(
                totalEquity = totalEquity,
                cashUsd = usd,
                btcValue = btcValue,
                highWaterMark = newHwm,
                drawdownPercent = drawdown,
                timestamp = [Instant.now](http://Instant.now)()
            ))
            
            Portfolio(
                totalEquity = totalEquity,
                cashBalance = usd,
                btcValue = btcValue,
                drawdownPercent = drawdown,
                highWaterMark = newHwm
            )
        }
    }
}

data class Portfolio(
    val totalEquity: BigDecimal,
    val cashBalance: BigDecimal,
    val btcValue: BigDecimal,
    val drawdownPercent: Double,
    val highWaterMark: BigDecimal
)
```

## Depends On

- 🔌 INTERFACE - ExchangeRepository
- 🗄️ Room Database

## Acceptance Criteria

- [ ]  Uses ExchangeRepository interface (not Coinbase directly)
- [ ]  Calculates drawdown correctly
- [ ]  Persists snapshots to Room
- [ ]  Unit testable with fake repository
