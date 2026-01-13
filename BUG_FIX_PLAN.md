# Bug Fix Implementation Plan

**Branch:** `claude/critical-bug-fixes`
**Goal:** Fix 5 critical bugs identified in consolidated review
**Risk Level:** [moderate] - Changes core trading logic but well-isolated

## Phase 1: Model Changes (Safe)

### Commit 1: Add highWaterMarkPrice to PerpetualPosition
**Files:** 1 file, ~5 lines
- `domain/src/main/kotlin/com/tradeflow/core/domain/model/PerpetualPosition.kt`
- Add `highWaterMarkPrice: BigDecimal` field to data class
- Initialize to entryPrice in constructor/copy calls
- **Risk:** [safe] - Just adding a field

## Phase 2: Exchange Bug Fixes (Moderate)

### Commit 2: Fix funding rate accounting
**Files:** 1 file, ~10 lines
- `domain/src/main/kotlin/com/tradeflow/core/domain/repository/SimulatedExchange.kt`
- Change `deductFundingRate()` to deduct from `usdBalance` directly (not margin)
- Remove funding cost subtraction from `realizePerpetualPosition()` pnl calculation
- **Risk:** [moderate] - Changes P&L accounting (critical path)

### Commit 3: Persist trailing stop high water mark
**Files:** 2 files, ~15 lines
- `domain/src/main/kotlin/com/tradeflow/core/domain/repository/SimulatedExchange.kt`
  - Update `updatePerpetualPositionPnL()` to track highWaterMarkPrice
  - Initialize highWaterMarkPrice = entryPrice when opening position
- `domain/src/main/kotlin/com/tradeflow/core/domain/usecase/ExecuteTradingCycleUseCase.kt`
  - Use position.highWaterMarkPrice instead of recalculating
- **Risk:** [moderate] - Changes trailing stop logic

## Phase 3: Configuration Adjustments (Safe)

### Commit 4: Adjust ADX thresholds to realistic values
**Files:** 1 file, ~3 lines
- `domain/src/main/kotlin/com/tradeflow/core/domain/model/RiskProfile.kt`
- Change `adxRangeThreshold` from 1.38 → 12.0 (BALANCED)
- Change other profiles proportionally
- **Risk:** [safe] - Just parameter tuning

### Commit 5: Relax signal filters for realistic trade frequency
**Files:** 1 file, ~5 lines
- `domain/src/main/kotlin/com/tradeflow/core/domain/usecase/MakeTradingDecisionUseCase.kt`
- Make RSI filter optional (only apply if RSI is extreme < 30 or > 70)
- Keep volume filter but lower threshold from 1.5x → 1.2x average
- **Risk:** [safe] - Strategy parameter tuning

## Phase 4: Observability (Safe)

### Commit 6: Add comprehensive performance metrics
**Files:** 2 files, ~50 lines
- `domain/src/test/kotlin/com/tradeflow/core/domain/HistoricalBacktestTest.kt`
  - Add Sharpe ratio calculation
  - Add max drawdown tracking
  - Add win rate, profit factor, R:R ratio
  - Print detailed metrics summary
- **Risk:** [safe] - Test-only changes

## Verification Plan

After each commit:
1. Run `./gradlew :domain:build` to verify compilation
2. Check for IDE errors
3. Commit with descriptive message

After all commits:
1. Run full backtesting suite
2. Compare metrics before/after fixes
3. Verify realistic P&L (funding rate correctly applied)
4. Verify trailing stops work (highWaterMark persists)

## Expected Outcomes

- **Funding rate:** Annual P&L now 10-12% lower (realistic)
- **Trailing stops:** Actually work (stops don't move backwards)
- **Trade frequency:** 3-5 trades/month (up from near-zero)
- **Performance metrics:** Full visibility into strategy performance
- **ADX range mode:** Actually triggers (10-20% of candles, not < 1%)

## Timeline

- Phase 1: 10 minutes
- Phase 2: 20 minutes
- Phase 3: 10 minutes
- Phase 4: 20 minutes
- Verification: 30 minutes
- **Total: ~90 minutes**
