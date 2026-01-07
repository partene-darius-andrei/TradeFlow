# 🏛️ COINBASE - WebSocket Implementation

Effort level: Large
Priority: High

## Objective

Implement `ExchangeWebSocketService` interface for Coinbase Advanced Trade WebSocket.

## File

`data/exchange/coinbase/CoinbaseWebSocketService.kt`

## Endpoints (VALIDATED)

- Market data: `wss://[advanced-trade-ws.coinbase.com](http://advanced-trade-ws.coinbase.com)`
- User data: `wss://[advanced-trade-ws-user.coinbase.com](http://advanced-trade-ws-user.coinbase.com)`

## Available Channels (COMPLETE LIST)

| Channel | Auth | Purpose |
| --- | --- | --- |
| `heartbeats` | No | Keep-alive (REQUIRED) |
| `ticker` | No | Real-time price |
| `ticker_batch` | No | Batched price updates |
| `level2` | No | Order book |
| `market_trades` | No | Trade history |
| `candles` | No | OHLCV updates |
| `status` | No | Product status |
| `user` | **Yes** | Order fills/updates |
| `futures_balance_summary` | Yes | Futures only |

## Rate Limits

- **750 requests/second** per connection
- Heartbeat required every 60 seconds

## Implementation

```kotlin
class CoinbaseWebSocketService @Inject constructor(
    private val authProvider: AuthTokenProvider,
    private val okHttpClient: OkHttpClient
) : ExchangeWebSocketService {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val tickerChannel = Channel<TickerUpdate>(Channel.BUFFERED)
    private val orderChannel = Channel<OrderUpdate>(Channel.BUFFERED)
    
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    
    override fun connect() {
        _connectionState.value = ConnectionState.CONNECTING
        
        val request = Request.Builder()
            .url("wss://[advanced-trade-ws.coinbase.com](http://advanced-trade-ws.coinbase.com)")
            .build()
            
        webSocket = okHttpClient.newWebSocket(request, createListener())
    }
    
    override fun subscribeTicker(productIds: List<String>): Flow<TickerUpdate> {
        val message = buildSubscribeMessage(
            channel = "ticker",
            productIds = productIds
        )
        webSocket?.send(message)
        return tickerChannel.receiveAsFlow()
    }
    
    override fun subscribeUserOrders(): Flow<OrderUpdate> {
        // User channel requires separate authenticated connection
        connectUserWebSocket()
        return orderChannel.receiveAsFlow()
    }
    
    private fun connectUserWebSocket() {
        val token = runBlocking { authProvider.generateWebSocketToken() }
        val request = Request.Builder()
            .url("wss://[advanced-trade-ws-user.coinbase.com](http://advanced-trade-ws-user.coinbase.com)")
            .build()
        // ... subscribe with JWT in message
    }
    
    private fun handleReconnect() {
        reconnectJob = scope.launch {
            var delay = 5_000L
            while (connectionState.value != ConnectionState.CONNECTED) {
                _connectionState.value = ConnectionState.RECONNECTING
                delay(delay)
                connect()
                delay = minOf(delay * 2, 60_000L)  // Max 60s
            }
        }
    }
}
```

## Message Format

```json
{
  "type": "subscribe",
  "channel": "ticker",
  "product_ids": ["BTC-USD"],
  "jwt": "<token>"  // Only for user channel
}
```

## Depends On

- 🔌 INTERFACE - ExchangeWebSocketService
- 🔌 INTERFACE - AuthTokenProvider
- 🏛️ COINBASE - JWT Generator

## Acceptance Criteria

- [ ]  Implements ExchangeWebSocketService interface
- [ ]  Handles both market and user connections
- [ ]  Auto-reconnect with exponential backoff (5s → 60s)
- [ ]  Heartbeat subscription automatic
- [ ]  Survives 1-hour stability test