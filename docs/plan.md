# Engine Trading System: Implementation Blueprint v2.1

**Document Status:** FINAL - Incorporates all Coinbase API research  
**Target Platform:** Android (API 26+)  
**Exchange:** Coinbase Advanced Trade API  
**Strategy:** Regime-Switching (Trend + Grid + Defense)

---

## Executive Summary

Engine is a rule-based, regime-adaptive cryptocurrency trading system for Android. It detects market conditions and switches between strategies accordingly:

| Mode | Condition | Action |
|------|-----------|--------|
| **DEFENSE** | Price < SMA(200) | Cash preservation, cancel buys |
| **TREND** | Price > SMA(200) AND ADX > 25 | Ride trends with bracket orders |
| **RANGE** | Price > SMA(200) AND ADX < 25 | Grid trading to harvest volatility |

---

## Critical Constraints (From Research)

| Constraint | Value | Impact |
|------------|-------|--------|
| **Minimum grid spacing** | 1.5% | Due to 0.60% maker fees at intro tier |
| **Must use `post_only: true`** | Always | Ensures maker fees (0.60% vs 1.20% taker) |
| **JWT token expiry** | 2 minutes | Regenerate per request |
| **WebSocket timeout** | 60-90 seconds | Must subscribe to heartbeats channel |
| **Max candles per request** | 350 | Need multiple calls for 200+ H4 candles |
| **REST rate limit** | 10,000/hour | Use WebSocket for real-time data |
| **Max open orders** | 500 per product | More than enough for grid |
| **Sandbox limitation** | Static responses only | Cannot use for paper trading |

---

## Part 1: Coinbase API Technical Reference

### 1.1 API Endpoints

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

### 1.2 WebSocket Endpoints

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

### 1.3 Authentication (JWT ES256)

Coinbase uses JWT with **ES256 (ECDSA P-256)** signing.

**⚠️ CRITICAL:** You must use the private key provided by Coinbase when creating API credentials. You CANNOT generate your own key.

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

### 1.4 Order Types

#### Market Order (Emergency Exit)
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

#### Limit Order - Maker Only (Grid Trading)
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

#### Bracket Order - Entry + TP + SL (Trend Trading)
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

**⚠️ IMPORTANT - Bracket Order Fields:**
- In `attached_order_configuration.trigger_bracket_gtc`:
    - `limit_price` = **Take Profit** price
    - `stop_trigger_price` = **Stop Loss** trigger
- Do NOT include `base_size` in attached config (inherits from parent)

#### Stop-Limit Order (Alternative Stop Loss)
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

### 1.5 Order Response Format

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

### 1.6 Order Status Lifecycle

```
PENDING → OPEN → FILLED
                ↘ CANCELLED
                ↘ EXPIRED
                ↘ FAILED
```

**Partial Fills:** Track via `filled_size`, `completion_percentage`, `leaves_quantity`

### 1.7 Fee Structure

| Tier | 30-Day Volume | Maker Fee | Taker Fee | Grid Break-Even |
|------|---------------|-----------|-----------|-----------------|
| Intro | $0 - $1K | **0.60%** | 1.20% | 1.20% (use 1.5%+) |
| Intro 2 | $1K - $10K | 0.35% | 0.75% | 0.70% |
| Advanced 1 | $10K - $50K | **0.25%** | 0.40% | 0.50% |
| Advanced 2 | $50K - $100K | 0.15% | 0.25% | 0.30% |

**Grid Break-Even Formula:** `spacing > (maker_fee × 2)`

At intro tier (0.60% maker), a 1% grid loses 0.2% per round trip after fees.

### 1.8 Candle Data

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

### 1.9 Sandbox Environment

**URL:** `https://api-sandbox.coinbase.com/api/v3/brokerage/`

**⚠️ LIMITATION:** The Advanced Trade sandbox returns **static mock data only**:
- Orders appear to succeed but never fill
- Market data doesn't update
- Only useful for testing API structure, NOT trading logic

**Alternatives for Testing:**
1. **Local simulation** - Record real data, replay against strategy
2. **Small real trades** - Use $10-20 positions on mainnet
3. **Bitsgap demo mode** - Third-party paper trading

---

## Part 2: Strategy Specification (Phase 1)

### 2.1 What's IN Phase 1

| Component | Implementation |
|-----------|----------------|
| Trend detection | SMA(200) on H4 candles |
| Trend strength | ADX(14) on H4 candles |
| Volatility sizing | ATR(14) on H4 candles |
| Mode switching | 3-candle hysteresis (except DEFENSE) |
| Position sizing | Fixed 5% per trade |
| Grid spacing | max(1.5%, ATR-based) |
| Risk limit | 15% drawdown kills service |

### 2.2 What's OUT of Phase 1

- ❌ Fear & Greed Index (add in Phase 2 if needed)
- ❌ RSI entry timing
- ❌ Multiple timeframes
- ❌ ML regime prediction
- ❌ On-chain metrics

### 2.3 Decision Logic

```
Every H4 candle (4 hours):

┌─────────────────────────────────────────────┐
│ Is price BELOW SMA(200)?                    │
└─────────────────────────────────────────────┘
        │ YES                    │ NO
        ▼                        ▼
┌───────────────┐    ┌─────────────────────────────────┐
│ DEFENSE MODE  │    │ Has ADX been > 25 for 3 candles?│
│ (Instant)     │    └─────────────────────────────────┘
│ • Cancel buys │           │ YES              │ NO
│ • Set stops   │           ▼                  ▼
│ • Hold cash   │    ┌─────────────┐    ┌─────────────────────────────────┐
└───────────────┘    │ TREND MODE  │    │ Has ADX been < 25 for 3 candles?│
                     │ • Bracket   │    └─────────────────────────────────┘
                     │   order     │           │ YES              │ NO
                     │ • TP: +6ATR │           ▼                  ▼
                     │ • SL: -3ATR │    ┌─────────────┐    ┌───────────┐
                     └─────────────┘    │ RANGE MODE  │    │ WAIT      │
                                        │ • Grid buys │    │ (Keep     │
                                        │ • 1.5% min  │    │  current) │
                                        │ • post_only │    └───────────┘
                                        └─────────────┘
```

### 2.4 Risk Limits (Hardcoded)

| Limit | Value | Enforcement |
|-------|-------|-------------|
| Max position/trade | 5% portfolio | Reject if exceeded |
| Max total exposure | 10% portfolio | No new orders |
| Max correlated assets | 1 | Block second asset |
| Portfolio drawdown | 15% from HWM | Emergency liquidate + stop |
| Unfilled order timeout | 48 hours | Cancel and re-evaluate |

---

## Part 3: Android Architecture

### 3.1 Tech Stack

| Component | Library | Rationale |
|-----------|---------|-----------|
| HTTP/WebSocket | **OkHttp 4.x** | 10x better battery than Ktor for WebSocket |
| JSON | kotlinx.serialization | Type-safe, fast |
| Database | Room | SQLite with compile-time checks |
| Background | Foreground Service | Required for 24/7 |
| Security | EncryptedSharedPreferences | API key storage |
| Indicators | ta4j | Battle-tested TA library |
| JWT | nimbus-jose-jwt | ES256 signing |
| Scheduler | WorkManager | Dead-man-switch backup |

### 3.2 Why OkHttp over Ktor

From research: Ktor WebSocket has **10x higher CPU usage** than OkHttp for long-running connections. For a 24/7 trading bot on battery, this is critical.

```kotlin
// OkHttp - use pingInterval for automatic keep-alive
val client = OkHttpClient.Builder()
    .pingInterval(30, TimeUnit.SECONDS)  // Handles heartbeat
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MINUTES)    // No read timeout for WS
    .build()
```

### 3.3 Doze Mode Survival Strategy

| Layer | Tool | Purpose |
|-------|------|---------|
| Primary | Battery optimization exemption | Prevent OS from killing |
| WebSocket | OkHttp pingInterval (30s) | Keep connection alive |
| Internal | Coroutine watchdog (45s) | Detect dead WebSocket |
| Backup | WorkManager (15-min periodic) | Restart service if killed |

**Critical for Xiaomi/Huawei/Samsung:** These vendors have aggressive battery optimization. Users MUST manually disable it for the app.

### 3.4 Project Structure

```
com.engine.trade/
├── data/
│   ├── local/
│   │   ├── EngineDatabase.kt
│   │   ├── dao/OrderDao.kt
│   │   └── entity/
│   │       ├── OrderEntity.kt
│   │       └── PortfolioEntity.kt
│   ├── remote/
│   │   ├── CoinbaseRestApi.kt
│   │   ├── CoinbaseWebSocket.kt
│   │   └── dto/
│   │       ├── OrderRequest.kt
│   │       └── OrderResponse.kt
│   └── security/
│       ├── SecureKeyStore.kt
│       └── JwtGenerator.kt
├── domain/
│   ├── model/
│   │   ├── Candle.kt
│   │   └── Decision.kt
│   └── strategy/
│       └── EngineDecisionEngine.kt
├── service/
│   ├── TradingService.kt
│   └── ServiceWatchdog.kt
└── ui/
    ├── MainActivity.kt
    └── screens/
```

---

## Part 4: Implementation Code

### 4.1 Domain Models

```kotlin
// domain/model/Candle.kt
package com.engine.trade.domain.model

import java.time.Instant

data class Candle(
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

// domain/model/Decision.kt
package com.engine.trade.domain.model

sealed class Decision {
    data class Wait(val reason: String) : Decision()
    
    data class Defense(val reason: String) : Decision()
    
    data class Trend(
        val stopLossPrice: Double,
        val takeProfitPrice: Double,
        val atr: Double
    ) : Decision()
    
    data class Range(
        val gridSpacing: Double,
        val atr: Double
    ) : Decision()
}
```

### 4.2 Decision Engine

```kotlin
// domain/strategy/EngineDecisionEngine.kt
package com.engine.trade.domain.strategy

import com.engine.trade.domain.model.Candle
import com.engine.trade.domain.model.Decision
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.ATRIndicator
import org.ta4j.core.indicators.SMAIndicator
import org.ta4j.core.indicators.adx.ADXIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import java.time.Duration
import java.time.ZoneOffset

class EngineDecisionEngine {

    // Hysteresis counters (persist across evaluations)
    private var trendConfirmCount = 0
    private var rangeConfirmCount = 0
    
    companion object {
        private const val SMA_PERIOD = 200
        private const val ADX_PERIOD = 14
        private const val ATR_PERIOD = 14
        private const val ADX_TREND_THRESHOLD = 25.0
        private const val HYSTERESIS_CANDLES = 3
        private const val MIN_GRID_SPACING_PERCENT = 0.015 // 1.5%
        private const val STOP_LOSS_ATR_MULT = 3.0
        private const val TAKE_PROFIT_ATR_MULT = 6.0  // 2:1 R:R
    }

    fun evaluate(candles: List<Candle>, currentPrice: Double): Decision {
        
        // Need enough history for SMA(200)
        if (candles.size < SMA_PERIOD) {
            return Decision.Wait("Initializing: ${candles.size}/$SMA_PERIOD candles")
        }

        // Build ta4j series from candles
        val series = BaseBarSeriesBuilder().withName("Engine").build()
        candles.forEach { candle ->
            series.addBar(
                Duration.ofHours(4),
                candle.timestamp.atZone(ZoneOffset.UTC),
                candle.open,
                candle.high,
                candle.low,
                candle.close,
                candle.volume
            )
        }

        // Calculate indicators
        val closePrice = ClosePriceIndicator(series)
        val lastIndex = series.endIndex
        
        val sma200 = SMAIndicator(closePrice, SMA_PERIOD)
            .getValue(lastIndex).doubleValue()
        
        val adx14 = ADXIndicator(series, ADX_PERIOD)
            .getValue(lastIndex).doubleValue()
        
        val atr14 = ATRIndicator(series, ATR_PERIOD)
            .getValue(lastIndex).doubleValue()

        // Regime detection
        val isPriceAboveSma = currentPrice > sma200
        val isStrongTrend = adx14 > ADX_TREND_THRESHOLD

        // State machine with hysteresis
        return when {
            // DEFENSE: Instant switch (safety first, no hysteresis)
            !isPriceAboveSma -> {
                resetCounters()
                Decision.Defense("Price ($currentPrice) below SMA ($sma200)")
            }
            
            // TREND: Requires 3 consecutive confirmations
            isStrongTrend -> {
                trendConfirmCount++
                rangeConfirmCount = 0
                
                if (trendConfirmCount >= HYSTERESIS_CANDLES) {
                    Decision.Trend(
                        stopLossPrice = currentPrice - (STOP_LOSS_ATR_MULT * atr14),
                        takeProfitPrice = currentPrice + (TAKE_PROFIT_ATR_MULT * atr14),
                        atr = atr14
                    )
                } else {
                    Decision.Wait("Trend confirming: $trendConfirmCount/$HYSTERESIS_CANDLES")
                }
            }
            
            // RANGE: Requires 3 consecutive confirmations
            else -> {
                rangeConfirmCount++
                trendConfirmCount = 0
                
                if (rangeConfirmCount >= HYSTERESIS_CANDLES) {
                    // Grid spacing: max of 1.5% or ATR-based
                    val atrSpacing = atr14
                    val minSpacing = currentPrice * MIN_GRID_SPACING_PERCENT
                    val spacing = maxOf(atrSpacing, minSpacing)
                    
                    Decision.Range(gridSpacing = spacing, atr = atr14)
                } else {
                    Decision.Wait("Range confirming: $rangeConfirmCount/$HYSTERESIS_CANDLES")
                }
            }
        }
    }
    
    private fun resetCounters() {
        trendConfirmCount = 0
        rangeConfirmCount = 0
    }
    
    // For testing/debugging
    fun getState(): String = "trend=$trendConfirmCount, range=$rangeConfirmCount"
}
```

### 4.3 Secure Key Storage

```kotlin
// data/security/SecureKeyStore.kt
package com.engine.trade.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureKeyStore(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "engine_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveCredentials(apiKeyId: String, privateKeyPem: String) {
        prefs.edit()
            .putString(KEY_API_ID, apiKeyId)
            .putString(KEY_PRIVATE_PEM, privateKeyPem)
            .apply()
    }
    
    fun getApiKeyId(): String? = prefs.getString(KEY_API_ID, null)
    
    fun getPrivateKeyPem(): String? = prefs.getString(KEY_PRIVATE_PEM, null)
    
    fun hasCredentials(): Boolean = 
        !getApiKeyId().isNullOrBlank() && !getPrivateKeyPem().isNullOrBlank()
    
    fun clearCredentials() {
        prefs.edit().clear().apply()
    }
    
    companion object {
        private const val KEY_API_ID = "coinbase_api_key_id"
        private const val KEY_PRIVATE_PEM = "coinbase_private_key_pem"
    }
}
```

### 4.4 JWT Generator

```kotlin
// data/security/JwtGenerator.kt
package com.engine.trade.data.security

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

class JwtGenerator(private val keyStore: SecureKeyStore) {
    
    /**
     * Generate JWT for REST API calls
     * @param method HTTP method (GET, POST, etc.)
     * @param host API host (api.coinbase.com)
     * @param path API path (/api/v3/brokerage/orders)
     */
    fun generateRestToken(method: String, host: String, path: String): String {
        val uri = "$method $host$path"
        return generateToken(uri)
    }
    
    /**
     * Generate JWT for WebSocket connections
     * WebSocket JWTs don't need URI claim
     */
    fun generateWebSocketToken(): String {
        return generateToken(null)
    }
    
    private fun generateToken(uri: String?): String {
        val keyId = keyStore.getApiKeyId() 
            ?: throw IllegalStateException("API Key ID not configured")
        val privateKeyPem = keyStore.getPrivateKeyPem() 
            ?: throw IllegalStateException("Private Key not configured")
        
        val privateKey = loadEcPrivateKey(privateKeyPem)
        val now = Instant.now()
        val nonce = UUID.randomUUID().toString().replace("-", "")
        
        val headerBuilder = JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(JOSEObjectType.JWT)
            .keyID(keyId)
            .customParam("nonce", nonce)
        
        val claimsBuilder = JWTClaimsSet.Builder()
            .issuer("cdp")
            .subject(keyId)
            .notBeforeTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(120)))
        
        // Only add URI for REST calls
        if (uri != null) {
            claimsBuilder.claim("uri", uri)
        }
        
        val signedJwt = SignedJWT(headerBuilder.build(), claimsBuilder.build())
        signedJwt.sign(ECDSASigner(privateKey))
        
        return signedJwt.serialize()
    }
    
    private fun loadEcPrivateKey(pem: String): ECPrivateKey {
        // Remove PEM headers and whitespace
        val keyContent = pem
            .replace("-----BEGIN EC PRIVATE KEY-----", "")
            .replace("-----END EC PRIVATE KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        
        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        
        return keyFactory.generatePrivate(keySpec) as ECPrivateKey
    }
}
```

### 4.5 REST API Client

```kotlin
// data/remote/CoinbaseRestApi.kt
package com.engine.trade.data.remote

import android.util.Log
import com.engine.trade.data.security.JwtGenerator
import com.engine.trade.domain.model.Candle
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class CoinbaseRestApi(private val jwtGenerator: JwtGenerator) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private const val TAG = "CoinbaseRestApi"
        private const val BASE_URL = "https://api.coinbase.com"
        private const val HOST = "api.coinbase.com"
    }
    
    // ==================== ORDER PLACEMENT ====================
    
    /**
     * Place limit order with entry + attached TP/SL (for TREND mode)
     */
    fun placeBracketOrder(
        productId: String,
        side: String,
        baseSize: Double,
        entryPrice: Double,
        takeProfitPrice: Double,
        stopLossPrice: Double
    ): OrderResult {
        val clientOrderId = UUID.randomUUID().toString()
        
        val body = buildJsonObject {
            put("client_order_id", clientOrderId)
            put("product_id", productId)
            put("side", side)
            putJsonObject("order_configuration") {
                putJsonObject("limit_limit_gtc") {
                    put("base_size", formatSize(baseSize))
                    put("limit_price", formatPrice(entryPrice))
                }
            }
            putJsonObject("attached_order_configuration") {
                putJsonObject("trigger_bracket_gtc") {
                    put("limit_price", formatPrice(takeProfitPrice))
                    put("stop_trigger_price", formatPrice(stopLossPrice))
                }
            }
        }
        
        return executeCreateOrder(body, clientOrderId)
    }
    
    /**
     * Place limit order with post_only (for RANGE/grid mode)
     */
    fun placeLimitOrder(
        productId: String,
        side: String,
        baseSize: Double,
        limitPrice: Double
    ): OrderResult {
        val clientOrderId = UUID.randomUUID().toString()
        
        val body = buildJsonObject {
            put("client_order_id", clientOrderId)
            put("product_id", productId)
            put("side", side)
            putJsonObject("order_configuration") {
                putJsonObject("limit_limit_gtc") {
                    put("base_size", formatSize(baseSize))
                    put("limit_price", formatPrice(limitPrice))
                    put("post_only", true)  // Maker only - lower fees
                }
            }
        }
        
        return executeCreateOrder(body, clientOrderId)
    }
    
    /**
     * Place market order (for emergency liquidation)
     */
    fun placeMarketOrder(
        productId: String,
        side: String,
        baseSize: Double
    ): OrderResult {
        val clientOrderId = UUID.randomUUID().toString()
        
        val body = buildJsonObject {
            put("client_order_id", clientOrderId)
            put("product_id", productId)
            put("side", side)
            putJsonObject("order_configuration") {
                putJsonObject("market_market_ioc") {
                    put("base_size", formatSize(baseSize))
                }
            }
        }
        
        return executeCreateOrder(body, clientOrderId)
    }
    
    private fun executeCreateOrder(body: JsonObject, clientOrderId: String): OrderResult {
        val path = "/api/v3/brokerage/orders"
        val jwt = jwtGenerator.generateRestToken("POST", HOST, path)
        
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .addHeader("Authorization", "Bearer $jwt")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                parseOrderResponse(responseBody, clientOrderId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Order request failed", e)
            OrderResult.Failed(clientOrderId, null, "Network error: ${e.message}")
        }
    }
    
    private fun parseOrderResponse(responseBody: String?, clientOrderId: String): OrderResult {
        if (responseBody.isNullOrBlank()) {
            return OrderResult.Failed(clientOrderId, null, "Empty response")
        }
        
        return try {
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
            val success = jsonResponse["success"]?.jsonPrimitive?.booleanOrNull ?: false
            
            if (success) {
                val successData = jsonResponse["success_response"]?.jsonObject
                val orderId = successData?.get("order_id")?.jsonPrimitive?.content ?: "unknown"
                OrderResult.Success(clientOrderId, orderId)
            } else {
                val errorData = jsonResponse["error_response"]?.jsonObject
                val errorMsg = errorData?.get("message")?.jsonPrimitive?.content
                    ?: errorData?.get("error")?.jsonPrimitive?.content
                    ?: "Unknown error"
                OrderResult.Failed(clientOrderId, null, errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response: $responseBody", e)
            OrderResult.Failed(clientOrderId, null, "Parse error: ${e.message}")
        }
    }
    
    // ==================== ORDER MANAGEMENT ====================
    
    fun cancelOrders(orderIds: List<String>): Boolean {
        if (orderIds.isEmpty()) return true
        
        val path = "/api/v3/brokerage/orders/batch_cancel"
        val jwt = jwtGenerator.generateRestToken("POST", HOST, path)
        
        val body = buildJsonObject {
            putJsonArray("order_ids") { orderIds.forEach { add(it) } }
        }
        
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .addHeader("Authorization", "Bearer $jwt")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Cancel request failed", e)
            false
        }
    }
    
    fun getOpenOrders(productId: String? = null): List<CoinbaseOrder> {
        val path = "/api/v3/brokerage/orders/historical/batch"
        val query = buildString {
            append("?order_status=OPEN")
            productId?.let { append("&product_id=$it") }
        }
        
        val jwt = jwtGenerator.generateRestToken("GET", HOST, path)
        
        val request = Request.Builder()
            .url("$BASE_URL$path$query")
            .addHeader("Authorization", "Bearer $jwt")
            .get()
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                
                val body = response.body?.string() ?: return emptyList()
                val jsonResponse = json.parseToJsonElement(body).jsonObject
                val ordersArray = jsonResponse["orders"]?.jsonArray ?: return emptyList()
                
                ordersArray.map { parseOrder(it.jsonObject) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get orders failed", e)
            emptyList()
        }
    }
    
    // ==================== MARKET DATA ====================
    
    fun getCandles(
        productId: String,
        granularity: String = "TWO_HOUR",
        limit: Int = 350
    ): List<Candle> {
        val path = "/api/v3/brokerage/products/$productId/candles"
        val query = "?granularity=$granularity&limit=$limit"
        
        val jwt = jwtGenerator.generateRestToken("GET", HOST, path)
        
        val request = Request.Builder()
            .url("$BASE_URL$path$query")
            .addHeader("Authorization", "Bearer $jwt")
            .get()
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                
                val body = response.body?.string() ?: return emptyList()
                val jsonResponse = json.parseToJsonElement(body).jsonObject
                val candlesArray = jsonResponse["candles"]?.jsonArray ?: return emptyList()
                
                candlesArray.map { candleJson ->
                    val obj = candleJson.jsonObject
                    Candle(
                        timestamp = Instant.ofEpochSecond(
                            obj["start"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0
                        ),
                        open = obj["open"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        high = obj["high"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        low = obj["low"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        close = obj["close"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        volume = obj["volume"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    )
                }.sortedBy { it.timestamp }  // Ensure chronological order
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get candles failed", e)
            emptyList()
        }
    }
    
    fun getAccounts(): List<AccountBalance> {
        val path = "/api/v3/brokerage/accounts"
        val jwt = jwtGenerator.generateRestToken("GET", HOST, path)
        
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .addHeader("Authorization", "Bearer $jwt")
            .get()
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                
                val body = response.body?.string() ?: return emptyList()
                val jsonResponse = json.parseToJsonElement(body).jsonObject
                val accountsArray = jsonResponse["accounts"]?.jsonArray ?: return emptyList()
                
                accountsArray.map { accountJson ->
                    val obj = accountJson.jsonObject
                    val balanceObj = obj["available_balance"]?.jsonObject
                    AccountBalance(
                        currency = obj["currency"]?.jsonPrimitive?.content ?: "",
                        available = balanceObj?.get("value")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get accounts failed", e)
            emptyList()
        }
    }
    
    // ==================== HELPERS ====================
    
    private fun parseOrder(obj: JsonObject) = CoinbaseOrder(
        orderId = obj["order_id"]?.jsonPrimitive?.content ?: "",
        clientOrderId = obj["client_order_id"]?.jsonPrimitive?.content ?: "",
        productId = obj["product_id"]?.jsonPrimitive?.content ?: "",
        side = obj["side"]?.jsonPrimitive?.content ?: "",
        status = obj["status"]?.jsonPrimitive?.content ?: "",
        filledSize = obj["filled_size"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
        avgFilledPrice = obj["average_filled_price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
    )
    
    private fun formatSize(size: Double) = "%.8f".format(size)
    private fun formatPrice(price: Double) = "%.2f".format(price)
}

// Data classes
sealed class OrderResult {
    data class Success(val clientOrderId: String, val exchangeOrderId: String) : OrderResult()
    data class Failed(val clientOrderId: String, val exchangeOrderId: String?, val error: String) : OrderResult()
}

data class CoinbaseOrder(
    val orderId: String,
    val clientOrderId: String,
    val productId: String,
    val side: String,
    val status: String,
    val filledSize: Double,
    val avgFilledPrice: Double
)

data class AccountBalance(
    val currency: String,
    val available: Double
)
```

### 4.6 WebSocket Client

```kotlin
// data/remote/CoinbaseWebSocket.kt
package com.engine.trade.data.remote

import android.util.Log
import com.engine.trade.data.security.JwtGenerator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class CoinbaseWebSocket(
    private val jwtGenerator: JwtGenerator,
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)  // Auto heartbeat
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MINUTES)    // No timeout for WebSocket
        .build()
    
    private var marketSocket: WebSocket? = null
    private var userSocket: WebSocket? = null
    
    private val _tickerFlow = MutableSharedFlow<TickerUpdate>(replay = 1)
    val tickerFlow: SharedFlow<TickerUpdate> = _tickerFlow
    
    private val _orderFlow = MutableSharedFlow<OrderUpdate>(replay = 0, extraBufferCapacity = 64)
    val orderFlow: SharedFlow<OrderUpdate> = _orderFlow
    
    // Reconnection state
    private val isConnecting = AtomicBoolean(false)
    private var backoffMs = 5000L
    private val maxBackoffMs = 60000L
    
    // Health monitoring
    private val lastMessageTime = AtomicLong(System.currentTimeMillis())
    private var healthCheckJob: Job? = null
    
    companion object {
        private const val TAG = "CoinbaseWebSocket"
        private const val MARKET_WS_URL = "wss://advanced-trade-ws.coinbase.com"
        private const val USER_WS_URL = "wss://advanced-trade-ws-user.coinbase.com"
        private const val HEALTH_CHECK_INTERVAL_MS = 45_000L
    }
    
    // ==================== CONNECTION ====================
    
    fun connect(productIds: List<String>) {
        if (isConnecting.getAndSet(true)) return
        
        connectMarketData(productIds)
        connectUserData(productIds)
        startHealthCheck(productIds)
        
        isConnecting.set(false)
    }
    
    private fun connectMarketData(productIds: List<String>) {
        val request = Request.Builder().url(MARKET_WS_URL).build()
        
        marketSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Market WebSocket opened")
                backoffMs = 5000L
                
                // Subscribe to ticker and heartbeats
                subscribeChannel(webSocket, "heartbeats", productIds, auth = false)
                subscribeChannel(webSocket, "ticker", productIds, auth = false)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                lastMessageTime.set(System.currentTimeMillis())
                parseMarketMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Market WebSocket failed", t)
                scheduleReconnect { connectMarketData(productIds) }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Market WebSocket closed: $code $reason")
            }
        })
    }
    
    private fun connectUserData(productIds: List<String>) {
        val request = Request.Builder().url(USER_WS_URL).build()
        
        userSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "User WebSocket opened")
                
                // User channel requires authentication
                subscribeChannel(webSocket, "user", productIds, auth = true)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                lastMessageTime.set(System.currentTimeMillis())
                parseUserMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "User WebSocket failed", t)
                scheduleReconnect { connectUserData(productIds) }
            }
        })
    }
    
    private fun subscribeChannel(
        webSocket: WebSocket, 
        channel: String, 
        productIds: List<String>,
        auth: Boolean
    ) {
        val message = buildJsonObject {
            put("type", "subscribe")
            put("channel", channel)
            putJsonArray("product_ids") { productIds.forEach { add(it) } }
            if (auth) {
                put("jwt", jwtGenerator.generateWebSocketToken())
            }
        }
        webSocket.send(message.toString())
    }
    
    // ==================== HEALTH CHECK ====================
    
    private fun startHealthCheck(productIds: List<String>) {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (isActive) {
                delay(HEALTH_CHECK_INTERVAL_MS)
                
                val timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime.get()
                if (timeSinceLastMessage > HEALTH_CHECK_INTERVAL_MS) {
                    Log.w(TAG, "WebSocket appears dead (${timeSinceLastMessage}ms since last msg)")
                    disconnect()
                    delay(1000)
                    connect(productIds)
                }
            }
        }
    }
    
    // ==================== MESSAGE PARSING ====================
    
    private fun parseMarketMessage(text: String) {
        try {
            val jsonObj = Json.parseToJsonElement(text).jsonObject
            val channel = jsonObj["channel"]?.jsonPrimitive?.content ?: return
            
            when (channel) {
                "ticker" -> {
                    val events = jsonObj["events"]?.jsonArray ?: return
                    events.forEach { event ->
                        val tickers = event.jsonObject["tickers"]?.jsonArray ?: return@forEach
                        tickers.forEach { ticker ->
                            val obj = ticker.jsonObject
                            val update = TickerUpdate(
                                productId = obj["product_id"]?.jsonPrimitive?.content ?: "",
                                price = obj["price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                                bid = obj["best_bid"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                                ask = obj["best_ask"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                                volume24h = obj["volume_24_h"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                            )
                            scope.launch { _tickerFlow.emit(update) }
                        }
                    }
                }
                "heartbeats" -> {
                    // Connection alive - no action needed
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse market message", e)
        }
    }
    
    private fun parseUserMessage(text: String) {
        try {
            val jsonObj = Json.parseToJsonElement(text).jsonObject
            val channel = jsonObj["channel"]?.jsonPrimitive?.content ?: return
            
            if (channel == "user") {
                val events = jsonObj["events"]?.jsonArray ?: return
                events.forEach { event ->
                    val eventObj = event.jsonObject
                    val type = eventObj["type"]?.jsonPrimitive?.content
                    
                    val orders = eventObj["orders"]?.jsonArray ?: return@forEach
                    orders.forEach { order ->
                        val obj = order.jsonObject
                        val update = OrderUpdate(
                            orderId = obj["order_id"]?.jsonPrimitive?.content ?: "",
                            clientOrderId = obj["client_order_id"]?.jsonPrimitive?.content ?: "",
                            productId = obj["product_id"]?.jsonPrimitive?.content ?: "",
                            status = obj["status"]?.jsonPrimitive?.content ?: "",
                            side = obj["side"]?.jsonPrimitive?.content ?: "",
                            filledSize = obj["filled_size"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                            avgFilledPrice = obj["average_filled_price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                            eventType = type ?: "update"
                        )
                        scope.launch { _orderFlow.emit(update) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user message", e)
        }
    }
    
    // ==================== RECONNECTION ====================
    
    private fun scheduleReconnect(reconnectAction: () -> Unit) {
        scope.launch {
            Log.d(TAG, "Scheduling reconnect in ${backoffMs}ms")
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(maxBackoffMs)
            reconnectAction()
        }
    }
    
    fun disconnect() {
        healthCheckJob?.cancel()
        marketSocket?.close(1000, "Client disconnect")
        userSocket?.close(1000, "Client disconnect")
        marketSocket = null
        userSocket = null
    }
}

data class TickerUpdate(
    val productId: String,
    val price: Double,
    val bid: Double,
    val ask: Double,
    val volume24h: Double
)

data class OrderUpdate(
    val orderId: String,
    val clientOrderId: String,
    val productId: String,
    val status: String,
    val side: String,
    val filledSize: Double,
    val avgFilledPrice: Double,
    val eventType: String
)
```

### 4.7 Room Database

```kotlin
// data/local/EngineDatabase.kt
package com.engine.trade.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [OrderEntity::class, PortfolioSnapshot::class, GridConfig::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EngineDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun gridDao(): GridDao
    
    companion object {
        @Volatile private var INSTANCE: EngineDatabase? = null
        
        fun getInstance(context: Context): EngineDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    EngineDatabase::class.java,
                    "engine_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class Converters {
    @TypeConverter fun fromTimestamp(value: Long?) = value?.let { java.time.Instant.ofEpochMilli(it) }
    @TypeConverter fun toTimestamp(instant: java.time.Instant?) = instant?.toEpochMilli()
}

// ==================== ENTITIES ====================

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["exchange_order_id"], unique = true),
        Index(value = ["status"]),
        Index(value = ["product_id", "grid_level"])
    ]
)
data class OrderEntity(
    @PrimaryKey val clientOrderId: String,
    val exchangeOrderId: String? = null,
    val productId: String,
    val side: String,
    val orderType: String,  // BRACKET, LIMIT, MARKET
    val price: Double,
    val size: Double,
    val status: String,  // PENDING, OPEN, FILLED, CANCELLED, FAILED
    val gridLevel: Int? = null,
    val filledSize: Double = 0.0,
    val avgFilledPrice: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalEquityUsd: Double,
    val cashUsd: Double,
    val btcValue: Double,
    val highWaterMark: Double,
    val drawdownPercent: Double,
    val regime: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "grid_configs")
data class GridConfig(
    @PrimaryKey val productId: String,
    val isActive: Boolean = false,
    val regime: String = "WAIT",
    val spacing: Double = 0.0,
    val lastAtr: Double = 0.0,
    val lastSma: Double = 0.0,
    val lastAdx: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

// ==================== DAOs ====================

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)
    
    @Update
    suspend fun update(order: OrderEntity)
    
    @Query("SELECT * FROM orders WHERE clientOrderId = :clientOrderId")
    suspend fun getByClientId(clientOrderId: String): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE exchange_order_id = :exchangeOrderId")
    suspend fun getByExchangeId(exchangeOrderId: String): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'OPEN')")
    suspend fun getActiveOrders(): List<OrderEntity>
    
    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'OPEN') AND product_id = :productId")
    suspend fun getActiveOrdersForProduct(productId: String): List<OrderEntity>
    
    @Query("UPDATE orders SET status = :status, updatedAt = :now WHERE clientOrderId = :clientOrderId")
    suspend fun updateStatus(clientOrderId: String, status: String, now: Long = System.currentTimeMillis())
    
    @Query("UPDATE orders SET exchange_order_id = :exchangeId, status = :status, updatedAt = :now WHERE clientOrderId = :clientOrderId")
    suspend fun confirmOrder(clientOrderId: String, exchangeId: String, status: String, now: Long = System.currentTimeMillis())
    
    @Query("UPDATE orders SET filledSize = :filled, avgFilledPrice = :price, status = :status, updatedAt = :now WHERE clientOrderId = :clientOrderId")
    suspend fun updateFill(clientOrderId: String, filled: Double, price: Double, status: String, now: Long = System.currentTimeMillis())
}

@Dao
interface PortfolioDao {
    @Insert
    suspend fun insert(snapshot: PortfolioSnapshot)
    
    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): PortfolioSnapshot?
    
    @Query("SELECT MAX(highWaterMark) FROM portfolio_snapshots")
    suspend fun getHighWaterMark(): Double?
    
    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<PortfolioSnapshot>>
}

@Dao
interface GridDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: GridConfig)
    
    @Query("SELECT * FROM grid_configs WHERE productId = :productId")
    suspend fun get(productId: String): GridConfig?
    
    @Query("SELECT * FROM grid_configs WHERE isActive = 1")
    fun observeActive(): Flow<List<GridConfig>>
}
```

### 4.8 Trading Service

```kotlin
// service/TradingService.kt
package com.engine.trade.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.engine.trade.MainActivity
import com.engine.trade.R
import com.engine.trade.data.local.*
import com.engine.trade.data.remote.*
import com.engine.trade.data.security.JwtGenerator
import com.engine.trade.data.security.SecureKeyStore
import com.engine.trade.domain.model.Decision
import com.engine.trade.domain.strategy.EngineDecisionEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.atomic.AtomicReference

class TradingService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    
    private lateinit var keyStore: SecureKeyStore
    private lateinit var jwtGenerator: JwtGenerator
    private lateinit var restApi: CoinbaseRestApi
    private lateinit var webSocket: CoinbaseWebSocket
    private lateinit var database: EngineDatabase
    private lateinit var decisionEngine: EngineDecisionEngine
    
    private val currentPrice = AtomicReference(0.0)
    private val currentDecision = AtomicReference<Decision>(Decision.Wait("Starting..."))
    
    companion object {
        private const val TAG = "TradingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "engine_trading"
        
        private const val PRODUCT_ID = "BTC-USD"
        private const val STRATEGY_INTERVAL_MS = 15 * 60 * 1000L  // 15 minutes
        private const val POSITION_SIZE_PERCENT = 0.05  // 5% per trade
        private const val DRAWDOWN_LIMIT = 0.15  // 15%
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service creating")
        
        // Initialize components
        keyStore = SecureKeyStore(this)
        jwtGenerator = JwtGenerator(keyStore)
        restApi = CoinbaseRestApi(jwtGenerator)
        webSocket = CoinbaseWebSocket(jwtGenerator, serviceScope)
        database = EngineDatabase.getInstance(this)
        decisionEngine = EngineDecisionEngine()
        
        // Acquire wake lock
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Engine::TradingService"
        ).apply { acquire() }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service starting")
        
        startForeground(NOTIFICATION_ID, createNotification("Initializing..."))
        
        if (!keyStore.hasCredentials()) {
            updateNotification("⚠️ No API credentials")
            return START_NOT_STICKY
        }
        
        // Launch async tasks
        serviceScope.launch { reconcileOrders() }
        serviceScope.launch { runPriceMonitor() }
        serviceScope.launch { runOrderMonitor() }
        serviceScope.launch { runStrategyLoop() }
        
        return START_STICKY
    }
    
    // ==================== CORE LOOPS ====================
    
    private suspend fun reconcileOrders() {
        Log.d(TAG, "Reconciling orders...")
        
        val localOrders = database.orderDao().getActiveOrders()
        if (localOrders.isEmpty()) return
        
        val exchangeOrders = restApi.getOpenOrders(PRODUCT_ID)
        val exchangeMap = exchangeOrders.associateBy { it.orderId }
        
        localOrders.forEach { local ->
            when {
                // Order on exchange - update local state
                local.exchangeOrderId != null && exchangeMap.containsKey(local.exchangeOrderId) -> {
                    val remote = exchangeMap[local.exchangeOrderId]!!
                    if (remote.status != local.status || remote.filledSize != local.filledSize) {
                        database.orderDao().updateFill(
                            local.clientOrderId,
                            remote.filledSize,
                            remote.avgFilledPrice,
                            remote.status
                        )
                    }
                }
                // Order never confirmed - mark failed
                local.status == "PENDING" -> {
                    database.orderDao().updateStatus(local.clientOrderId, "FAILED")
                }
                // Order was filled/cancelled while offline
                else -> {
                    database.orderDao().updateStatus(local.clientOrderId, "UNKNOWN")
                }
            }
        }
        
        Log.d(TAG, "Reconciliation complete")
    }
    
    private suspend fun runPriceMonitor() {
        Log.d(TAG, "Starting price monitor")
        
        webSocket.connect(listOf(PRODUCT_ID))
        
        webSocket.tickerFlow.collectLatest { ticker ->
            if (ticker.productId == PRODUCT_ID) {
                currentPrice.set(ticker.price)
            }
        }
    }
    
    private suspend fun runOrderMonitor() {
        Log.d(TAG, "Starting order monitor")
        
        webSocket.orderFlow.collectLatest { update ->
            Log.d(TAG, "Order update: ${update.orderId} -> ${update.status}")
            
            // Find by exchange ID or client ID
            val order = database.orderDao().getByExchangeId(update.orderId)
                ?: database.orderDao().getByClientId(update.clientOrderId)
            
            order?.let {
                database.orderDao().updateFill(
                    it.clientOrderId,
                    update.filledSize,
                    update.avgFilledPrice,
                    update.status
                )
            }
        }
    }
    
    private suspend fun runStrategyLoop() {
        Log.d(TAG, "Starting strategy loop")
        
        while (isActive) {
            try {
                // 1. Get candles (H4 = aggregate TWO_HOUR)
                val twoHourCandles = restApi.getCandles(PRODUCT_ID, "TWO_HOUR", 350)
                
                // Aggregate to H4
                val h4Candles = aggregateToH4(twoHourCandles)
                
                if (h4Candles.size < 200) {
                    Log.w(TAG, "Not enough candles: ${h4Candles.size}/200")
                    delay(STRATEGY_INTERVAL_MS)
                    continue
                }
                
                val price = currentPrice.get()
                if (price <= 0) {
                    Log.w(TAG, "No price data yet")
                    delay(STRATEGY_INTERVAL_MS)
                    continue
                }
                
                // 2. Evaluate strategy
                val decision = decisionEngine.evaluate(h4Candles, price)
                currentDecision.set(decision)
                Log.d(TAG, "Decision: $decision")
                
                // 3. Execute
                executeDecision(decision, price)
                
                // 4. Check drawdown
                checkDrawdown(price)
                
                // 5. Update notification
                updateNotification("${decision::class.simpleName} | $${"%.0f".format(price)}")
                
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Strategy loop error", e)
            }
            
            delay(STRATEGY_INTERVAL_MS)
        }
    }
    
    private fun aggregateToH4(twoHourCandles: List<com.engine.trade.domain.model.Candle>): List<com.engine.trade.domain.model.Candle> {
        // Group consecutive pairs of 2-hour candles into 4-hour candles
        return twoHourCandles.chunked(2).mapNotNull { pair ->
            if (pair.size < 2) return@mapNotNull null
            com.engine.trade.domain.model.Candle(
                timestamp = pair[0].timestamp,
                open = pair[0].open,
                high = maxOf(pair[0].high, pair[1].high),
                low = minOf(pair[0].low, pair[1].low),
                close = pair[1].close,
                volume = pair[0].volume + pair[1].volume
            )
        }
    }
    
    // ==================== EXECUTION ====================
    
    private suspend fun executeDecision(decision: Decision, price: Double) {
        when (decision) {
            is Decision.Defense -> executeDefense()
            is Decision.Trend -> executeTrend(decision, price)
            is Decision.Range -> executeRange(decision, price)
            is Decision.Wait -> { /* Hold current state */ }
        }
    }
    
    private suspend fun executeDefense() {
        // Cancel all open buy orders
        val activeOrders = database.orderDao().getActiveOrdersForProduct(PRODUCT_ID)
        val buyOrders = activeOrders.filter { it.side == "BUY" }
        
        if (buyOrders.isNotEmpty()) {
            val orderIds = buyOrders.mapNotNull { it.exchangeOrderId }
            if (restApi.cancelOrders(orderIds)) {
                buyOrders.forEach {
                    database.orderDao().updateStatus(it.clientOrderId, "CANCELLED")
                }
            }
        }
    }
    
    private suspend fun executeTrend(decision: Decision.Trend, price: Double) {
        // Check if already in position
        val activeOrders = database.orderDao().getActiveOrdersForProduct(PRODUCT_ID)
        if (activeOrders.any { it.orderType == "BRACKET" && it.status == "OPEN" }) {
            return  // Already have a trend position
        }
        
        val size = calculatePositionSize(price)
        val result = restApi.placeBracketOrder(
            productId = PRODUCT_ID,
            side = "BUY",
            baseSize = size,
            entryPrice = price,
            takeProfitPrice = decision.takeProfitPrice,
            stopLossPrice = decision.stopLossPrice
        )
        
        handleOrderResult(result, "BRACKET", price, size)
    }
    
    private suspend fun executeRange(decision: Decision.Range, price: Double) {
        val activeOrders = database.orderDao().getActiveOrdersForProduct(PRODUCT_ID)
        val existingLevels = activeOrders.mapNotNull { it.gridLevel }.toSet()
        
        // Place grid orders at levels 1-5 below current price
        for (level in 1..5) {
            if (level in existingLevels) continue
            
            val gridPrice = price * (1 - (level * decision.gridSpacing / price))
            val size = calculateGridSize(price)
            
            val result = restApi.placeLimitOrder(
                productId = PRODUCT_ID,
                side = "BUY",
                baseSize = size,
                limitPrice = gridPrice
            )
            
            handleOrderResult(result, "LIMIT", gridPrice, size, level)
        }
    }
    
    private suspend fun handleOrderResult(
        result: OrderResult,
        type: String,
        price: Double,
        size: Double,
        gridLevel: Int? = null
    ) {
        when (result) {
            is OrderResult.Success -> {
                database.orderDao().insert(OrderEntity(
                    clientOrderId = result.clientOrderId,
                    exchangeOrderId = result.exchangeOrderId,
                    productId = PRODUCT_ID,
                    side = "BUY",
                    orderType = type,
                    price = price,
                    size = size,
                    status = "OPEN",
                    gridLevel = gridLevel
                ))
                Log.d(TAG, "Order placed: ${result.exchangeOrderId}")
            }
            is OrderResult.Failed -> {
                Log.e(TAG, "Order failed: ${result.error}")
            }
        }
    }
    
    // ==================== RISK MANAGEMENT ====================
    
    private suspend fun checkDrawdown(price: Double) {
        val accounts = restApi.getAccounts()
        val usd = accounts.find { it.currency == "USD" }?.available ?: 0.0
        val btc = accounts.find { it.currency == "BTC" }?.available ?: 0.0
        
        val totalEquity = usd + (btc * price)
        val hwm = database.portfolioDao().getHighWaterMark() ?: totalEquity
        val newHwm = maxOf(hwm, totalEquity)
        val drawdown = if (newHwm > 0) (newHwm - totalEquity) / newHwm else 0.0
        
        // Save snapshot
        database.portfolioDao().insert(PortfolioSnapshot(
            totalEquityUsd = totalEquity,
            cashUsd = usd,
            btcValue = btc * price,
            highWaterMark = newHwm,
            drawdownPercent = drawdown * 100,
            regime = currentDecision.get()::class.simpleName ?: "UNKNOWN"
        ))
        
        // Emergency liquidation
        if (drawdown > DRAWDOWN_LIMIT) {
            Log.e(TAG, "🚨 DRAWDOWN LIMIT HIT: ${"%.1f".format(drawdown * 100)}%")
            emergencyLiquidate(btc)
            stopSelf()
        }
    }
    
    private suspend fun emergencyLiquidate(btcBalance: Double) {
        // Cancel all orders
        val allOrders = database.orderDao().getActiveOrders()
        val orderIds = allOrders.mapNotNull { it.exchangeOrderId }
        restApi.cancelOrders(orderIds)
        
        // Market sell all BTC
        if (btcBalance > 0.0001) {
            restApi.placeMarketOrder(PRODUCT_ID, "SELL", btcBalance)
        }
        
        updateNotification("🛑 EMERGENCY STOP - Drawdown limit")
    }
    
    private suspend fun calculatePositionSize(price: Double): Double {
        val accounts = restApi.getAccounts()
        val usd = accounts.find { it.currency == "USD" }?.available ?: 0.0
        val btc = accounts.find { it.currency == "BTC" }?.available ?: 0.0
        val total = usd + (btc * price)
        
        val positionUsd = total * POSITION_SIZE_PERCENT
        return positionUsd / price
    }
    
    private suspend fun calculateGridSize(price: Double): Double {
        // 2% per grid level (5 levels = 10% max)
        return calculatePositionSize(price) * 0.4
    }
    
    // ==================== NOTIFICATION ====================
    
    private fun createNotification(content: String): Notification {
        createChannel()
        
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Engine Trading")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(content))
    }
    
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Trading Status", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Engine trading bot status" }
        
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    
    // ==================== LIFECYCLE ====================
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroying")
        webSocket.disconnect()
        wakeLock?.release()
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
```

---

## Part 5: Gradle Dependencies

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.engine.trade"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.engine.trade"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // OkHttp (HTTP + WebSocket)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JWT
    implementation("com.nimbusds:nimbus-jose-jwt:9.37")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Technical Analysis
    implementation("org.ta4j:ta4j-core:0.15")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

---

## Part 6: Manifest

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <application
        android:name=".EngineApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="Engine"
        android:theme="@style/Theme.Engine">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <service
            android:name=".service.TradingService"
            android:foregroundServiceType="dataSync"
            android:exported="false" />
            
    </application>
</manifest>
```

---

## Part 7: Testing Checklist

Since Coinbase sandbox is static-only, use these approaches:

### Unit Tests
- [ ] Decision engine produces correct modes for test data
- [ ] Hysteresis requires 3 confirmations
- [ ] Defense mode activates instantly (no hysteresis)
- [ ] Grid spacing respects 1.5% minimum
- [ ] ATR/SMA/ADX calculations match reference values

### Integration Tests (Small Real Trades)
- [ ] JWT generation produces valid tokens
- [ ] REST API authentication succeeds
- [ ] Can create and cancel orders
- [ ] WebSocket connects and receives ticker
- [ ] Order updates flow through correctly

### System Tests
- [ ] Service survives device sleep (Doze)
- [ ] Service restarts after force stop (START_STICKY)
- [ ] Drawdown calculation triggers at 15%
- [ ] Emergency liquidation sells all BTC

---

## Document History

| Version | Changes |
|---------|---------|
| 1.0 | Initial Gemini draft |
| 1.1 | Claude corrections (hallucinations fixed) |
| 2.0 | Full rewrite with Coinbase research |
| **2.1** | **Integrated all MCP research findings, added complete code** |

---

## Key Research Findings Integrated

1. **Rate Limits:** 10,000 REST requests/hour, 750 WS connections/second
2. **Fees:** 0.60% maker at intro tier → 1.5% minimum grid spacing
3. **JWT:** 2-minute expiry, ES256 algorithm, nonce required
4. **WebSocket:** Must subscribe to heartbeats, 60-90 second timeout
5. **Candles:** Max 350/request, use TWO_HOUR and aggregate for H4
6. **Sandbox:** Static responses only - cannot use for paper trading
7. **Bracket Orders:** `limit_price` = TP, `stop_trigger_price` = SL
8. **Battery:** OkHttp over Ktor (10x better), battery exemption critical
9. **Security:** EncryptedSharedPreferences, trade-only permissions
