# Polymarket API Integration Plan

**Status:** 🔮 FUTURE ENHANCEMENT - Implement AFTER Phase 0A complete
**Priority:** Phase 2 or Phase 3 (after Coinbase integration proven)
**Complexity:** Medium (new exchange module)
**Risk:** Medium (prediction market different from spot trading)

---

## 📋 Executive Summary

**Goal:** Add Polymarket prediction market trading as a second strategy alongside Coinbase spot trading.

**Opportunity:** Exploit cross-market arbitrage (like the 0x8dxd bot) by comparing crypto spot prices on Coinbase with Polymarket prediction odds.

**Architecture Impact:** New exchange module (`:exchange:polymarket`) that implements existing domain interfaces.

**Timeline:** Implement AFTER Coinbase integration is proven profitable for 30+ days.

---

## 🎯 Why Polymarket Integration?

### The Arbitrage Opportunity

```
┌─────────────────────────────────────────────────────────┐
│  Cross-Market Arbitrage Strategy                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. Monitor: BTC spot price on Coinbase               │
│     └─> Current price: $42,150                        │
│                                                         │
│  2. Compare: Polymarket odds for "BTC above $42k"     │
│     └─> Current odds: 0.52 (should be 0.99+)         │
│                                                         │
│  3. Execute: Buy "YES" shares if mispriced            │
│     └─> Guaranteed profit when Polymarket updates     │
│                                                         │
│  4. Exit: Sell when odds correct to fair value       │
│     └─> Low-risk profit from market inefficiency      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Advantages Over Spot Trading

| Aspect | Coinbase Spot | Polymarket Prediction |
|--------|---------------|----------------------|
| **Edge Type** | Predictive (hard) | Arbitrage (easier) |
| **Win Rate** | 52-58% realistic | 70-90% possible |
| **Capital Risk** | High (directional) | Lower (bounded loss) |
| **Fees** | 0.25-0.5% per trade | 0.5-1% per trade |
| **Regulation** | Clear | Uncertain (US restricted) |

### Risks & Constraints

⚠️ **Critical Risks:**
1. **Legal:** Polymarket restricted in US (requires VPN or offshore entity)
2. **Liquidity:** Lower volume than Coinbase (slippage on large orders)
3. **Market Efficiency:** Arbitrage opportunities may already be closed
4. **Platform Risk:** Centralized prediction market (can halt trading)

---

## 🏗️ Technical Architecture

### Official API Resources

**CLOB API (Central Limit Order Book):**
- REST API: `https://clob.polymarket.com`
- WebSocket: Real-time price feeds, order book updates
- Authentication: HMAC-SHA256 signature + API keys
- Rate Limits: 100 requests/minute (free tier)

**Official Clients:**
- Python: [py-clob-client](https://github.com/Polymarket/py-clob-client)
- TypeScript: [real-time-data-client](https://github.com/Polymarket/real-time-data-client)
- Rust: [rs-clob-client](https://github.com/Polymarket/rs-clob-client)

### TradeFlow Integration Pattern

```kotlin
// New module: :exchange:polymarket
exchange/
├── coinbase/                   ← Existing
│   ├── CoinbaseRepository.kt
│   └── CoinbaseJwtGenerator.kt
└── polymarket/                 ← NEW
    ├── PolymarketRepository.kt
    ├── PolymarketAuthProvider.kt
    ├── PolymarketWebSocket.kt
    └── model/
        ├── Market.kt
        ├── Position.kt
        └── Outcome.kt
```

### Domain Interface Compatibility

**Existing interfaces CAN be reused:**

```kotlin
// core/domain/repository/ExchangeRepository.kt
interface ExchangeRepository {
    suspend fun getBalance(): Result<Portfolio>
    suspend fun placeOrder(order: Order): Result<OrderId>
    suspend fun cancelOrder(orderId: OrderId): Result<Unit>
    // ... existing methods work for Polymarket too
}

// Polymarket implements same interface
class PolymarketRepository @Inject constructor(
    private val clobClient: PolymarketClobClient
) : ExchangeRepository {
    override suspend fun placeOrder(order: Order): Result<OrderId> {
        // Map TradeFlow Order -> Polymarket CLOB order
        val polyOrder = order.toPolymarketOrder()
        return clobClient.createOrder(polyOrder)
    }
}
```

**New domain types needed:**

```kotlin
// core/domain/model/PredictionMarket.kt
data class PredictionMarket(
    val id: String,
    val question: String,
    val outcomes: List<Outcome>,
    val endDate: Instant,
    val volume: BigDecimal
)

data class Outcome(
    val id: String,
    val label: String,       // "YES" or "NO"
    val price: BigDecimal,   // Current odds (0.0 to 1.0)
    val liquidity: BigDecimal
)

// core/domain/model/ArbitrageOpportunity.kt
data class ArbitrageOpportunity(
    val market: PredictionMarket,
    val outcome: Outcome,
    val spotPrice: BigDecimal,        // BTC price on Coinbase
    val predictionOdds: BigDecimal,   // Polymarket odds
    val expectedValue: BigDecimal,    // Profit potential
    val confidence: Double            // How certain is arbitrage
)
```

---

## 🔧 Implementation Plan

### Phase 1: Research & Validation (1-2 weeks)

**Goal:** Confirm arbitrage opportunities still exist

```kotlin
// Proof-of-concept: Monitor discrepancies
class ArbitrageMonitor(
    private val coinbaseRepo: ExchangeRepository,
    private val polymarketApi: PolymarketClobClient
) {
    suspend fun findOpportunities(): List<ArbitrageOpportunity> {
        // 1. Get BTC price from Coinbase
        val btcPrice = coinbaseRepo.getCurrentPrice("BTC-USD")

        // 2. Get Polymarket odds for "BTC above $X"
        val markets = polymarketApi.getMarkets(query = "bitcoin")

        // 3. Find mispriced markets
        return markets.mapNotNull { market ->
            val expectedOdds = calculateExpectedOdds(btcPrice, market)
            val actualOdds = market.outcomes.find { it.label == "YES" }?.price

            if (actualOdds != null && expectedOdds - actualOdds > 0.10) {
                ArbitrageOpportunity(
                    market = market,
                    outcome = market.outcomes.first(),
                    spotPrice = btcPrice,
                    predictionOdds = actualOdds,
                    expectedValue = expectedOdds - actualOdds,
                    confidence = 0.95
                )
            } else null
        }
    }
}
```

**Deliverables:**
- [ ] Monitor script that logs discrepancies for 7 days
- [ ] Analysis: How often do arbitrage opportunities appear?
- [ ] Analysis: How long do they last? (seconds? minutes?)
- [ ] Decision: Is edge real or already closed?

### Phase 2: Infrastructure (2-3 weeks)

**Goal:** Build Polymarket exchange module

**Tasks:**
```
1. Create :exchange:polymarket module
   ├── build.gradle.kts (add Ktor, kotlinx-serialization)
   └── src/main/kotlin/com/tradeflow/exchange/polymarket/

2. Implement authentication
   ├── PolymarketAuthProvider.kt (HMAC-SHA256 signing)
   └── AuthModule.kt (DI binding)

3. Implement REST client
   ├── PolymarketClobClient.kt (order placement, cancellation)
   └── model/ (DTOs for markets, orders, positions)

4. Implement WebSocket client
   ├── PolymarketWebSocket.kt (real-time price feeds)
   └── PolymarketWebSocketModule.kt (DI)

5. Implement ExchangeRepository
   └── PolymarketRepository.kt (maps domain types to Polymarket API)

6. Add tests
   ├── AuthProviderTest.kt
   ├── ClobClientTest.kt
   └── RepositoryTest.kt
```

**Code Example:**

```kotlin
// exchange/polymarket/src/main/kotlin/auth/PolymarketAuthProvider.kt
class PolymarketAuthProvider @Inject constructor(
    private val credentialStore: CredentialStore
) : AuthTokenProvider {

    override suspend fun generateToken(request: String): Result<String> {
        val apiKey = credentialStore.getApiKey() ?: return Result.failure(...)
        val apiSecret = credentialStore.getApiSecret() ?: return Result.failure(...)

        val timestamp = Clock.System.now().epochSeconds
        val payload = "$timestamp$request"

        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(SecretKeySpec(apiSecret.toByteArray(), "HmacSHA256"))
        val signature = hmac.doFinal(payload.toByteArray())

        return Result.success(signature.encodeBase64())
    }
}

// exchange/polymarket/src/main/kotlin/PolymarketRepository.kt
class PolymarketRepository @Inject constructor(
    private val clobClient: PolymarketClobClient,
    private val authProvider: AuthTokenProvider
) : ExchangeRepository {

    override suspend fun placeOrder(order: Order): Result<OrderId> {
        return try {
            val signature = authProvider.generateToken(order.toString()).getOrThrow()
            val response = clobClient.createOrder(
                marketId = order.symbol,
                side = if (order.side == OrderSide.BUY) "BUY" else "SELL",
                size = order.quantity.toString(),
                price = order.price?.toString(),
                signature = signature
            )
            Result.success(OrderId(response.orderId))
        } catch (e: Exception) {
            Result.failure(ExchangeError.OrderPlacementFailed(e.message))
        }
    }

    override suspend fun getMarkets(): Result<List<PredictionMarket>> {
        // Implementation details...
    }
}
```

### Phase 3: Arbitrage Strategy Engine (1-2 weeks)

**Goal:** Automated arbitrage detection and execution

```kotlin
// app/src/main/kotlin/strategy/ArbitrageEngine.kt
class ArbitrageEngine @Inject constructor(
    private val coinbaseRepo: ExchangeRepository,
    private val polymarketRepo: PolymarketRepository,
    private val riskManager: RiskManager
) {

    suspend fun execute() {
        // 1. Get spot price
        val btcPrice = coinbaseRepo.getCurrentPrice("BTC-USD").getOrNull() ?: return

        // 2. Find arbitrage opportunities
        val opportunities = findArbitrageOpportunities(btcPrice)

        // 3. Execute highest-confidence trades
        opportunities
            .filter { it.confidence > 0.90 }
            .sortedByDescending { it.expectedValue }
            .take(3)  // Max 3 concurrent positions
            .forEach { opportunity ->
                if (riskManager.canTrade(opportunity)) {
                    polymarketRepo.placeOrder(opportunity.toOrder())
                }
            }
    }

    private suspend fun findArbitrageOpportunities(
        spotPrice: BigDecimal
    ): List<ArbitrageOpportunity> {
        val markets = polymarketRepo.getMarkets().getOrNull() ?: return emptyList()

        return markets.mapNotNull { market ->
            // Example: "Will BTC be above $42,000 on Jan 8?"
            val threshold = extractPriceThreshold(market.question) ?: return@mapNotNull null
            val outcome = market.outcomes.find { it.label == "YES" } ?: return@mapNotNull null

            // Calculate expected odds based on spot price
            val expectedOdds = when {
                spotPrice > threshold -> 0.99  // Already above threshold
                spotPrice < threshold * 0.95 -> 0.01  // Far below
                else -> 0.50  // Uncertain
            }

            // Check for mispricing (arbitrage opportunity)
            val discrepancy = expectedOdds - outcome.price.toDouble()
            if (discrepancy > 0.15) {  // 15%+ edge
                ArbitrageOpportunity(
                    market = market,
                    outcome = outcome,
                    spotPrice = spotPrice,
                    predictionOdds = outcome.price,
                    expectedValue = discrepancy.toBigDecimal(),
                    confidence = 0.95
                )
            } else null
        }
    }
}
```

### Phase 4: Risk Management & Monitoring (1 week)

**Goal:** Prevent catastrophic losses

```kotlin
// core/domain/service/PolymarketRiskManager.kt
class PolymarketRiskManager @Inject constructor(
    private val portfolioRepo: PortfolioRepository
) {
    private val limits = RiskLimits(
        maxPositionSize = "50".toBigDecimal(),      // $50 per market
        maxConcurrentPositions = 5,                  // Max 5 open markets
        maxDailyLoss = "100".toBigDecimal(),        // Stop at -$100/day
        minConfidence = 0.85                         // Only high-conviction
    )

    fun canTrade(opportunity: ArbitrageOpportunity): Boolean {
        val portfolio = portfolioRepo.getCurrent()

        return listOf(
            opportunity.confidence >= limits.minConfidence,
            portfolio.openPositions.size < limits.maxConcurrentPositions,
            portfolio.dailyLoss < limits.maxDailyLoss,
            opportunity.expectedValue * 100 <= limits.maxPositionSize
        ).all { it }
    }
}
```

---

## 📊 Success Metrics

### Phase 1 Validation (Research)

```
✅ Success Criteria:
- Found 10+ arbitrage opportunities in 7 days
- Average edge: 10%+ discrepancy
- Opportunities last 30+ seconds (enough time to execute)

❌ Failure Criteria:
- < 5 opportunities in 7 days
- Edge < 5% (not enough to cover fees)
- Opportunities close in < 10 seconds (too fast)
```

### Phase 2-4 Implementation

```
✅ Success Criteria (30-day live test):
- Win rate: 70%+ (arbitrage should be high)
- Average return per trade: 5-10%
- Max drawdown: < 10%
- No API errors or downtime

❌ Failure Criteria:
- Win rate: < 60% (edge doesn't exist)
- Average return: < 3% (fees eating profit)
- Max drawdown: > 20%
- Frequent API failures
```

---

## 🗓️ Timeline & Dependencies

### Prerequisites (MUST complete first)

```
✅ Phase 0A: Domain models, database, JWT auth
✅ Phase 0B: Decision engine, risk manager
✅ Phase 1: Coinbase integration PROVEN PROFITABLE
   └─> 30+ days of live trading with positive returns
```

### Estimated Timeline

```
Week 1-2:  Research arbitrage opportunities (Phase 1)
Week 3-5:  Build Polymarket module (Phase 2)
Week 6-7:  Implement arbitrage engine (Phase 3)
Week 8:    Risk management + testing (Phase 4)
Week 9-12: Live testing with small capital ($100-500)
```

**Total:** ~3 months AFTER Coinbase integration is complete

---

## 💰 Cost-Benefit Analysis

### Development Cost

```
Time: ~3 months (part-time)
Risk: Medium (new platform, uncertain regulation)
Complexity: Medium (similar to Coinbase module)
```

### Expected Returns

```
Conservative:
- 10 arbitrage trades/week @ 7% avg edge
- Starting capital: $500
- Expected weekly profit: $35-50
- Annual return: 40-60%

Optimistic:
- 30 arbitrage trades/week @ 10% avg edge
- Starting capital: $1000
- Expected weekly profit: $150-300
- Annual return: 100%+

Reality Check:
- Edge may not exist or decay quickly
- Requires constant monitoring
- Legal risks in US
```

### Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| No arbitrage edge | Medium | High | Phase 1 validation first |
| Legal issues (US) | High | High | Use VPN or offshore entity |
| Low liquidity | Medium | Medium | Start small ($100-500) |
| Platform shutdown | Low | High | Diversify across exchanges |
| API rate limits | Medium | Low | Implement backoff logic |

---

## 🚀 Implementation Checklist

### Before Starting

- [ ] Phase 0A complete (domain models, database, Coinbase auth)
- [ ] Phase 0B complete (decision engine, risk manager)
- [ ] Coinbase integration proven profitable for 30+ days
- [ ] User has Polymarket account + API keys
- [ ] Legal review complete (US restrictions understood)

### Phase 1: Validation

- [ ] Create monitoring script (ArbitrageMonitor.kt)
- [ ] Run for 7 days, log all opportunities
- [ ] Analyze: Frequency, edge size, duration
- [ ] Decision: GO/NO-GO based on data

### Phase 2: Infrastructure

- [ ] Create `:exchange:polymarket` module
- [ ] Implement PolymarketAuthProvider (HMAC-SHA256)
- [ ] Implement PolymarketClobClient (REST API)
- [ ] Implement PolymarketWebSocket (real-time feeds)
- [ ] Implement PolymarketRepository (ExchangeRepository interface)
- [ ] Write unit tests (80%+ coverage)

### Phase 3: Strategy Engine

- [ ] Implement ArbitrageEngine.kt
- [ ] Add opportunity detection logic
- [ ] Add order execution logic
- [ ] Integrate with existing DI system

### Phase 4: Risk & Monitoring

- [ ] Implement PolymarketRiskManager
- [ ] Add position size limits
- [ ] Add daily loss limits
- [ ] Add execution speed monitoring
- [ ] Create dashboard view (optional)

### Phase 5: Live Testing

- [ ] Start with $100 capital
- [ ] Monitor for 30 days
- [ ] Track: win rate, returns, drawdown
- [ ] Decision: Scale up or shut down

---

## 📚 Resources

### Official Documentation
- [Polymarket Documentation](https://docs.polymarket.com/)
- [Gamma Markets API Overview](https://docs.polymarket.com/developers/gamma-markets-api/overview)
- [WSS Overview](https://docs.polymarket.com/developers/CLOB/websocket/wss-overview)

### Official Client Libraries
- [Python CLOB Client](https://github.com/Polymarket/py-clob-client)
- [TypeScript Real-Time Client](https://github.com/Polymarket/real-time-data-client)
- [Rust CLOB Client](https://github.com/Polymarket/rs-clob-client)
- [AI Agents Framework](https://github.com/Polymarket/agents)

### Community Resources
- [Polymarket API Guide 2025 | PolyTrack](https://blog.polytrackhq.app/blog/polymarket-api-guide)
- [Bitquery Polymarket API](https://docs.bitquery.io/docs/examples/polymarket-api/)
- [Apidog Tutorial](https://apidog.com/blog/polymarket-api/)

### TradeFlow Context
- [Case Study: Polymarket Bot Analysis](../case-studies/polymarket-bot-analysis.md)
- [TradeFlow Roadmap](../roadmap.md)
- [Reference Architecture](../reference.md)

---

## 🎯 Final Recommendation

### When to Implement

**✅ GO if:**
1. Coinbase spot trading proven profitable (30+ days)
2. Phase 1 validation finds real arbitrage edge (10%+ discrepancy, 10+ opportunities/week)
3. User comfortable with legal/regulatory risks
4. Capital available for second strategy ($500-1000)

**❌ NO-GO if:**
1. Coinbase integration not yet profitable
2. No arbitrage opportunities found in validation
3. Legal concerns too high (US restrictions)
4. Better to focus on refining Coinbase strategy

### My Assessment

**Priority:** Medium-High (exciting opportunity, but validate first)

**Realistic Outcome:**
- Arbitrage edge may already be closed (many bots now exploit this)
- Even if edge exists, likely small (5-7% vs. the 10%+ from 2025)
- Legal risks are real (Polymarket banned in US)
- But... if edge exists, it's lower risk than directional trading

**Bottom Line:** Excellent addition AFTER simple phase proven, but don't count on 98% win rates.

---

**Status:** 📋 Documentation complete. Ready to implement AFTER Phase 0A/0B/1 complete.
**Next Action:** Finish current roadmap, then revisit this plan.
