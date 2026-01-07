# TradeFlow Technical Reference

**Last Updated:** 2026-01-07
**Document Status:** FINAL - Hierarchical structure for easy navigation
**Target Platform:** Android (API 26+)
**Exchange:** Coinbase Advanced Trade API
**Strategy:** Regime-Switching (Trend + Grid + Defense)

---

## Overview

This technical reference provides complete implementation guidance for TradeFlow, a rule-based, regime-adaptive cryptocurrency trading system for Android. The documentation is split into focused sections for easy navigation.

### Trading Strategy

Engine detects market conditions and switches between strategies:

| Mode | Condition | Action |
|------|-----------|--------|
| **DEFENSE** | Price < SMA(200) | Cash preservation, cancel buys |
| **TREND** | Price > SMA(200) AND ADX > 25 | Ride trends with bracket orders |
| **RANGE** | Price > SMA(200) AND ADX < 25 | Grid trading to harvest volatility |

---

## Documentation Structure

### 📡 API Integration

**[api/coinbase.md](api/coinbase.md)** - Coinbase Advanced Trade API reference
- REST API endpoints and rate limits
- WebSocket channels (market data + user orders)
- JWT ES256 authentication
- Order types (market, limit, bracket, stop-limit)
- Fee structure and grid break-even calculations
- Candle data and sandbox limitations

### 🎯 Trading Strategy

**[strategy/overview.md](strategy/overview.md)** - Strategy specification and architecture
- Phase 1 strategy components (SMA, ADX, ATR)
- Decision logic and regime switching
- Risk limits and position sizing
- Android architecture (tech stack, project structure)
- Doze mode survival strategy

### 💻 Implementation Examples

Complete Kotlin code examples organized by component:

1. **[implementation/domain.md](implementation/domain.md)** - Domain models and decision engine
   - Candle and Decision models
   - EngineDecisionEngine with ta4j indicators
   - Hysteresis logic for regime switching

2. **[implementation/security.md](implementation/security.md)** - Security and authentication
   - SecureKeyStore (EncryptedSharedPreferences)
   - JwtGenerator (ES256 signing for Coinbase)

3. **[implementation/clients.md](implementation/clients.md)** - API communication
   - CoinbaseRestApi (order placement, candles, accounts)
   - CoinbaseWebSocket (ticker + user order updates)
   - Ktor HTTP/WebSocket implementation

4. **[implementation/storage.md](implementation/storage.md)** - Persistence and background execution
   - Room database schema (orders, portfolio, grid config)
   - TradingService (foreground service with wake lock)
   - Strategy loop and execution logic

5. **[implementation/config.md](implementation/config.md)** - Project setup
   - Gradle dependencies
   - AndroidManifest.xml
   - Testing checklist

---

## Critical Constraints (Quick Reference)

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

## Quick Start Guide

### For Implementation

1. **Start with API integration:** Read [api/coinbase.md](api/coinbase.md) to understand Coinbase endpoints
2. **Understand the strategy:** Read [strategy/overview.md](strategy/overview.md) for decision logic
3. **Follow roadmap:** See [roadmap.md](roadmap.md) - Implement tickets in order (Phase 0 → Phase 4)
4. **Reference code examples:** Use implementation/*.md files as blueprints
5. **Configure project:** Follow [implementation/config.md](implementation/config.md)

### For Code Review

1. **API calls:** Check against [api/coinbase.md](api/coinbase.md) for correct endpoints/format
2. **Strategy logic:** Verify against [strategy/overview.md](strategy/overview.md) decision tree
3. **Code patterns:** Compare with implementation/*.md reference code
4. **Dependencies:** Ensure [implementation/config.md](implementation/config.md) is up to date

---

## Key Research Findings

The following critical findings from Coinbase API research are integrated throughout:

1. **Rate Limits:** 10,000 REST requests/hour, 750 WS connections/second
2. **Fees:** 0.60% maker at intro tier → 1.5% minimum grid spacing
3. **JWT:** 2-minute expiry, ES256 algorithm, nonce required
4. **WebSocket:** Must subscribe to heartbeats, 60-90 second timeout
5. **Candles:** Max 350/request, use TWO_HOUR and aggregate for H4
6. **Sandbox:** Static responses only - cannot use for paper trading
7. **Bracket Orders:** `limit_price` = TP, `stop_trigger_price` = SL
8. **Battery:** Using Ktor with periodic evaluation (not 24/7 streaming), battery exemption critical
9. **Security:** EncryptedSharedPreferences, trade-only permissions

---

## Document History

| Version | Changes |
|---------|---------|
| 1.0 | Initial Gemini draft |
| 1.1 | Claude corrections (hallucinations fixed) |
| 2.0 | Full rewrite with Coinbase research |
| 2.1 | Integrated all MCP research findings, added complete code |
| **3.0** | **Reorganized into hierarchical structure with links (<1000 lines per file)** |

---

## Navigation

- **[Back to docs/README.md](README.md)** - Complete documentation index
- **[Back to roadmap.md](roadmap.md)** - Implementation roadmap
- **[Back to CLAUDE.md](../CLAUDE.md)** - Project context
