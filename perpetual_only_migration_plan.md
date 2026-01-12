# Perpetual-Only Migration Plan

**Date:** 2026-01-12
**Objective:** Remove ALL spot trading logic, focus exclusively on perpetual futures

---

## Phase 1: SimulatedExchange.kt - Core Removal

### Remove:
1. **`btcBalance` field** (line 18) - No longer holding actual BTC
2. **`executeOrder()` spot logic** (lines 114-125) - Spot order execution
3. **`canExecute()` spot checks** (lines 99-108) - BTC balance checks
4. **`getTotalEquity()` BTC calculation** (line 132) - Remove `btcBalance * currentPrice`
5. **`getBalances()` BTC balance** (line 136) - Remove BTC from balance list
6. **`getPortfolio()` BTC balance** (lines 140-142) - Remove BTC from portfolio
7. **`placeBracketOrder()` spot branch** (lines 224-267) - Entire "else" spot trading block
8. **`placeMarketOrder()` spot logic** (lines 280-305) - Remove spot market order execution

### Keep:
- Perpetual position tracking (`perpetualPosition`, `lastFundingTime`)
- Perpetual-specific methods (`openPerpetualPosition`, `updatePerpetualPositionPnL`, `deductFundingRate`, `realizePerpetualPosition`)
- Order matching logic in `advanceTime()` (works for perpetual TP/SL)
- `getPerpetualPosition()`, `closePerpetualPosition()`, `getFundingRate()`

### Result:
- SimulatedExchange becomes pure perpetual futures simulator
- Only tracks USD margin + open perpetual positions
- No BTC holdings, no spot order execution

---

## Phase 2: ExecuteTradingCycleUseCase.kt - Remove Spot Logic

### Remove:
1. **Spot BTC balance checks** (lines 433-434):
   ```kotlin
   val btcBalance = portfolio.getBtcBalance()
   val hasBtcBalance = btcBalance > config.execution.minBtcDustThreshold
   ```

2. **Spot liquidation in circuit breaker** (lines 415-419):
   ```kotlin
   val btc = portfolio.getBtcBalance()
   if (btc > config.execution.minBtcDustThreshold) {
       exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, btc)
   }
   ```

3. **Spot liquidation in Defense** (lines 465-468):
   ```kotlin
   if (hasBtcBalance) {
       exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, btcBalance).getOrThrow()
   }
   ```

4. **Range decision handling** (lines 508-535):
   - Entire Range case - this was for spot grid trading
   - Grid orders don't make sense for perpetual futures

5. **`isInTrade` spot logic** (line 436):
   - Remove `hasBtcBalance` from condition
   - Keep only: `isInTrade = hasPerpetualPosition || hasOpenOrders`

6. **State logging spot BTC** (line 443):
   - Remove: `Spot BTC: $btcBalance`

### Keep:
- Perpetual position checks (`hasPerpetualPosition`)
- Trend decision handling (works for LONG/SHORT perpetual)
- Defense decision (as legacy fallback)
- Perpetual liquidation in circuit breaker

### Result:
- Only executes Trend decisions (LONG/SHORT perpetual)
- No spot grid trading
- Simplified state tracking

---

## Phase 3: RiskManager.kt - Remove Spot Exposure Checks

### Remove:
1. **BTC balance exposure calculation** (lines 290-300):
   ```kotlin
   if (request.side == OrderSide.BUY) {
       val currentBtcValue = portfolio.getBtcBalance() * currentPrice
       val currentExposure = currentBtcValue
           .divide(portfolio.totalEquityUsd, config.risk.percentDecimalPlaces, RoundingMode.HALF_UP)
       val newExposure = currentExposure + positionPercent

       if (newExposure > config.risk.maxTotalExposurePercent) {
           return RiskCheck.Rejected(...)
       }
   }
   ```

### Why Remove:
- Perpetual positions track exposure differently (via margin, not BTC balance)
- Exposure is already limited by margin requirements
- Position size validation still applies (per-position limits)

### Keep:
- Position size validation (maxPositionPercent)
- Drawdown monitoring
- Position sizing methods (calculateTrendPositionSize, calculateGridPositionSize)

### Result:
- Simpler validation (no BTC balance checks)
- Perpetual margin system handles exposure naturally

---

## Phase 4: Documentation Cleanup

### backtesting_validation.md:
- **Remove Test Cases 1-2** (spot BUY/SELL fees)
- **Keep Test Cases 3-15** (slippage, perpetual LONG/SHORT, funding, liquidation, etc.)
- Update intro to say "perpetual futures only"

### CLAUDE.md:
- **Remove all "spot trading" references**
- **Remove "BTC/USD" product ID references** (replace with "BTC-PERP")
- **Update strategy description** to "perpetual futures only"
- **Remove grid trading** from strategy docs (was for spot)
- **Keep** perpetual LONG/SHORT strategy

### audit_findings.md:
- **Remove spot-related findings**
- Mark as "perpetual-only architecture"

---

## Phase 5: Balance Model Changes

### Portfolio.kt:
Need to check:
- Does `getBtcBalance()` method exist?
- Is it used anywhere besides what we're removing?
- Should we remove it entirely?

### Balance.kt:
- Keep as-is (generic balance model used for USD margin)

---

## Summary of Changes

| Component | Spot Logic | Perpetual Logic | Action |
|-----------|------------|-----------------|--------|
| SimulatedExchange | ❌ Remove all | ✅ Keep all | Simplify to perpetual-only |
| ExecuteTradingCycleUseCase | ❌ Remove spot checks, Range | ✅ Keep Trend (LONG/SHORT) | Remove grid trading |
| RiskManager | ❌ Remove BTC exposure checks | ✅ Keep margin validation | Simplify validation |
| Documentation | ❌ Remove spot test cases/docs | ✅ Keep perpetual docs | Clean up docs |

---

## Commits Plan

**Commit 1:** Remove spot logic from SimulatedExchange
**Commit 2:** Remove spot logic from ExecuteTradingCycleUseCase
**Commit 3:** Remove spot validation from RiskManager
**Commit 4:** Clean up documentation (backtesting_validation.md, CLAUDE.md, audit_findings.md)

---

## Risk Assessment

**Breaking Changes:** YES - This is a major architectural change
**Backwards Compatibility:** NO - Spot trading will no longer work
**Testing Required:** YES - Run backtests after each commit
**Build Verification:** YES - Compile after each change

---

**Status:** Ready for execution
**Next Step:** Begin Phase 1 (SimulatedExchange removal)
