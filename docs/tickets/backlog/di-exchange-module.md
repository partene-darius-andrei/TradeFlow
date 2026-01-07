# 💉 DI - Exchange Module (Hilt)

Effort level: Small
Priority: High
Status: Not started

## Objective

Hilt dependency injection module that binds exchange implementations to interfaces.

## File

`di/ExchangeModule.kt`

## Implementation

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ExchangeModule {

    @Binds
    @Singleton
    abstract fun bindExchangeRepository(
        impl: CoinbaseRepository
    ): ExchangeRepository

    @Binds
    @Singleton
    abstract fun bindBracketOrderRepository(
        impl: CoinbaseRepository
    ): BracketOrderRepository

    @Binds
    @Singleton
    abstract fun bindWebSocketService(
        impl: CoinbaseWebSocketService
    ): ExchangeWebSocketService

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(
        impl: CoinbaseJwtGenerator
    ): AuthTokenProvider
}
```

## Future: Multi-Exchange Support

```kotlin
// When adding Kraken/Binance, use MapKey binding:
@Module
@InstallIn(SingletonComponent::class)
abstract class ExchangeMultiModule {

    @Binds
    @IntoMap
    @ExchangeKey(Exchange.COINBASE)
    abstract fun bindCoinbase(impl: CoinbaseRepository): ExchangeRepository

    @Binds
    @IntoMap
    @ExchangeKey(Exchange.KRAKEN)
    abstract fun bindKraken(impl: KrakenRepository): ExchangeRepository
}

@Qualifier
@MapKey
annotation class ExchangeKey(val value: Exchange)

enum class Exchange { COINBASE, KRAKEN, BINANCE }
```

## Exchange Manager (Future)

```kotlin
@Singleton
class ExchangeManager @Inject constructor(
    private val exchanges: Map<Exchange, @JvmSuppressWildcards ExchangeRepository>,
    private val settings: SettingsRepository
) {
    val activeExchange: Flow<ExchangeRepository> = settings.selectedExchange
        .map { exchanges[it] ?: throw IllegalStateException("Exchange not configured") }
}
```

## Why This Matters

- **Single place to swap exchanges**
- Domain/UI code never changes when adding new exchange
- Can be A/B tested or feature flagged

## Acceptance Criteria

- [ ]  All interfaces bound to Coinbase implementations
- [ ]  Singleton scope for stateful services
- [ ]  Clean separation - no Coinbase imports in module signature