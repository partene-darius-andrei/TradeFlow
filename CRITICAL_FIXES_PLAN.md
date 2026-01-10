# Critical Fixes Implementation Plan
**Created:** 2026-01-09
**Status:** Ready for Implementation
**Branch:** claude/critical-fixes

---

## Phase 1: Core Trading Logic Fixes [CRITICAL]

### Fix 1.1: TradeOrchestrator Position Sizing
**File:** `TradeOrchestrator.kt:67`
**Issue:** Uses `currentPrice` instead of `decision.entryPrice` for Trend mode
**Fix:** Change to `decision.entryPrice`
**Risk:** [safe] - Direct bug fix
**Test:** Unit test for Trend execution

### Fix 1.2: Grid Position Sizing
**File:** `TradeOrchestrator.kt:80`
**Issue:** Uses `currentPrice` instead of `gridPrice` for position sizing
**Fix:** Change to `gridPrice`
**Risk:** [safe] - Direct bug fix
**Test:** Unit test for Range execution

### Fix 1.3: Trend Direction Detection
**File:** `TradingDecisionEngine.kt:72`
**Issue:** Hardcoded `OrderSide.BUY` - no trend direction logic
**Solution:** Add directional indicator using SMA slope + price position
**Logic:**
- If price > SMA200 AND SMA is rising → LONG (BUY)
- If price < SMA200 → DEFENSE (no trend trades)
- If price > SMA200 but SMA is falling → Wait/RANGE (no trend)
**Risk:** [moderate] - Changes strategy behavior
**Test:** Unit tests for uptrend, downtrend, sideways

### Fix 1.4: Multi-Level Grid Trading
**File:** `TradeOrchestrator.kt:76-96`
**Issue:** Only places 1 order instead of 5 levels
**Solution:** Loop through levels and place multiple orders
**Logic:**
```kotlin
for (i in 1..decision.levels) {
    val levelPrice = currentPrice - (decision.gridSpacing * BigDecimal(i))
    placeLimitOrder(levelPrice, sizePerLevel)
}
```
**Risk:** [moderate] - Changes execution logic
**Test:** Unit test verifying multiple orders placed

---

## Phase 2: Thread Safety & State Management [HIGH]

### Fix 2.1: TradingDecisionEngine Thread Safety
**File:** `TradingDecisionEngine.kt:15-18`
**Issue:** Mutable state in singleton (race condition risk)
**Solution:** Make stateless - move state to external `EngineState` object
**Changes:**
- Create `DecisionEngineState` data class
- Pass state as parameter to `evaluate()`
- Return new state with Decision
**Risk:** [moderate] - API change
**Test:** Concurrent execution test

### Fix 2.2: High Water Mark Management
**File:** `TradeOrchestrator.kt:27-38`
**Issue:** Doesn't update high water mark when equity increases
**Solution:** Add logic to update HWM before drawdown check
**Risk:** [safe] - Missing logic addition
**Test:** Unit test for HWM updates

---

## Phase 3: Data Validation & Error Handling [MEDIUM]

### Fix 3.1: Candle Data Validation
**File:** `TechnicalAnalysisService.kt:26-39`
**Issue:** No validation of OHLC integrity
**Solution:** Add validation before creating bars
**Checks:**
- high >= open, close, low
- low <= open, close, high
- volume > 0
- prices > 0
**Risk:** [safe] - Defensive programming
**Test:** Unit tests with invalid candles

### Fix 3.2: TradeOrchestrator Error Handling
**File:** `TradeOrchestrator.kt:21-24`
**Issue:** Generic catch-all loses error context
**Solution:** Granular error handling per API call
**Risk:** [safe] - Better error messages
**Test:** Unit tests for API failures

---

## Phase 4: Comprehensive Unit Tests [HIGH]

### Test 4.1: TradeOrchestrator Test Suite
**File:** `TradeOrchestratorTest.kt` (NEW)
**Coverage:**
- Defense mode liquidation
- Trend mode position opening
- Range mode grid placement
- Drawdown circuit breaker
- Error scenarios
- State transitions

### Test 4.2: TradingDecisionEngine Hysteresis Tests
**File:** `TradingDecisionEngineTest.kt` (EXPAND)
**Coverage:**
- 1-candle confirmation → Wait
- 2-candle confirmation → Wait
- 3-candle confirmation → Switch
- ADX flip-flop → Stay in mode
- Price crosses SMA during hysteresis → Immediate Defense

### Test 4.3: Integration Tests
**File:** `RealTradeSimulationTest.kt` (EXPAND)
**Coverage:**
- Bear market scenarios
- Sideways market scenarios
- Multiple grid fills

---

## Phase 5: Architecture Improvements [MEDIUM]

### Fix 5.1: Decision Model Validation
**File:** `Decision.kt`
**Solution:** Add validation in init blocks
**Checks:**
- Trend: stopLoss < entryPrice < takeProfit (for LONG)
- Trend: takeProfit < entryPrice < stopLoss (for SHORT)
- All: positionSize between 0 and 1
- All: ATR > 0

### Fix 5.2: Extract Strategy Execution Logic
**File:** `TradeOrchestrator.kt`
**Solution:** Extract execution into separate classes
**New Classes:**
- `TrendExecutor`
- `RangeExecutor`
- `DefenseExecutor`
**Risk:** [moderate] - Refactoring
**Benefit:** SRP compliance, easier testing

---

## Implementation Order

### Sprint 1 (Critical - Do First)
1. Fix 1.3: Trend Direction Detection ← **MOST CRITICAL**
2. Fix 1.1: Trend Position Sizing
3. Fix 1.2: Grid Position Sizing
4. Test 4.1: TradeOrchestrator Unit Tests
5. Fix 1.4: Multi-Level Grid Trading

### Sprint 2 (High Priority)
6. Fix 2.1: Thread Safety (Stateless Engine)
7. Fix 2.2: High Water Mark Updates
8. Test 4.2: Hysteresis Tests
9. Fix 3.1: Candle Validation

### Sprint 3 (Polish)
10. Fix 3.2: Error Handling
11. Fix 5.1: Decision Validation
12. Test 4.3: Integration Tests
13. Fix 5.2: Extract Executors (optional)

---

## Success Criteria

### Before Merging to Main
- [ ] All critical fixes implemented (1.1-1.4)
- [ ] TradeOrchestrator has 80%+ test coverage
- [ ] Hysteresis logic has dedicated tests
- [ ] Trend direction works in bear market scenarios
- [ ] Grid trading places multiple orders
- [ ] All existing tests still pass
- [ ] Build succeeds with no warnings

### Before Production Deploy
- [ ] Backtest shows positive PnL in bear markets
- [ ] Backtest shows grid strategy works as designed
- [ ] Thread safety verified with concurrent tests
- [ ] All Medium+ priority fixes completed
- [ ] Code review approved

---

## Risk Mitigation

### High-Risk Changes
1. **Trend Direction Logic:** Test extensively with historical data
2. **Stateless Engine:** Verify hysteresis still works correctly
3. **Multi-Level Grid:** Start with 2 levels, then scale to 5

### Rollback Plan
- Keep fixes in separate commits
- Each fix should be independently revertible
- Maintain feature flags for new logic (if needed)

---

## Post-Implementation

### Monitoring
- Track decision distribution (Defense/Trend/Range %)
- Monitor position sizes vs expected
- Log grid order placements
- Alert on emergency liquidations

### Documentation
- Update CLAUDE.md with new strategy details
- Document trend direction algorithm
- Add grid trading behavior to README
- Create troubleshooting guide
