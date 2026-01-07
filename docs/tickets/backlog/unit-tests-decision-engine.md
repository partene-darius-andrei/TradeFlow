# 🧪 Unit Tests - Decision Engine

Effort level: Medium
Priority: Medium
Status: Not started

## Objective

Write unit tests for the strategy decision engine.

## File

`test/domain/strategy/EngineDecisionEngineTest.kt`

## Test Cases

### Defense Mode

```kotlin
@Test
fun `returns Defense when price below SMA200`() {
    val candles = generateCandlesWithSMA(sma200 = 50000.0)
    val currentPrice = 48000.0  // Below SMA
    
    val decision = engine.evaluate(candles, currentPrice)
    
    assertIs<Decision.Defense>(decision)
}
```

### Trend Mode

```kotlin
@Test
fun `returns Trend after 3 candles with high ADX`() {
    val candles = generateCandlesWithADX(adx = 30.0)  // > 25
    val currentPrice = 52000.0  // Above SMA
    
    // First 2 calls should return Wait
    repeat(2) {
        val decision = engine.evaluate(candles, currentPrice)
        assertIs<Decision.Wait>(decision)
    }
    
    // Third call should return Trend
    val decision = engine.evaluate(candles, currentPrice)
    assertIs<Decision.Trend>(decision)
}
```

### Range Mode

```kotlin
@Test
fun `returns Range after 3 candles with low ADX`() {
    val candles = generateCandlesWithADX(adx = 20.0)  // < 25
    val currentPrice = 52000.0  // Above SMA
    
    // Need 3 confirmations
    repeat(3) { engine.evaluate(candles, currentPrice) }
    
    val decision = engine.evaluate(candles, currentPrice)
    assertIs<Decision.Range>(decision)
}
```

### Hysteresis

```kotlin
@Test
fun `Defense resets hysteresis counters`() {
    // Build up trend confirmations
    val trendCandles = generateCandlesWithADX(adx = 30.0)
    engine.evaluate(trendCandles, 52000.0)  // 1 confirm
    engine.evaluate(trendCandles, 52000.0)  // 2 confirms
    
    // Drop below SMA - should reset
    val defenseCandles = generateCandlesWithSMA(sma200 = 55000.0)
    engine.evaluate(defenseCandles, 50000.0)
    
    // Back above SMA with trend - should need 3 fresh confirms
    val decision = engine.evaluate(trendCandles, 52000.0)
    assertIs<Decision.Wait>(decision)  // Only 1 confirm
}
```

### Grid Spacing

```kotlin
@Test
fun `grid spacing never below 1_5 percent`() {
    val candles = generateCandlesWithATR(atr = 100.0)  // Low ATR
    val currentPrice = 50000.0
    
    repeat(3) { engine.evaluate(candles, currentPrice) }
    val decision = engine.evaluate(candles, currentPrice) as Decision.Range
    
    val minSpacing = currentPrice * 0.015  // $750
    assertTrue(decision.gridSpacing >= minSpacing)
}
```

## Test Helpers

- `generateCandlesWithSMA()` - Create 200+ candles with target SMA
- `generateCandlesWithADX()` - Create candles with target ADX
- `generateCandlesWithATR()` - Create candles with target ATR

## Acceptance Criteria

- All 4 modes tested
- Hysteresis logic verified
- Edge cases covered
- Tests run in < 5 seconds