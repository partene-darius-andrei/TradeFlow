# Fix Critical Backtesting Bugs - Implementation Plan

**Branch:** main (direct)
**Scope:** Fix all 7 issues (4 critical + 3 moderate)
**Timeline:** 3-4 hours implementation + testing

---

## PHASE 1: CRITICAL FIXES

### Fix #1: Portfolio Equity Missing Perpetual PnL [CRITICAL]
**Risk:** [BREAKING] - Changes portfolio calculation contract
**Files:** `UpdatePortfolioUseCase.kt`
**Changes:**
1. Add method to fetch perpetual positions from repository
2. Sum unrealized PnL from all positions
3. Add to cash equity to get total equity

**Implementation:**
```kotlin
suspend fun execute(): Result<Portfolio> = runCatching {
    val balances = repository.getBalances().getOrThrow()

    // Get all perpetual positions and sum unrealized PnL
    val positions = repository.getAllPerpetualPositions().getOrElse { emptyList() }
    val perpetualPnl = positions.sumOf { it.unrealizedPnl }

    val cashEquity = balances.sumOf { it.available + it.hold }
    val totalEquity = cashEquity + perpetualPnl

    Portfolio(
        balances = balances,
        totalEquityUsd = totalEquity,
        timestamp = Instant.now()
    )
}
```

**Test Plan:**
- Add unit test with mock perpetual position
- Verify equity = cash + unrealized PnL
- Test with no positions (should equal cash only)

---

### Fix #2: Liquidation Auto-Trigger [CRITICAL]
**Risk:** [MODERATE] - Adds new liquidation logic
**Files:** `SimulatedExchange.kt`
**Changes:**
1. Check candle high/low vs liquidation price on each advanceTime()
2. Force-close position at liquidation price
3. Apply 5% liquidation fee
4. Clear position and reset funding time

**Implementation:**
```kotlin
fun advanceTime(newCandle: Candle) {
    currentPrice = newCandle.close
    history.add(newCandle)

    // Check liquidation BEFORE processing orders
    checkLiquidation(newCandle)

    // Process limit orders
    processLimitOrders(newCandle)

    // Update position PnL
    updatePerpetualPositionPnL()

    // Deduct funding
    deductFundingRate(newCandle.timestamp)
}

private fun checkLiquidation(candle: Candle) {
    val position = perpetualPosition ?: return

    val liquidationTriggered = when (position.side) {
        OrderSide.BUY -> candle.low <= position.liquidationPrice
        OrderSide.SELL -> candle.high >= position.liquidationPrice
    }

    if (liquidationTriggered) {
        val liquidationFee = position.margin * BigDecimal("0.05")  // 5%
        val remainingMargin = position.margin - liquidationFee

        usdBalance += remainingMargin.coerceAtLeast(BigDecimal.ZERO)
        perpetualPosition = null
        lastFundingTime = null

        println("⚠️ LIQUIDATED ${position.side} position at ${position.liquidationPrice}")
    }
}
```

**Test Plan:**
- Test LONG liquidation (candle.low touches liquidation price)
- Test SHORT liquidation (candle.high touches liquidation price)
- Verify 5% fee deducted
- Verify position cleared

---

### Fix #3: Funding Rate Double-Count [CRITICAL]
**Risk:** [MODERATE] - Changes funding logic
**Files:** `SimulatedExchange.kt`
**Changes:**
1. Only deduct funding from position margin
2. Remove deduction from usdBalance
3. Margin is returned to balance when position closes

**Implementation:**
```kotlin
private fun deductFundingRate(currentTime: Instant) {
    val position = perpetualPosition ?: return
    val lastFunding = lastFundingTime ?: return

    val hoursSinceLastFunding = Duration.between(lastFunding, currentTime).toHours()

    if (hoursSinceLastFunding >= parameters.fundingIntervalHours) {
        val fundingCost = position.size * position.currentPrice * parameters.fundingRatePerInterval
        val newMargin = position.margin - fundingCost

        if (newMargin <= BigDecimal.ZERO) {
            // Margin exhausted - liquidate
            perpetualPosition = null
            lastFundingTime = null
            println("⚠️ LIQUIDATED due to funding exhaustion")
        } else {
            // Only reduce margin, DON'T touch usdBalance
            perpetualPosition = position.copy(margin = newMargin)
            lastFundingTime = currentTime
        }
    }
}
```

**Test Plan:**
- Verify funding only deducted from margin once
- Test position close returns correct margin to balance
- Test funding exhaustion liquidation

---

### Fix #4: Sharpe Ratio Calculation [CRITICAL]
**Risk:** [SAFE] - Only affects metrics calculation
**Files:** `LongTermBacktestTest.kt`
**Changes:**
1. Filter out zero/negative equity before calculation
2. Use sample stddev (n-1) instead of population stddev
3. Annualize by candles per year, not trading days

**Implementation:**
```kotlin
private fun calculateSharpe(equity: List<Double>, candleIntervalHours: Int = 4): Double {
    // Filter out invalid equity values
    val validEquity = equity.filter { it > 0.01 }
    if (validEquity.size < 2) return 0.0

    // Calculate returns
    val returns = validEquity.zipWithNext { a, b -> (b - a) / a }
    if (returns.isEmpty()) return 0.0

    val avgReturn = returns.average()

    // Sample standard deviation (n-1)
    val variance = returns.map { (it - avgReturn).pow(2) }.sum() / (returns.size - 1)
    val stdDev = sqrt(variance)

    if (stdDev == 0.0) return 0.0

    // Annualize by actual candles per year
    val candlesPerYear = 8760.0 / candleIntervalHours  // 8760 hours per year
    val sharpe = avgReturn / stdDev * sqrt(candlesPerYear)

    return sharpe
}
```

**Test Plan:**
- Test with normal equity curve
- Test with near-zero equity (should filter out)
- Test with negative equity (should filter out)
- Verify annualization factor (4H candles → 2190 per year)

---

## PHASE 2: MODERATE FIXES

### Fix #5: Maker Fees for Limit Orders [MODERATE]
**Risk:** [SAFE] - Makes fees more realistic
**Files:** `SimulatedExchange.kt`
**Changes:**
1. Add `orderType` parameter to fee calculation
2. Use maker fee (0.25%) for limit orders
3. Use taker fee (0.4%) for market orders

**Implementation:**
```kotlin
// In closePerpetualPosition (line 285)
val fee = exitValue * parameters.makerFeeRate  // Limit close uses maker fee

// In openPerpetualPosition (line 334)
val fee = notionalValue * parameters.takerFeeRate  // Market entry uses taker fee

// In processLimitOrders (line 95)
val fillPrice = order.price  // No slippage on limit fills
val notionalValue = fillSize * fillPrice
val fee = notionalValue * parameters.makerFeeRate  // Limit orders use maker fee
```

**Test Plan:**
- Verify market entry charged 0.4%
- Verify limit TP/SL exit charged 0.25%
- Calculate round-trip cost (should be 0.65%, not 0.8%)

---

### Fix #6: Remove Slippage from Limit Orders [MODERATE]
**Risk:** [SAFE] - Makes fills more realistic
**Files:** `SimulatedExchange.kt`
**Changes:**
1. Remove applySlippage() call for limit order fills
2. Keep slippage only for market orders

**Implementation:**
```kotlin
// In processLimitOrders (line 95)
val fillPrice = order.price  // Fill at exact limit price (no slippage)

// In placeBracketOrder - entry is market order
val entryPrice = applySlippage(currentPrice, side)  // Keep slippage for market entry
```

**Test Plan:**
- Test TP fill at exact TP price (no +0.1%)
- Test SL fill at exact SL price (no -0.1%)
- Test market entry still has slippage

---

### Fix #7: Realistic Order Fill Modeling [MODERATE]
**Risk:** [SAFE] - Minor refinement
**Files:** `SimulatedExchange.kt`
**Changes:**
1. Add micro-slippage when limit orders trigger
2. TP fills at limit + 0.05%
3. SL fills at limit - 0.05%

**Implementation:**
```kotlin
private fun processLimitOrders(newCandle: Candle) {
    val filledOrders = mutableListOf<LimitOrder>()

    limitOrders.forEach { order ->
        val triggered = when (order.side) {
            OrderSide.BUY -> newCandle.low <= order.price
            OrderSide.SELL -> newCandle.high >= order.price
        }

        if (triggered) {
            // Determine if this is TP or SL based on current position
            val position = perpetualPosition
            val isTakeProfit = position != null &&
                ((position.side == OrderSide.BUY && order.side == OrderSide.SELL && order.price > position.entryPrice) ||
                 (position.side == OrderSide.SELL && order.side == OrderSide.BUY && order.price < position.entryPrice))

            // Apply micro-slippage
            val fillPrice = if (isTakeProfit) {
                order.price * BigDecimal("0.9995")  // TP: -0.05%
            } else {
                order.price * BigDecimal("1.0005")  // SL: +0.05%
            }

            closePerpetualPosition(fillPrice)
            filledOrders.add(order)
        }
    }

    limitOrders.removeAll(filledOrders)
}
```

**Test Plan:**
- Test TP fills at limit - 0.05%
- Test SL fills at limit + 0.05%
- Verify fill price is within candle range

---

## PHASE 3: VALIDATION

### Test Suite Updates
**Files:** Create new test file `BacktestValidationTest.kt`
**Tests:**
1. Portfolio equity with perpetual positions
2. Liquidation triggers correctly
3. Funding deducted once (not twice)
4. Sharpe ratio calculation accuracy
5. Fee structure (maker vs taker)
6. Order fill realism

### Full Backtest Re-run
**Files:** `LongTermBacktestTest.kt`
**Process:**
1. Run full 2024 backtest with all fixes
2. Compare results to previous (expect lower returns due to liquidation)
3. Verify metrics are realistic:
   - Win rate: 52%+ (after fixes should be lower but still positive)
   - Sharpe ratio: 1.0+ (should be HIGHER after fix #4)
   - Max drawdown: < 20%
   - Monthly return: 3-5% (expect LOWER after liquidation fix)

### Expected Changes in Backtest Results
- **Returns:** Lower by 1-2% monthly (liquidation + funding fixes)
- **Sharpe:** Higher by 2-3× (calculation fix)
- **Drawdown:** Higher (liquidation will trigger at correct times)
- **Win rate:** Lower by 2-5% (realistic liquidations)

---

## IMPLEMENTATION SEQUENCE

### Commit 1: Fix Portfolio Equity (Fix #1)
- Update `UpdatePortfolioUseCase.kt`
- Add test for perpetual PnL inclusion
- Build and verify no compilation errors
- Commit: "Fix: Include perpetual unrealized PnL in portfolio equity"

### Commit 2: Fix Liquidation Detection (Fix #2)
- Update `SimulatedExchange.kt` - add checkLiquidation()
- Add liquidation tests
- Build and verify
- Commit: "Fix: Auto-trigger liquidation when price touches liquidation level"

### Commit 3: Fix Funding Double-Count (Fix #3)
- Update `SimulatedExchange.kt` - remove usdBalance deduction
- Add funding tests
- Build and verify
- Commit: "Fix: Deduct funding rate only from margin, not balance"

### Commit 4: Fix Sharpe Ratio (Fix #4)
- Update `LongTermBacktestTest.kt` - new calculateSharpe()
- Add Sharpe calculation tests
- Build and verify
- Commit: "Fix: Correct Sharpe ratio calculation (sample stddev + proper annualization)"

### Commit 5: Maker Fees + Slippage (Fix #5, #6)
- Update `SimulatedExchange.kt` - differentiate maker/taker fees
- Remove slippage from limit fills
- Add fee tests
- Build and verify
- Commit: "Fix: Use maker fees for limit orders, remove slippage from limit fills"

### Commit 6: Realistic Order Fills (Fix #7)
- Update `SimulatedExchange.kt` - add micro-slippage to limit triggers
- Add fill realism tests
- Build and verify
- Commit: "Fix: Add realistic micro-slippage to limit order fills"

### Commit 7: Re-run Full Backtest
- Execute `LongTermBacktestTest.kt`
- Document results in commit message
- Commit: "Test: Re-run full backtest with all fixes applied"

---

## ROLLBACK PLAN

If any commit breaks the build or tests:
1. `git revert HEAD` (undo last commit)
2. Fix the issue
3. Re-commit

If multiple commits need reverting:
```bash
git log --oneline -7  # See last 7 commits
git revert <commit-hash>
```

---

## SUCCESS CRITERIA

**Phase 1 (Critical):**
- [ ] Portfolio equity includes perpetual PnL
- [ ] Liquidations auto-trigger at correct price
- [ ] Funding only deducted once
- [ ] Sharpe ratio calculated correctly
- [ ] All tests pass
- [ ] Build succeeds

**Phase 2 (Moderate):**
- [ ] Maker fees used for limit orders
- [ ] No slippage on limit fills
- [ ] Micro-slippage on limit triggers
- [ ] All tests pass
- [ ] Build succeeds

**Phase 3 (Validation):**
- [ ] Full backtest completes
- [ ] Win rate: 45-55% (expect lower after liquidation fix)
- [ ] Sharpe ratio: 0.8-1.5 (expect higher after calculation fix)
- [ ] Max drawdown: 15-25% (expect higher after liquidation fix)
- [ ] Results are internally consistent

---

## ESTIMATED CHANGES

**Lines of code:**
- Add: ~150 lines (new methods, tests)
- Modify: ~80 lines (existing methods)
- Delete: ~20 lines (dead code, wrong logic)

**Files touched:**
- `UpdatePortfolioUseCase.kt` (1 file)
- `SimulatedExchange.kt` (1 file)
- `LongTermBacktestTest.kt` (1 file)
- New: `BacktestValidationTest.kt` (1 file)

**Total:** 4 files, ~250 lines changed

---

## NOTES

1. **No dead code removal yet** - Will clean up RiskManager unused methods in separate commit
2. **No candle validation yet** - Will add in separate commit (data model change)
3. **Focus on backtesting fixes only** - Portfolio equity fix affects both live and backtest
4. **All changes are backwards compatible** - No breaking changes to public APIs

---

**Ready to proceed with implementation.**
**Next step: Commit 1 - Fix Portfolio Equity**
