# Backlog Grooming Analysis
**Date:** 2026-01-09
**Analyst:** Claude Sonnet 4.5
**Total Backlog:** 36 tickets

---

## Decision Matrix

### ✅ KEEP IN BACKLOG (6 tickets) - Valid future work

| Ticket | Rationale |
|--------|-----------|
| **13-rest-api-client.md** | Phase 2 - Extend CoinbaseRepository with orders/candles |
| **14-websocket-client.md** | Phase 2 - Real-time price feeds (not yet implemented) |
| **15-decision-engine.md** | Phase 2 - IMMEDIATE NEXT (SMA/ADX/ATR logic) |
| **16-risk-manager.md** | Phase 2 - Position sizing, drawdown tracking |
| **19-integration-tests.md** | Phase 3 - E2E testing with live API |
| **20-mvp-milestone.md** | Phase 3 - Final validation milestone |

### 🗄️ ARCHIVED - Service tickets (NOT NEEDED)

| Ticket | Rationale |
|--------|-----------|
| **17-trading-service.md** | App runs FOREGROUND 24/7 = high process priority, no service needed |
| **18-battery-optimization.md** | Different concerns for foreground app (keep screen on, etc.) |

**Architecture Decision:** Foreground app with trading loop as coroutine is simpler and sufficient. No service lifecycle complexity needed.

---

### ✅ DONE - Move to done/ (15 tickets)

| Ticket | Evidence | Move Action |
|--------|----------|-------------|
| **project-modularization.md** | All 8 modules exist in settings.gradle.kts | → done/ |
| **project-setup.md** | App initialized, Hilt configured, dependencies added | → done/ |
| **domain-models-candle-decision.md** | Candle.kt, Decision.kt exist in core:domain | → done/ |
| **domain-models-exchange-agnostic.md** | Balance.kt, Order.kt, Portfolio.kt, Ticker.kt all exist | → done/ |
| **room-database-entities-daos.md** | 4 entities + 4 DAOs implemented in core:data | → done/ |
| **room-database-setup.md** | EngineDatabase.kt exists with all tables | → done/ |
| **credential-store-infra.md** | StaticCredentialStore.kt + build-time injection working | → done/ |
| **coinbase-jwt-generator-updated.md** | CoinbaseJwtGenerator.kt exists with ES256 + BouncyCastle | → done/ |
| **coinbase-repository.md** | CoinbaseRepository.kt exists (partial - getBalances working) | → done/ (Phase 1 scope) |
| **interface-auth-token-provider.md** | AuthTokenProvider.kt exists in core:domain/auth/ | → done/ |
| **interface-exchange-repository.md** | ExchangeRepository.kt exists in core:domain/repository/ | → done/ |
| **interface-exchange-websocket.md** | ExchangeWebSocket.kt exists in core:domain/repository/ | → done/ |
| **di-exchange-module.md** | ExchangeModule.kt exists in exchange:coinbase/di/ | → done/ |
| **ui-dashboard.md** | DashboardScreen.kt exists with all components | → done/ |
| **dashboard-viewmodel-duplicate.md** | DashboardViewModel.kt exists and working | → done/ |

---

### 🗄️ ARCHIVE - Duplicates/Superseded/Not Needed (12 tickets)

| Ticket | Reason | Archive Action |
|--------|--------|----------------|
| **risk-manager-duplicate.md** | Duplicate of ticket 16 (same content) | → archived/ |
| **decision-engine-strategy.md** | Duplicate of ticket 15 (same content) | → archived/ |
| **coinbase-rest-order-placement.md** | Part of ticket 13 (Full REST API) | → archived/ |
| **coinbase-websocket.md** | Duplicate of ticket 14 (WebSocket Client) | → archived/ |
| **ui-settings.md** | Already done - SettingsScreen.kt exists | → archived/ |
| **trading-service-orchestrator.md** | Duplicate of ticket 17 (Trading Service) | → archived/ |
| **integration-tests-coinbase.md** | Duplicate of ticket 19 (Integration Tests) | → archived/ |
| **untitled-placeholder.md** | Empty placeholder - no content | → archived/ |
| **api-research.md** | Documentation/notes - belongs in docs/api/ | → archived/ |
| **architecture-summary.md** | Documentation - already covered in CLAUDE.md | → archived/ |

---

### ⚠️ KEEP AS REFERENCE (3 tickets) - Not active work, but useful

| Ticket | Rationale | Action |
|--------|-----------|--------|
| **unit-tests-decision-engine.md** | Part of ticket 15 implementation | Keep (testing guidance) |
| **usecase-get-portfolio.md** | Future: UseCases layer (optional pattern) | Keep (maybe future) |
| **usecase-place-order.md** | Future: UseCases layer (optional pattern) | Keep (maybe future) |

---

## Summary Actions

```
Current Backlog: 36 tickets

Actions:
- KEEP in backlog: 6 tickets (Phase 2-3 roadmap)
- MOVE to done/: 15 tickets (implemented)
- MOVE to archived/: 12 tickets (duplicates/superseded/not needed)
- KEEP as reference: 3 tickets (future/optional)

After grooming: 9 tickets in backlog (6 active + 3 reference)
```

**Architecture Simplification:** Removed service tickets (17, 18) - foreground app doesn't need service

---

## Implementation Status by Category

### ✅ Foundation (100% Complete)
- [x] Project modularization (8 modules)
- [x] Domain models (6 models: Candle, Decision, Order, Balance, Portfolio, Ticker)
- [x] Repository interfaces (ExchangeRepository, AuthTokenProvider, ExchangeWebSocket)
- [x] Room database (4 entities + 4 DAOs)
- [x] Credential store (build-time injection)

### ✅ Coinbase Integration Phase 1 (100% Complete)
- [x] JWT Generator (ES256 with BouncyCastle)
- [x] CoinbaseRepository (getBalances working)
- [x] DI modules (AuthModule, ExchangeModule, NetworkModule)
- [x] DTO/Mapper layer (AccountDto → Balance)

### ✅ UI Foundation (100% Complete)
- [x] Theme + Core UI components
- [x] Dashboard screen (with real Coinbase data)
- [x] Settings screen
- [x] Navigation (AppNavHost)

### ❌ Phase 2: Core Trading Logic (0% Complete)
- [ ] Decision Engine (ticket 15)
- [ ] Risk Manager (ticket 16)
- [ ] Full REST API (ticket 13)
- [ ] WebSocket Client (ticket 14)

### ❌ Phase 3: Testing & MVP (0% Complete)
- [ ] Integration Tests (ticket 19)
- [ ] MVP Milestone (ticket 20)

### 🗄️ Archived: Not Needed (Foreground App Architecture)
- [x] Trading Service (ticket 17) - App runs foreground, no service needed
- [x] Battery Optimization (ticket 18) - Different concerns for foreground

---

## Recommendations

1. **Execute moves immediately** - Clear separation helps focus
2. **Keep backlog lean** - Only 9 tickets remain (down from 36)
3. **Next priority** - Ticket 15 (Decision Engine) is unblocked and ready
4. **UseCases layer** - Defer until proven necessary (YAGNI principle)
5. **Architecture simplification** - Foreground app eliminates service complexity

---

## File Operations Summary

```bash
# Move to done/ (15 files)
mv backlog/project-modularization.md done/
mv backlog/project-setup.md done/
mv backlog/domain-models-candle-decision.md done/
mv backlog/domain-models-exchange-agnostic.md done/
mv backlog/room-database-entities-daos.md done/
mv backlog/room-database-setup.md done/
mv backlog/credential-store-infra.md done/
mv backlog/coinbase-jwt-generator-updated.md done/
mv backlog/coinbase-repository.md done/
mv backlog/interface-auth-token-provider.md done/
mv backlog/interface-exchange-repository.md done/
mv backlog/interface-exchange-websocket.md done/
mv backlog/di-exchange-module.md done/
mv backlog/ui-dashboard.md done/
mv backlog/dashboard-viewmodel-duplicate.md done/

# Move to archived/ (10 files)
mv backlog/risk-manager-duplicate.md archived/
mv backlog/decision-engine-strategy.md archived/
mv backlog/coinbase-rest-order-placement.md archived/
mv backlog/coinbase-websocket.md archived/
mv backlog/ui-settings.md archived/
mv backlog/trading-service-orchestrator.md archived/
mv backlog/integration-tests-coinbase.md archived/
mv backlog/untitled-placeholder.md archived/
mv backlog/api-research.md archived/
mv backlog/architecture-summary.md archived/

# Result: 11 tickets in backlog (8 active + 3 reference)
```
