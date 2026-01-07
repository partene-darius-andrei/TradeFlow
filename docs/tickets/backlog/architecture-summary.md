# 📊 SUMMARY - Architecture Split Overview

Effort level: Small
Priority: High
Status: Refined

## TradeFlow Architecture Split

This ticket tracks the overall architecture refactoring to achieve **complete Coinbase API isolation**.

## Architecture Layers

```
┌───────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                     │
│  📱 UI Screens (Compose)  |  🧠 ViewModels  |  ⚙️ Service │
└───────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                          │
│  📦 Models  |  🎯 UseCases  |  🧠 DecisionEngine  |  🚨 Risk │
│                                                           │
│  🔌 INTERFACES (ExchangeRepository, WebSocketService)     │
└───────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                       DATA LAYER                           │
│  ┌─────────────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │ 🏛️ COINBASE       │  │ 🗄️ Room    │  │ 🔐 Security │ │
│  │ (Swappable)       │  │ (Local DB) │  │ (KeyStore) │ │
│  └─────────────────────┘  └─────────────┘  └─────────────┘ │
└───────────────────────────────────────────────────────────┘
```

## Ticket Categories

### 🔌 INTERFACE (Blockers - Do First)

- [ ]  ExchangeRepository
- [ ]  ExchangeWebSocketService
- [ ]  AuthTokenProvider

### 📦 DOMAIN (Pure Kotlin - No Android)

- [ ]  Models (Candle, Order, Decision, Account)
- [ ]  DecisionEngine
- [ ]  RiskManager
- [ ]  UseCases (GetPortfolio, PlaceOrder)

### 🏛️ COINBASE (Isolated - Swappable)

- [ ]  CoinbaseRepository
- [ ]  CoinbaseWebSocketService
- [ ]  CoinbaseJwtGenerator

### 📱 UI (Pure Compose)

- [ ]  Dashboard Screen
- [ ]  Settings Screen

### 🧠 PRESENTATION (ViewModels)

- [ ]  Dashboard ViewModel
- [ ]  Settings ViewModel

### ⚙️ SERVICE

- [ ]  TradingService (Orchestrator)

### 🗄️ INFRA

- [ ]  Room Database
- [ ]  Credential Store
- [ ]  Hilt DI Module

## Implementation Order

**Week 1: Foundation**

1. 🔌 Interfaces (all 3)
2. 📦 Domain Models
3. 💉 Hilt DI Module skeleton

**Week 2: Domain Logic**

1. 🧠 DecisionEngine
2. 🚨 RiskManager
3. 🎯 UseCases

**Week 3: Coinbase Implementation**

1. 🏛️ JWT Generator
2. 🏛️ Repository
3. 🏛️ WebSocket

**Week 4: Presentation**

1. 📱 UI Screens
2. 🧠 ViewModels
3. ⚙️ TradingService

## Parallel Development Tracks

| Track | Owner | Tickets |
| --- | --- | --- |
| **A: Domain** | - | Models, DecisionEngine, RiskManager, UseCases |
| **B: Coinbase** | - | JWT, Repository, WebSocket |
| **C: UI** | - | Screens, ViewModels |
| **D: Infra** | - | Room, DI, Service |

Tracks A, B, C can run in parallel after interfaces are defined.

## Success Criteria

- [ ]  Zero Coinbase imports in domain/ package
- [ ]  Zero Coinbase imports in presentation/ package
- [ ]  All Coinbase code in data/exchange/coinbase/
- [ ]  Can swap to Kraken by implementing interfaces
- [ ]  Unit tests pass without network