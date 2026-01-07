# 🎯 USECASE - PlaceOrder

Effort level: Small
Priority: High
Status: Not started

## Objective

Domain use case for placing orders with validation.

## File

`domain/usecase/PlaceOrderUseCase.kt`

## Implementation

```kotlin
class PlaceOrderUseCase @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val bracketRepository: BracketOrderRepository,
    private val orderDao: OrderDao,
    private val riskManager: RiskManager
) {
    suspend operator fun invoke(request: PlaceOrderRequest): Result<Order> {
        // 1. Validate against risk limits
        val riskCheck = riskManager.validateOrder(request)
        if (riskCheck is RiskCheck.Rejected) {
            return Result.failure(RiskLimitException(riskCheck.reason))
        }
        
        // 2. Place order based on type
        val result = when (request) {
            is PlaceOrderRequest.Limit -> {
                exchangeRepository.placeOrder(request.toOrderRequest())
            }
            is PlaceOrderRequest.Bracket -> {
                bracketRepository.placeBracketOrder(
                    order = request.toOrderRequest(),
                    takeProfitPrice = request.takeProfitPrice,
                    stopLossPrice = request.stopLossPrice
                )
            }
            is [PlaceOrderRequest.Market](http://PlaceOrderRequest.Market) -> {
                exchangeRepository.placeOrder(request.toOrderRequest())
            }
        }
        
        // 3. Persist to local DB
        result.onSuccess { order ->
            orderDao.insert(order.toEntity())
        }
        
        return result
    }
}

sealed class PlaceOrderRequest {
    abstract val productId: String
    abstract val side: OrderSide
    abstract val size: BigDecimal
    
    data class Limit(
        override val productId: String,
        override val side: OrderSide,
        override val size: BigDecimal,
        val price: BigDecimal,
        val gridLevel: Int? = null
    ) : PlaceOrderRequest()
    
    data class Bracket(
        override val productId: String,
        override val side: OrderSide,
        override val size: BigDecimal,
        val entryPrice: BigDecimal,
        val takeProfitPrice: BigDecimal,
        val stopLossPrice: BigDecimal
    ) : PlaceOrderRequest()
    
    data class Market(
        override val productId: String,
        override val side: OrderSide,
        override val size: BigDecimal
    ) : PlaceOrderRequest()
}
```

## Depends On

- 🔌 INTERFACE - ExchangeRepository
- 🔌 INTERFACE - BracketOrderRepository
- 🚨 RiskManager
- 🗄️ Room Database

## Acceptance Criteria

- [ ]  Validates against risk limits before placing
- [ ]  Supports all order types
- [ ]  Persists orders locally
- [ ]  Unit testable with fake repositories