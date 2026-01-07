# 🏗️ MODULE: Project Modularization Setup

Effort level: Medium
Priority: High
Status: Not started
Module: :app

## Objective

Set up the multi-module Gradle project structure for TradeFlow.

## Module Architecture

```
TradeFlow/
├── app/                      # Application module
├── core/
│   ├── domain/              # Pure Kotlin domain models & interfaces
│   ├── data/                # Data layer abstractions
│   └── ui/                  # Shared UI components & theme
├── exchange/
│   ├── api/                 # Exchange abstraction interfaces
│   └── coinbase/            # Coinbase implementation
├── feature/
│   ├── dashboard/           # Dashboard feature
│   └── settings/            # Settings feature
└── service/
    └── trading/             # Trading foreground service
```

## Dependency Graph

```
:app
├── :feature:dashboard
├── :feature:settings
└── :service:trading

:feature:dashboard
├── :core:domain
├── :core:ui
└── :exchange:api

:exchange:coinbase
├── :exchange:api
└── :core:domain

:service:trading
├── :core:domain
├── :exchange:api
└── :core:data
```

## Files to Create

- `settings.gradle.kts` - Module includes
- `build-logic/` - Convention plugins for consistent config
- Each module's `build.gradle.kts`

## Convention Plugins

```kotlin
// build-logic/convention/src/main/kotlin/
- AndroidApplicationConventionPlugin.kt
- AndroidLibraryConventionPlugin.kt  
- AndroidFeatureConventionPlugin.kt
- KotlinLibraryConventionPlugin.kt  // For pure Kotlin modules
```

## Key Rules

- `:core:domain` has NO Android dependencies (pure Kotlin)
- `:exchange:api` has NO Android dependencies (pure Kotlin)
- Feature modules never depend on each other
- Only `:app` depends on `:exchange:coinbase` (DI binding)

## Acceptance Criteria

- [ ]  All modules compile independently
- [ ]  Dependency graph enforced (no circular deps)
- [ ]  Convention plugins applied consistently
- [ ]  `./gradlew :core:domain:test` runs without Android SDK