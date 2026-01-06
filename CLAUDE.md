# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Vision

**TradeFlow** - Personal automated crypto trading bot for Coinbase Advanced Trade API.

**Goals:**
- Remove human emotions from trading decisions
- Run 24/7 unattended (physical device when proven)
- Simple UI, simple implementation, easy to maintain
- Backtest → Paper trade → Live (small) → Scale
- Never published - personal use only

**Reality Constraints:**
- Fees matter: ~0.25-0.5% per trade on Advanced Trade
- Most retail algo traders lose money - respect this
- Simple strategies often beat complex ML
- Every trade is a taxable event

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew clean                  # Clean build
```

## Architecture

**Clean Architecture** with Single Activity + Jetpack Compose.

```
com.dpart.tradeflow/
├── di/                          # Hilt modules
├── data/
│   ├── remote/                  # Coinbase API (Ktor), DTOs
│   ├── local/                   # Room (trades, candles, settings)
│   └── repository/              # Repository implementations
├── domain/
│   ├── model/                   # Domain models
│   ├── repository/              # Repository interfaces
│   └── usecase/                 # Use cases
├── presentation/                # Screens and ViewModels
├── trading/
│   ├── engine/                  # Trading engine (Foreground Service)
│   ├── strategy/                # Strategy implementations
│   └── risk/                    # Risk management
├── MainActivity.kt
└── TradeFlowApp.kt
```

## Tech Stack

| Library | Purpose |
|---------|---------|
| Hilt | Dependency injection |
| Ktor | HTTP + WebSocket (Coinbase API) |
| Room | Local database |
| Timber | Logging |
| Vico | Charts |
| Compose + Material3 | UI |
| Coroutines/Flow | Async |
| WorkManager | Scheduled DCA execution |
| DataStore | Encrypted API keys, settings |

## Coinbase Advanced Trade API

**REST:** `https://api.coinbase.com/api/v3/brokerage/`
**WebSocket Market:** `wss://advanced-trade-ws.coinbase.com`
**WebSocket User:** `wss://advanced-trade-ws-user.coinbase.com`
**Sandbox:** `https://api-sandbox.coinbase.com` (static responses)

**Auth:** CDP API Keys → JWT tokens (refresh every 2 min for WS)

**Key Channels:** heartbeats, ticker, candles, level2, user, market_trades

## Development Phases

See `docs/tradeflow_master_plan.md` for complete system design.
See `docs/phase1_plan.md` for current implementation details.

### Phase 1: Data Foundation + Smart DCA (Current)
- Coinbase JWT auth + API integration
- Fear & Greed API integration
- Local indicator calculation (RSI, EMA, ATR)
- Smart DCA strategy with sentiment adjustment
- Basic UI: Dashboard, History, Settings
- Trade logging with signal capture

### Phase 2: Backtesting
- Historical data download (Binance API)
- Backtesting engine
- Performance metrics & visualization

### Phase 3: Regime Detection
- Label historical data with market regimes
- Train TensorFlow classifier
- Deploy as TFLite on device
- Strategy selection based on regime

### Phase 4: Advanced Strategies
- Trend Following
- Mean Reversion
- Momentum Breakout
- Grid Trading
- Signal aggregation

### Phase 5: Risk & Hardening
- Position sizing (Kelly/ATR-based)
- Stop loss management
- Kill switch
- Daily/weekly loss limits

### Phase 6: On-Chain Edge
- Glassnode integration
- Exchange flow signals
- Whale tracking

## Key Configuration

- Package: `com.dpart.tradeflow`
- Min SDK: 24 / Target SDK: 36
- JVM: 17
- Dependencies: `gradle/libs.versions.toml`

## Critical Rules

1. **Never hardcode API keys** - Encrypted DataStore only
2. **Log every trade** - For debugging and taxes
3. **Paper trade first** - Validate before real money
4. **Account for fees** - In all calculations
5. **Kill switch always** - Immediate stop capability
