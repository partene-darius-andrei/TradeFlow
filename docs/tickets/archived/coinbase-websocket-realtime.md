# 🔌 [SUPERSEDED] CoinbaseWebSocket - Real-time Data

Effort level: Large
Priority: High
Blocked by: Replaced by: 🟡 COINBASE: WebSocket Client

## Objective

Implement WebSocket client for real-time price and order updates.

## File

`data/remote/CoinbaseWebSocket.kt`

## Endpoints

- `wss://[advanced-trade-ws.coinbase.com](http://advanced-trade-ws.coinbase.com)` - Market data
- `wss://[advanced-trade-ws-user.coinbase.com](http://advanced-trade-ws-user.coinbase.com)` - User orders (auth required)

## Channels

| Channel | Purpose | Auth |
| --- | --- | --- |
| `heartbeats` | Keep alive (REQUIRED) | No |
| `ticker` | Real-time price | No |
| `user` | Order fills/status | Yes |

## Critical Requirements (From Research)

- **Must subscribe to heartbeats** - connection dies after 60-90 sec without activity
- Use **Ktor WebSocket** with OkHttp engine - good balance for periodic connections
- Set `pingInterval = 30_000` (30 seconds) for auto keep-alive
- User channel requires fresh JWT in subscription message

## Flows

```kotlin
val tickerFlow: SharedFlow<TickerUpdate>
val orderFlow: SharedFlow<OrderUpdate>
```

## Health Monitoring

- Track `lastMessageTime`
- 45-second watchdog coroutine
- Auto-reconnect with exponential backoff (5s → 60s max)

## Data Classes

```kotlin
data class TickerUpdate(
    val productId: String,
    val price: Double,
    val bid: Double,
    val ask: Double
)

data class OrderUpdate(
    val orderId: String,
    val clientOrderId: String,
    val status: String,
    val filledSize: Double,
    val avgFilledPrice: Double
)
```

## Acceptance Criteria

- Connects and receives ticker updates
- Reconnects automatically on failure
- Order updates flow to subscribers
- No disconnection during 1-hour test