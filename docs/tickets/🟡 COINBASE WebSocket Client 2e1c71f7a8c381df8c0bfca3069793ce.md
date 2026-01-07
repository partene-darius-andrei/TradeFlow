# 🟡 COINBASE: WebSocket Client

Effort level: Large
Priority: High
Status: Not started
Blocked by: COINBASE: JWT Token Generator
Module: :exchange:coinbase

## Objective

Implement Coinbase WebSocket for real-time data.

## Module

`:exchange:coinbase`

## Implements

`ExchangeWebSocket`

## Endpoints (Validated)

- Market data: `wss://[advanced-trade-ws.coinbase.com](http://advanced-trade-ws.coinbase.com)`
- User data: `wss://[advanced-trade-ws-user.coinbase.com](http://advanced-trade-ws-user.coinbase.com)`

## Channels (Validated)

| Channel | Auth Required | Purpose |
| --- | --- | --- |
| `heartbeats` | No | Keep-alive (REQUIRED) |
| `ticker` | No | Real-time price |
| `ticker_batch` | No | Batched price updates |
| `level2` | No | Order book |
| `user` | **Yes** | Order fills/status |
| `market_trades` | No | Trade history |
| `candles` | No | Real-time candles |
| `status` | No | Product status |

## Implementation

```kotlin
class CoinbaseWebSocket(
    private val authProvider: AuthTokenProvider,
    private val json: Json
) : ExchangeWebSocket {
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val marketWs = OkHttpClient().newWebSocket(...)
    private val userWs = OkHttpClient().newWebSocket(...)  // Separate connection
    
    private val _tickerFlow = MutableSharedFlow<Ticker>()
    private val _orderFlow = MutableSharedFlow<Order>()
    
    override fun subscribeTicker(productIds: List<String>): Flow<Ticker> {
        sendSubscribe(
            channel = "ticker",
            productIds = productIds
        )
        return _tickerFlow.filter { it.productId in productIds }
    }
    
    override fun subscribeOrderUpdates(): Flow<Order> {
        val jwt = runBlocking { authProvider.getWebSocketToken() }
        sendUserSubscribe(
            channel = "user",
            jwt = jwt
        )
        return _orderFlow
    }
}
```

## Critical Requirements

- **Must subscribe to heartbeats** - connection dies after ~60-90 sec
- Set `pingInterval = 30_000` for auto keep-alive
- User channel requires fresh JWT in subscription
- Auto-reconnect with exponential backoff (5s → 60s max)

## Health Monitoring

```kotlin
private val watchdog = scope.launch {
    while (isActive) {
        delay(45_000)
        if (lastMessageTime.elapsedNow() > 45.seconds) {
            reconnect()
        }
    }
}
```

## File Structure

```
exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
└── websocket/
    ├── CoinbaseWebSocket.kt
    ├── CoinbaseWebSocketMessage.kt
    └── WebSocketReconnectPolicy.kt
```

## Acceptance Criteria

- [ ]  Connects and receives ticker updates
- [ ]  Reconnects automatically on failure
- [ ]  Order updates flow to subscribers
- [ ]  No disconnection during 1-hour test