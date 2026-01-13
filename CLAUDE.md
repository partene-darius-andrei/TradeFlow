# TradeFlow - Core Trading Logic (Standalone v3.0)

**Last Updated:** 2026-01-12
**Project Status:** Phase 3 - Standalone JVM Architecture
**Current Build:** SUCCESS

**⚠️ CRITICAL ARCHITECTURE CHANGE:**
**TradeFlow now uses PERPETUAL FUTURES ONLY.**
All spot trading logic has been removed. The system trades BTC-PERP with leverage (LONG/SHORT positions) instead of spot BTC/USD.

## 🎯 Architecture (Standalone JVM)

TradeFlow has transitioned from Android app to standalone JVM application for reliability and deployment simplicity.

| Layer | Responsibility | Key Component |
|-------|----------------|---------------|
| **Engine** | Main Loop & Execution | `Main.kt`, JWT Auth, HTTP Client |
| **Domain** | Pure Logic & Models | `ExecuteTradingCycleUseCase`, `AnalyzeCandlesUseCase`, `MakeTradingDecisionUseCase` |
| **Exchange** | Remote API | `CoinbaseRepository`, `JwtRepository` |

### Why Standalone?
- **Reliability:** No Android lifecycle interruptions
- **Simplicity:** Pure Kotlin JVM - easier to deploy, debug, and run on servers
- **Cloud-Ready:** Can run on any laptop, VPS, or cloud instance
- **95% Code Reuse:** All domain logic remains unchanged

---

## 🏗️ Core Components

### 1. ExecuteTradingCycleUseCase (The "Brain")
**Implementation:** `ExecuteTradingCycleUseCase.kt`

Single entry point for the trading cycle. Handles:
- Fetching fresh market data and portfolio state
- Risk management (Drawdown circuit breaker, position sizing)
- Triggering the decision engine and executing orders
- **Trailing stop management:** Dynamically updates stop-loss orders to protect profits

### 2. AnalyzeCandlesUseCase (The "Eyes")
**Implementation:** `AnalyzeCandlesUseCase.kt`

Unified service for technical indicators.
- Calculates SMA, ADX, ATR, RSI, Volume, CMF in a **single pass** over candles
- Uses `ta4j` internally but exposes clean `BigDecimal` results

### 3. MakeTradingDecisionUseCase (The "Strategy")
**Implementation:** `MakeTradingDecisionUseCase.kt`

**Stateful** engine with 3-candle hysteresis to prevent whipsaw mode switching.
- Converts indicators into a `Decision` (Wait, Trend, Range)
- Maintains internal state: `lastMode`, `confirmationCount`, `candidateMode`
- **Signal quality filters:** RSI, Volume (1.5× avg), CMF for high-confidence trades only

---

## 🚦 Current Status

### ✅ Standalone JVM Application (v3.0)
- **Module:** `:engine` - Self-contained Kotlin/JVM application
- **Authentication:** JWT generation using ES256 algorithm (ECDSA signing)
- **HTTP Client:** Ktor with OkHttp engine for Coinbase API
- **Credential Loading:** From `local.properties` or environment variables
- **Proof-of-Concept:** Successfully fetches Coinbase account balances
- **Next Step:** Expand to full trading loop

### ✅ Streamlined Domain Layer (v3.0)
- **Simplified Dependency Injection:** DependencyInjection object with builder pattern (no lambdas, no use cases stored)
- **Use Case Pattern:** Each use case gets dependencies via default parameters from DependencyInjection
- **Renamed Classes:** AdaptiveOptimizer → AdaptiveOptimizerUseCase, CoinbaseJwtGenerator → JwtRepository
- **Stateful Decision Engine:** TradingDecisionEngine uses 3-candle hysteresis
- **Complete Risk Management:** RiskManager fully implemented with position sizing, drawdown monitoring, validation
- **Rich Models:** Portfolio and other models encapsulate their own utility logic

---

## 🔄 Development Workflow

### Running the Standalone App
```bash
./gradlew :engine:run
```

This executes `Main.kt` which:
1. Loads credentials from `local.properties`
2. Generates JWT token for Coinbase API authentication
3. Fetches account balances and displays results

### Local Development
1. **Verify Build:** `./gradlew :engine:build`
2. **Run Application:** `./gradlew :engine:run`
3. **Add Features:** Extend `Main.kt` to include trading logic
4. **Test:** Use simulated exchange for strategy validation

### Credential Setup
Create `local.properties` in project root:
```properties
coinbase.api.key=your_coinbase_api_key_here
coinbase.api.secret=-----BEGIN EC PRIVATE KEY-----\n...\n-----END EC PRIVATE KEY-----
```

Or set environment variables:
```bash
export COINBASE_API_KEY="your_key"
export COINBASE_API_SECRET="your_secret"
```

---

## 📚 Additional Documentation

### Bitcoin-First Strategy
See detailed strategy documentation at end of this file covering:
- Why Bitcoin-only trading for small accounts
- Regime switching logic (Defense, Trend, Range)
- Risk management parameters
- Realistic performance expectations

### Backtesting Framework
Production-realistic trading simulation documented at end of file:
- Realistic Coinbase Advanced Trade fees
- Slippage modeling
- Order matching logic
- Performance metrics (Sharpe, drawdown, win rate)

### Future Enhancement Ideas
Archive of valuable future features documented at end of file:
- Polymarket arbitrage integration
- Advanced optimization
- Multi-asset support
- Confidence-based position sizing

---

## 🎯 Bitcoin-First Trading Strategy

**Capital Target:** $500 starting account
**Timeline:** 5-10 years to meaningful passive income

### Core Decision
**Trade BTC/USDT exclusively until account reaches $2,500+**

Research shows:
- **97% of day traders fail** (only 1-3% profitable long-term)
- **Trading costs kill small accounts** (fees + slippage eat 1.5%+ daily on $500)
- **Bitcoin has the best risk-adjusted returns** for automated trading
- **Small-cap altcoins lose 3-15% to slippage alone** on $500 positions

### Realistic Expectations
- **Monthly return target:** 5% (exceptional skill required)
- **Year 1 goal:** Don't lose money (education phase)
- **Year 2-3 goal:** Consistent 3-5% monthly returns
- **Year 5-10:** Compound to $10k-20k (meaningful passive income)

### The Math That Matters

**Binance with BNB discount:**
- Maker fee: 0.075%
- Taker fee: 0.075%
- Round-trip cost: 0.15%

**$500 account trading 3x daily:**
```
Position size: $500
Round-trip cost: $500 × 0.15% = $0.75 per trade
Daily trades: 3
Daily fees: $2.25
Monthly fees: $67.50

Required monthly return to break even: 13.5%
Required monthly return for 5% profit: 18.5%
```

**Why this kills altcoins:**

| Asset | Spread + Slippage | Round-Trip Cost | 3 Trades/Day Cost |
|-------|------------------|-----------------|-------------------|
| BTC/USDT | 0.02% + 0.05% | 0.22% | $3.30/day |
| ETH/USDT | 0.03% + 0.15% | 0.33% | $4.95/day |
| SOL/USDT | 0.05% + 0.40% | 0.60% | $9.00/day |
| Small-cap | 3% + 5% | 8-15% | **$120-225/day** |

**Conclusion:** Small-cap altcoins are mathematically untradeable at $500.

### Risk Management Parameters

**Position Sizing (1-2% Risk Rule):**
```kotlin
data class RiskParameters(
    val accountSize: BigDecimal = "500".toBigDecimal(),
    val riskPerTrade: BigDecimal = "0.01".toBigDecimal(),  // 1% = $5 per trade
    val maxDailyLoss: BigDecimal = "0.05".toBigDecimal(),   // 5% = $25 per day
    val maxDailyTrades: Int = 3,
    val minConfidence: Double = 0.75,
    val stopLossPercent: BigDecimal = "0.02".toBigDecimal(), // 2% stop
    val takeProfitPercent: BigDecimal = "0.04".toBigDecimal() // 4% target (2:1 R:R)
)
```

**Emergency Stop (15% Drawdown):**
- Account drops from $500 → $425 → trading halts
- Manual review required to restart

**Perpetual Futures Risk Management (v3.0):**
- **Margin-based limits:** Position size controlled by margin requirements (notionalValue / leverage)
- **No BTC balance exposure checks:** Removed for perpetuals (spot-only concept)
- **Liquidation monitoring:** Automatic position closure if price hits liquidation level
- **Funding rate checks:** Skip trades if funding rate > maxAcceptableFundingRate
- **Per-position limits:** Still enforced via maxPositionPercent (default 5.23%)

### Regime Switching Strategy

**Two modes (Perpetual Futures v3.0):**
1. **TREND:** Strong directional market (ADX ≥ threshold, default 15.69)
   - Place directional trade: LONG if price above SMA200, SHORT if below
   - ATR-based stops and targets
   - **Trailing stops enabled:** Stop moves with price to protect profits (+15% performance)

2. **RANGE:** Choppy sideways market (ADX < threshold)
   - Mean-reversion strategy: trade against SMA200 deviations
   - LONG when price drops > 0.5× ATR below SMA200
   - SHORT when price rises > 0.5× ATR above SMA200
   - Target: Return to SMA200 (mean)
   - Stop: 2× ATR beyond entry

**Signal Quality Filters (v3.0):**
- RSI confirmation: LONG requires RSI > 50, SHORT requires RSI < 50
- Volume confirmation: Trade volume must exceed 1.5× average
- CMF (Chaikin Money Flow): Optional confidence boost for money flow alignment

**Expected Performance:**
- Win rate: 52-58% (after fees and slippage)
- Risk/reward: 2:1 to 2.7:1
- Monthly return: 3-5% (exceptional skill required)

---

## 🧪 Realistic Backtesting Framework

Production-realistic trading simulation for validating the complete TradeFlow system before live deployment.

### Components

#### PortfolioSimulator
Tracks USD and BTC balances with realistic fee deductions.

**Features:**
- Coinbase Advanced Trade fees (0.4% taker, 0.25% maker)
- Validates sufficient balance before fills
- Calculates total equity and tracks high water mark

#### OrderBook
Manages limit order queue and realistic matching logic.

**Features:**
- Separate BUY/SELL queues sorted by price
- BUY orders fill when candle.low touches price
- SELL orders fill when candle.high touches price

#### SimulatedExchangeRepository
In-memory exchange implementing ExchangeRepository interface.

**Features:**
- All repository methods implemented
- Market orders fill instantly with 0.1% slippage
- Limit orders fill when price touches level
- Realistic order lifecycle management

#### PerformanceTracker
Comprehensive metrics calculation and reporting.

**Tracked Metrics:**
- Basic: Total PnL, win rate, trade counts
- Risk: Max drawdown, Sharpe ratio, profit factor
- Per-strategy: TREND vs RANGE breakdown

#### BacktestEngine
Orchestrates complete trading simulations.

**Process:**
1. Advances time candle-by-candle
2. Matches pending limit orders
3. Executes DecisionEngine for market regime
4. Places orders with risk management
5. Handles emergency liquidation (15% drawdown)
6. Tracks performance metrics

### Usage Example

```kotlin
import com.tradeflow.core.domain.simulator.*
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val candles = BinanceDataLoader.fetchBtcUsdtYear2024()

    val config = BacktestConfig(
        startingCapital = BigDecimal("500"),
        productId = "BTC-USD",
        historicalCandles = candles
    )

    val engine = BacktestEngine()
    val result = engine.runBacktest(config)

    println("Final Equity: ${result.finalEquity} USD")
    println("Total PnL: ${result.totalPnl} (${result.totalPnlPercent}%)")
    println("Win Rate: ${result.winRate}%")
    println("Max Drawdown: ${result.maxDrawdownPercent}%")
    println("Sharpe Ratio: ${result.sharpeRatio}")
}
```

### Fee Structure

**Coinbase Advanced Trade (Tier 1):**
- **Taker fees**: 0.4% (market orders)
- **Maker fees**: 0.25% (limit orders)

**Slippage:**
- **Market BUY**: +0.1% (buy at ask)
- **Market SELL**: -0.1% (sell at bid)

### Success Criteria

Backtest must pass ALL of these:
1. Time period: 7+ years
2. Win rate: 52%+ (after fees and slippage)
3. Sharpe ratio: 1.0+
4. Max drawdown: < 20%
5. Average monthly return: 3-5%
6. Trades per month: 20-90

---

## 📚 Historical Data Testing (BinanceDataLoader)

### Quick Start

```kotlin
// Get last 10 hourly candles
val candles = BinanceDataLoader.fetchHistoricalCandles(
    symbol = "BTCUSDT",
    interval = "1h",
    limit = 10
)

// Get last week of data
val weekData = BinanceDataLoader.fetchBtcUsdtLastWeek()

// Get entire year 2024
val yearData = BinanceDataLoader.fetchBtcUsdtYear2024()
```

### API Details

**Binance Klines Endpoint:**
- **URL**: `https://api.binance.com/api/v3/klines`
- **Authentication**: None required (public endpoint)
- **Rate Limit**: Weight 2 per request
- **Max Candles**: 1000 per request

**Available Intervals:**
- Minutes: `1m`, `3m`, `5m`, `15m`, `30m`
- Hours: `1h`, `2h`, `4h`, `6h`, `8h`, `12h`
- Days: `1d`, `3d`
- Weeks: `1w`
- Months: `1M`

---

## 🔮 Future Enhancement Ideas

**IMPORTANT:** Do NOT implement any features below until you achieve:
- ✅ 30+ days of profitable live trading
- ✅ Win rate > 52%
- ✅ Sharpe ratio > 1.0
- ✅ Max drawdown < 15%

### 1. Polymarket Arbitrage Integration

**Overview:** Add Polymarket prediction market trading as a second strategy alongside Bitcoin spot trading. Exploit cross-market arbitrage by comparing crypto spot prices on Coinbase with Polymarket prediction odds.

**The Opportunity:**
```
Cross-Market Arbitrage Strategy:
1. Monitor: BTC spot price on Coinbase (Current: $42,150)
2. Compare: Polymarket odds for "BTC above $42k" (Current: 0.52, should be 0.99+)
3. Execute: Buy "YES" shares if mispriced
4. Exit: Sell when odds correct to fair value
```

**Advantages:**
- **Edge Type:** Arbitrage (easier than prediction)
- **Win Rate:** 70-90% possible (vs 52-58% for directional)
- **Capital Risk:** Lower (bounded loss)

**Critical Risks:**
⚠️ **IMPORTANT:**
1. **Legal:** Polymarket restricted in US (requires VPN or offshore entity)
2. **Liquidity:** Lower volume than Coinbase
3. **Market Efficiency:** Arbitrage opportunities may already be closed

**Implementation Timeline:**
**ONLY implement AFTER:**
1. ✅ Coinbase spot trading proven profitable (30+ days)
2. ✅ Phase 1 validation finds real arbitrage edge (10%+ discrepancy)
3. ✅ Legal/regulatory risks assessed and accepted

### 2. Advanced Optimization Features

**Multi-Objective Pareto Optimization:**
- Optimize return vs risk frontier
- Find multiple optimal solutions (aggressive, balanced, conservative)
- Effort: 1-2 weeks

**Ensemble Strategies:**
- Combine multiple optimized configs running in parallel
- Diversification reduces single-strategy risk
- Effort: 2-3 weeks
- **Best ROI**

**Reinforcement Learning Integration:**
- RL agent learns optimal regime switching
- High complexity risk (may overtrain)
- Effort: 4-6 weeks

### 3. Confidence Scoring & Position Sizing

**Implementation:**
```kotlin
sealed class Decision {
    abstract val confidence: Double  // 0.0 to 1.0

    data class Trend(
        val stopLossPrice: Double,
        val takeProfitPrice: Double,
        override val confidence: Double  // Based on ADX strength + confirmations
    ) : Decision()
}

// Position sizing scales with confidence
fun calculatePositionSize(
    confidence: Double,
    portfolioValue: Double
): Double {
    val baseSize = 0.02  // 2% base
    val maxSize = 0.05   // 5% max
    val minConfidence = 0.75

    return when {
        confidence < minConfidence -> 0.0  // Don't trade
        else -> {
            val scaledSize = baseSize + (maxSize - baseSize) *
                ((confidence - minConfidence) / (1.0 - minConfidence))
            portfolioValue * scaledSize
        }
    }
}
```

**Effort:** 1-2 days
**Benefit:** Higher returns if confidence calibration is accurate
**Risk:** Overconfidence can lead to larger losses

### 4. Multi-Asset Support

**Phase 2: Add Ethereum (Account: $2,500+)**
- **Unlock condition:** Sustained 3 months of 3%+ monthly returns on BTC
- **Trading Pairs:** BTC/USDT (70%) + ETH/USDT (30%)
- **Why:** Correlation ~0.85, acceptable spreads, large-cap safety

**Phase 3: Consider Solana (Account: $5,000+)**
- **Unlock condition:** Account above $5,000 + 6 months profitable
- **Trading Pairs:** BTC/USDT (50%) + ETH/USDT (30%) + SOL/USDT (20%)
- **Why wait:** Higher slippage, more volatile, limited historical data

**Never Trade:** Small-cap altcoins
- 90% go to zero in bear markets
- 3-15%+ round-trip costs
- Delisting risk
- Insufficient data for backtesting

### 5. Advanced Testing Features

**Walk-Forward Optimization:**
- Continuously re-optimize on rolling window
- Adapt to changing market conditions
- Effort: 1 week

**Sortino and Calmar Ratio Tracking:**
- Better risk metrics than Sharpe alone
- Effort: 1-2 days

**Monte Carlo Simulation:**
- Test across thousands of randomized scenarios
- Effort: 1 week

---

## ⏳ Implementation Priority

**DO FIRST (before any enhancements):**
1. ✅ Complete core optimization
2. ✅ Wait for favorable market conditions (price > SMA200)
3. ✅ Achieve 30+ days profitable live trading
4. ✅ Prove win rate > 52%, Sharpe > 1.0, drawdown < 15%

**THEN CONSIDER (in priority order):**
1. **Confidence-based position sizing** (1-2 days, high ROI)
2. **Ensemble strategies** (2-3 weeks, good ROI)
3. **Advanced metrics** (Sortino, Calmar) (1-2 days)
4. **Multi-asset support** (only if account > $2,500)
5. **Polymarket arbitrage** (3 months, high risk/reward)
6. **RL integration** (4-6 weeks, high complexity risk)

---

## 🚫 What NOT to Implement

**Never implement:**
1. ❌ ML/AI complexity (simple rules beat complex models)
2. ❌ High-frequency trading (fees kill small accounts)
3. ❌ Leveraged trading (catastrophic risk for algo)
4. ❌ Small-cap altcoins (3-15% round-trip costs)
5. ❌ Grid trading without regime detection (fails in trend)

---

## 🎓 Key Insights Archive

### From Polymarket Case Study
1. ✅ Simplicity beats complexity
2. ✅ Volume + small wins compound powerfully
3. ✅ Speed matters in execution
4. ✅ Only trade when edge is clear
5. ❌ Can't replicate 98% win rate (arbitrage vs prediction)

### From Bitcoin-First Strategy
1. ✅ Trade one asset well (BTC mastery > multi-asset mediocrity)
2. ✅ Cut losses ruthlessly (2% stop, no exceptions)
3. ✅ Trade high-conviction setups only (75%+ confidence)
4. ✅ Keep position size small (1-2% risk per trade)
5. ✅ Treat it like a business (not gambling)

### From Optimization Results
1. ✅ 86% loss reduction achieved through parameter optimization
2. ✅ Strategy works correctly (refuses to trade in unfavorable conditions)
3. ✅ More aggressive entry + tighter risk management
4. ✅ Waiting for bull market conditions to demonstrate profitability

---

## 🚨 The Honest Truth

### What 97% of Traders Don't Accept

1. **Most traders lose money** (72% end year in deficit)
2. **Small accounts have structural disadvantages** (fees, slippage)
3. **5% monthly returns are exceptional** (not average)
4. **It takes 5-10 years to meaningful income** (not 6 months)
5. **You're probably not in the top 1-3%** (but you can try)

### What the Top 1-3% Do Differently

1. **Trade one asset well**
2. **Cut losses ruthlessly**
3. **Trade high-conviction setups only**
4. **Keep position size small**
5. **Accept losses as business cost**
6. **Track every metric religiously**
7. **Continuously improve strategy**
8. **Treat it like a business**

### The $500 Reality

```
Scenario 1: Typical Trader (97%)
- Year 1: $500 → $200 (-60% loss)
- Outcome: Quit trading, valuable lesson learned

Scenario 2: Disciplined Trader (2%)
- Year 1: $500 → $480 (-4% learning)
- Year 2: $480 → $600 (+25% consistency)
- Year 3: $600 → $850 (+42% scaling)

Scenario 3: Exceptional Trader (1%)
- Year 1: $500 → $580 (+16%)
- Year 2: $580 → $900 (+55%, 5% monthly)
- Year 3: $900 → $1,600 (+78%)
- Year 10: $10,000-20,000 (passive income)
```

**Which scenario are you?** You won't know until you try for 12+ months.

---

## 📋 Implementation Checklist

### Before Live Trading

- [ ] Build strategy in backtesting framework
- [ ] Backtest on 7+ years of BTC/USDT data
- [ ] Verify 52%+ win rate after realistic fees
- [ ] Verify Sharpe ratio > 1.0
- [ ] Verify max drawdown < 20%
- [ ] Paper trade for 30 days
- [ ] Compare paper results to backtest
- [ ] Fund account with $500 (money you can afford to lose)
- [ ] Set up daily loss alerts ($25 limit)
- [ ] Set up drawdown alerts (15% emergency stop)

### First 30 Days Live

- [ ] Trade BTC/USDT only
- [ ] Risk 1% per trade ($5 max)
- [ ] Max 3 trades per day
- [ ] Stop trading at $25 daily loss
- [ ] Track every metric
- [ ] Review weekly
- [ ] Adjust strategy if win rate < 48%

### After 90 Days

- [ ] Evaluate: Win rate 52%+?
- [ ] Evaluate: Monthly return 3%+?
- [ ] Evaluate: Max drawdown < 15%?
- [ ] Decision: Continue if all YES, stop if any NO

### After 1 Year

- [ ] Evaluate: Account above $450?
- [ ] Evaluate: Consistent monthly profits?
- [ ] Evaluate: Emotional discipline maintained?
- [ ] Decision: If YES to all, you're in the top 3%
- [ ] Next step: Scale to $1,000-2,000 account

---

**Strategy Status:** Ready for implementation
**Risk Level:** High (97% failure rate for day traders)
**Expected Outcome:** Education (maybe profit if you're in the top 1-3%)
**Timeline:** 5-10 years to meaningful passive income
**Capital Requirement:** $500 you can afford to lose entirely

**Next Step:** Build the engine, backtest ruthlessly, paper trade thoroughly, then deploy with discipline.
