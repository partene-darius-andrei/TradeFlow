# 🔌 INTERFACE - ExchangeRepository

Effort level: Medium
Priority: High
Status: Not started

## Objective

Define the core exchange abstraction interface that ALL domain logic uses. This is the **critical blocker** for API isolation.

## File

`domain/repository/ExchangeRepository.kt`

## Interface Definition

```kotlin
interface ExchangeRepository {
    // Account operations
    suspend fun getAccounts(): Result<List<Account>>
    suspend fun getAccountBalance(accountId: String): Result<Balance>
    
    // Order operations
    suspend fun placeOrder(order: OrderRequest): Result<OrderResponse>
    suspend fun cancelOrder(orderId: String): Result<Unit>
    suspend fun cancelOrders(orderIds: List<String>): Result<BatchCancelResult>
    suspend fun getOrder(orderId: String): Result<Order>
    suspend fun getOpenOrders(productId: String): Result<List<Order>>
    
    // Market data
    suspend fun getCandles(
        productId: String,
        granularity: CandleGranularity,
        startTime: Instant,
        endTime: Instant
    ): Result<List<Candle>>
    
    suspend fun getProducts(): Result<List<Product>>
}
```

## Extended Interface for Bracket Orders

```kotlin
interface BracketOrderRepository : ExchangeRepository {
    suspend fun placeBracketOrder(
        order: OrderRequest,
        takeProfitPrice: BigDecimal,
        stopLossPrice: BigDecimal
    ): Result<BracketOrderResponse>
}
```

## Why This Matters

- Domain layer imports ONLY this interface
- Coinbase implementation lives in `data/exchange/coinbase/`
- Future Kraken/Binance implementations in their own packages
- Swap exchanges by changing Hilt binding

## Acceptance Criteria

- [ ]  Interface defined with all required methods
- [ ]  No Coinbase-specific types in interface
- [ ]  Uses domain models only (Candle, Order, Account)
- [ ]  Result wrapper for error handling