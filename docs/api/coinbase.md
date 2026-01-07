# Coinbase Advanced Trade API Integration

**Parent:** [../reference.md](../reference.md)

Complete reference for integrating with Coinbase Advanced Trade API.

---

## API Endpoints

**Base URL:** `https://api.coinbase.com/api/v3/brokerage/`

| Operation | Method | Endpoint | Purpose |
|-----------|--------|----------|---------|
| Create Order | POST | `/orders` | Place new orders |
| Cancel Orders | POST | `/orders/batch_cancel` | Cancel multiple orders |
| List Orders | GET | `/orders/historical/batch` | Get order history |
| Get Order | GET | `/orders/historical/{order_id}` | Single order details |
| List Fills | GET | `/orders/historical/fills` | Execution history |
| List Accounts | GET | `/accounts` | Get balances |
| Get Product | GET | `/products/{product_id}` | Product info (min size, etc.) |
| Get Candles | GET | `/products/{product_id}/candles` | OHLCV historical data |

**Rate Limits:**
- REST API: **10,000 requests/hour** per API key
- WebSocket connections: **750/second** per IP
- WebSocket unauthenticated messages: **8/second** per IP

---

## WebSocket Endpoints

| Endpoint | Purpose | Auth Required |
|----------|---------|---------------|
| `wss://advanced-trade-ws.coinbase.com` | Market data | No (but recommended) |
| `wss://advanced-trade-ws-user.coinbase.com` | User orders/fills | Yes |

**Available Channels:**

| Channel | Description | Auth | Use Case |
|---------|-------------|------|----------|
| `heartbeats` | Keep connection alive | No | **REQUIRED** - prevents 60-90s timeout |
| `ticker` | Real-time price (10-50ms) | No | Price monitoring |
| `ticker_batch` | Price every 5 seconds | No | Lower bandwidth option |
| `candles` | 5-minute candles only | No | Real-time candle updates |
| `level2` | Order book updates | No | Not needed for Engine |
| `user` | Order fills & status | Yes | **REQUIRED** - order state sync |
| `market_trades` | All trades | No | Not needed for Engine |

**WebSocket Message Format (Subscribe):**
```json
{
  "type": "subscribe",
  "product_ids": ["BTC-USD"],
  "channel": "ticker",
  "jwt": "your_jwt_token_here"
}
```

---

## Authentication (JWT ES256)

Coinbase uses JWT with **ES256 (ECDSA P-256)** signing.

**CRITICAL:** You must use the private key provided by Coinbase when creating API credentials. You CANNOT generate your own key.

**JWT Header:**
```json
{
  "alg": "ES256",
  "typ": "JWT",
  "kid": "organizations/{org_id}/apiKeys/{key_id}",
  "nonce": "random_hex_16_bytes"
}
```

**JWT Payload:**
```json
{
  "iss": "cdp",
  "sub": "organizations/{org_id}/apiKeys/{key_id}",
  "nbf": 1704067200,
  "exp": 1704067320,
  "uri": "POST api.coinbase.com/api/v3/brokerage/orders"
}
```

**Key Points:**
- Token expires in **120 seconds** (2 minutes)
- `uri` format: `{METHOD} {host}{path}` (no https://)
- For WebSocket: `uri` can be omitted or use empty path
- Nonce should be random hex string (16 bytes = 32 chars)

**See:** [../implementation/security.md](../implementation/security.md) for JwtGenerator implementation

---

## Order Types

### Market Order (Emergency Exit)
```json
{
  "client_order_id": "uuid-v4",
  "product_id": "BTC-USD",
  "side": "SELL",
  "order_configuration": {
    "market_market_ioc": {
      "base_size": "0.001"
    }
  }
}
```

### Limit Order - Maker Only (Grid Trading)
```json
{
  "client_order_id": "uuid-v4",
  "product_id": "BTC-USD",
  "side": "BUY",
  "order_configuration": {
    "limit_limit_gtc": {
      "base_size": "0.001",
      "limit_price": "49000.00",
      "post_only": true
    }
  }
}
```
**Note:** `post_only: true` ensures maker-only execution. Order is rejected if it would match immediately (taker).

### Bracket Order - Entry + TP + SL (Trend Trading)
```json
{
  "client_order_id": "uuid-v4",
  "product_id": "BTC-USD",
  "side": "BUY",
  "order_configuration": {
    "limit_limit_gtc": {
      "base_size": "0.001",
      "limit_price": "50000.00"
    }
  },
  "attached_order_configuration": {
    "trigger_bracket_gtc": {
      "limit_price": "55000.00",
      "stop_trigger_price": "48000.00"
    }
  }
}
```

**IMPORTANT - Bracket Order Fields:**
- In `attached_order_configuration.trigger_bracket_gtc`:
    - `limit_price` = **Take Profit** price
    - `stop_trigger_price` = **Stop Loss** trigger
- Do NOT include `base_size` in attached config (inherits from parent)

### Stop-Limit Order (Alternative Stop Loss)
```json
{
  "client_order_id": "uuid-v4",
  "product_id": "BTC-USD",
  "side": "SELL",
  "order_configuration": {
    "stop_limit_stop_limit_gtc": {
      "base_size": "0.001",
      "limit_price": "47500.00",
      "stop_price": "48000.00",
      "stop_direction": "STOP_DIRECTION_STOP_DOWN"
    }
  }
}
```

---

## Order Response Format

**Success:**
```json
{
  "success": true,
  "success_response": {
    "order_id": "abc123",
    "product_id": "BTC-USD",
    "side": "BUY",
    "client_order_id": "your-uuid"
  }
}
```

**Failure:**
```json
{
  "success": false,
  "error_response": {
    "error": "INSUFFICIENT_FUND",
    "message": "Insufficient balance in source account",
    "error_details": "...",
    "new_order_failure_reason": "INSUFFICIENT_FUND"
  }
}
```

---

## Order Status Lifecycle

```
PENDING → OPEN → FILLED
                ↘ CANCELLED
                ↘ EXPIRED
                ↘ FAILED
```

**Partial Fills:** Track via `filled_size`, `completion_percentage`, `leaves_quantity`

---

## Fee Structure

| Tier | 30-Day Volume | Maker Fee | Taker Fee | Grid Break-Even |
|------|---------------|-----------|-----------|-----------------|
| Intro | $0 - $1K | **0.60%** | 1.20% | 1.20% (use 1.5%+) |
| Intro 2 | $1K - $10K | 0.35% | 0.75% | 0.70% |
| Advanced 1 | $10K - $50K | **0.25%** | 0.40% | 0.50% |
| Advanced 2 | $50K - $100K | 0.15% | 0.25% | 0.30% |

**Grid Break-Even Formula:** `spacing > (maker_fee × 2)`

At intro tier (0.60% maker), a 1% grid loses 0.2% per round trip after fees.

---

## Candle Data

**REST Endpoint:** `GET /products/{product_id}/candles`

**Parameters:**
| Param | Values |
|-------|--------|
| `granularity` | ONE_MINUTE, FIVE_MINUTE, FIFTEEN_MINUTE, THIRTY_MINUTE, ONE_HOUR, TWO_HOUR, SIX_HOUR, ONE_DAY |
| `start` | ISO 8601 timestamp |
| `end` | ISO 8601 timestamp |

**Limits:** Maximum **350 candles** per request

**For H4 Candles:** Use `TWO_HOUR` granularity and aggregate 2 candles, OR use `SIX_HOUR` if less precision is acceptable.

**WebSocket Candles:** Only **5-minute** granularity available

---

## Sandbox Environment

**URL:** `https://api-sandbox.coinbase.com/api/v3/brokerage/`

**LIMITATION:** The Advanced Trade sandbox returns **static mock data only**:
- Orders appear to succeed but never fill
- Market data doesn't update
- Only useful for testing API structure, NOT trading logic

**Alternatives for Testing:**
1. **Local simulation** - Record real data, replay against strategy
2. **Small real trades** - Use $10-20 positions on mainnet
3. **Bitsgap demo mode** - Third-party paper trading

---

## Navigation

- **[Back to Technical Reference](../reference.md)** - Parent document
- **[Next: Trading Strategy](../strategy/overview.md)** - Strategy specification
- **[Implementation: API Clients](../implementation/clients.md)** - Code examples for REST/WebSocket
