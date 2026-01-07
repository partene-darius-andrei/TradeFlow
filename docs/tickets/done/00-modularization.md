# 🏗️ MODULE: Project Modularization Setup

**Effort Level:** Medium
**Priority:** CRITICAL (blocks all Phase 0 tickets)
**Status:** ✅ COMPLETE
**Completed:** 2026-01-07
**PR:** #5
**Blocked By:** None
**Blocks:** All Phase 0-4 tickets

---

## Objective

Transform single-module Android app into multi-module architecture with clear layer separation BEFORE implementing any business logic. This is **the perfect time** to modularize - only 7 files exist, zero business logic implemented.

**Key architectural decision:** Coinbase integration is a **separate module** (`:exchange:coinbase`) to enable easy exchange swapping and clean separation of concerns.

---

## Why Modularize NOW?

### Current State
```
TradeFlow/
└── app/
    └── src/main/java/com/dpart/tradeflow/
        ├── MainActivity.kt              (7 files total)
        ├── TradeFlowApp.kt
        ├── di/ (3 empty Hilt modules)
        └── data/local/ (2 dummy files)
```

### Problems with Single Module
1. **No dependency enforcement** - Presentation can import Coinbase directly
2. **Slower build times** - Change 1 file = rebuild everything
3. **Hard to test** - Domain logic coupled to Android
4. **Difficult to swap exchanges** - Coinbase code not isolated
5. **No parallel development** - Everything in one module

### Benefits of Modularization
1. **✅ Enforced architecture** - Gradle prevents invalid dependencies
2. **✅ Faster builds** - Gradle caches unchanged modules
3. **✅ Pure Kotlin domain** - Zero Android dependencies, easier testing
4. **✅ Isolated exchange code** - Swap Coinbase → Kraken with zero domain changes
5. **✅ Parallel work** - Features can be developed independently

---

## Proposed Module Structure

```
TradeFlow/
├── app/                              # Application module (minimal - DI wiring only)
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # App entry point, permissions
│   │   ├── kotlin/com/tradeflow/
│   │   │   ├── MainActivity.kt       # Hosts NavHost
│   │   │   ├── TradeFlowApp.kt       # Application class (Timber, Hilt)
│   │   │   ├── TradeFlowNavGraph.kt  # Compose navigation
│   │   │   └── di/
│   │   │       └── AppModule.kt      # App-level DI bindings
│   │   └── res/                      # App icon, strings, etc.
│   └── build.gradle.kts              # Depends on all features + :core:ui
│
├── core/
│   ├── domain/                       # Pure Kotlin (NO Android dependencies)
│   │   ├── src/main/kotlin/com/tradeflow/core/domain/
│   │   │   ├── model/                # Candle, Order, Portfolio, Decision
│   │   │   ├── repository/           # ExchangeRepository interface
│   │   │   ├── strategy/             # DecisionEngine interface
│   │   │   └── risk/                 # RiskManager interface
│   │   ├── src/test/kotlin/          # JVM unit tests only
│   │   └── build.gradle.kts          # kotlin("jvm") plugin, NO Android
│   │
│   ├── data/                         # Generic data layer (Room, credentials)
│   │   ├── src/main/kotlin/com/tradeflow/core/data/
│   │   │   ├── local/
│   │   │   │   ├── entity/           # Room entities
│   │   │   │   ├── dao/              # Room DAOs
│   │   │   │   └── database/         # Room database
│   │   │   ├── security/             # SecureCredentialStore
│   │   │   └── di/
│   │   │       ├── DatabaseModule.kt # Room DI
│   │   │       └── SecurityModule.kt # Credentials DI
│   │   ├── src/androidTest/kotlin/   # Integration tests (Room)
│   │   └── build.gradle.kts          # Depends on :core:domain
│   │
│   └── ui/                           # Shared UI components
│       ├── src/main/kotlin/com/tradeflow/core/ui/
│       │   ├── theme/                # Theme, Color, Typography
│       │   ├── components/           # Reusable composables
│       │   └── extension/            # Compose extensions
│       └── build.gradle.kts          # Depends on :core:domain
│
├── exchange/                         # Exchange-specific implementations
│   └── coinbase/                     # Coinbase Advanced Trade API (ISOLATED)
│       ├── src/main/kotlin/com/tradeflow/exchange/coinbase/
│       │   ├── auth/                 # CoinbaseJwtGenerator (ES256)
│       │   ├── api/                  # CoinbaseRepository (REST)
│       │   ├── websocket/            # CoinbaseWebSocket
│       │   ├── dto/                  # Coinbase DTOs
│       │   ├── mapper/               # DTO → domain mappers
│       │   └── di/
│       │       └── CoinbaseModule.kt # Binds CoinbaseRepository → ExchangeRepository
│       ├── src/androidTest/kotlin/   # Coinbase API integration tests
│       └── build.gradle.kts          # Depends ONLY on :core:domain
│
└── feature/
    ├── dashboard/                    # Dashboard feature
    │   ├── src/main/kotlin/com/tradeflow/feature/dashboard/
    │   │   ├── DashboardScreen.kt    # Pure Compose UI
    │   │   ├── DashboardViewModel.kt # Business logic
    │   │   ├── DashboardUiState.kt   # UI state model
    │   │   └── di/
    │   │       └── DashboardModule.kt # Feature-specific DI
    │   └── build.gradle.kts          # Depends on :core:domain, :core:data, :core:ui
    │
    ├── trading/                      # Trading feature
    │   ├── src/main/kotlin/com/tradeflow/feature/trading/
    │   │   ├── TradingScreen.kt
    │   │   ├── TradingViewModel.kt
    │   │   ├── service/              # TradingService (foreground)
    │   │   └── di/
    │   │       └── TradingModule.kt
    │   └── build.gradle.kts          # Depends on :core:domain, :core:data, :core:ui
    │
    └── settings/                     # Settings feature
        ├── src/main/kotlin/com/tradeflow/feature/settings/
        │   ├── SettingsScreen.kt
        │   ├── SettingsViewModel.kt
        │   └── di/
        │       └── SettingsModule.kt
        └── build.gradle.kts          # Depends on :core:domain, :core:data, :core:ui
│
├── settings.gradle.kts               # Include all modules
├── build.gradle.kts                  # Root build config
└── gradle/libs.versions.toml         # Shared dependency versions
```

---

## Module Dependency Graph

```
┌─────────────────────────────────────────┐
│               :app                      │
│  (Only knows about Coinbase for DI)    │
└───┬────┬────┬────┬─────────────┬───────┘
    │    │    │    │             │
    v    v    v    v             v
┌────┐┌────┐┌────┐┌────┐   ┌─────────┐
│dash││trad││sett││ui  │   │:exchange│
│    ││    ││    ││    │   │:coinbase│
└─┬──┘└─┬──┘└─┬──┘└─┬──┘   └────┬────┘
  │     │     │     │           │
  └─────┴─────┴─────┼───────────┘
                    │
        ┌───────────┼───────────┐
        v           v           v
   ┌────────┐  ┌────────┐  ┌────────┐
   │:core:  │  │:core:  │  │:core:  │
   │ domain │  │  data  │  │   ui   │
   └────────┘  └───┬────┘  └───┬────┘
                   │           │
                   └───────────┘
                       │
                       v
                  ┌────────┐
                  │:core:  │
                  │ domain │
                  └────────┘
```

### Dependency Rules

| Module | Can Import | CANNOT Import |
|--------|-----------|---------------|
| `:app` | All features, `:exchange:coinbase`, `:core:ui` | `:core:domain`, `:core:data` (indirect only) |
| `:feature:*` | `:core:domain`, `:core:data`, `:core:ui` | `:exchange:*`, other features, `:app` |
| `:exchange:coinbase` | `:core:domain` **ONLY** | `:core:data`, `:core:ui`, `:feature:*`, `:app` |
| `:core:data` | `:core:domain` | `:exchange:*`, `:core:ui`, `:feature:*`, `:app` |
| `:core:ui` | `:core:domain` | `:exchange:*`, `:core:data`, `:feature:*`, `:app` |
| `:core:domain` | **NOTHING** (pure Kotlin) | **ALL** (no Android, no other modules) |

**CRITICAL:** Features NEVER import `:exchange:coinbase`. Only `:app` knows about Coinbase (for DI binding).

---

## Implementation Plan

### Step 1: Create Module Structure (30 min)

Create directories:
```bash
mkdir -p core/domain/src/{main,test}/kotlin/com/tradeflow/core/domain
mkdir -p core/data/src/{main,androidTest}/kotlin/com/tradeflow/core/data
mkdir -p core/ui/src/main/kotlin/com/tradeflow/core/ui
mkdir -p exchange/coinbase/src/{main,androidTest}/kotlin/com/tradeflow/exchange/coinbase
mkdir -p feature/dashboard/src/main/kotlin/com/tradeflow/feature/dashboard
mkdir -p feature/trading/src/main/kotlin/com/tradeflow/feature/trading
mkdir -p feature/settings/src/main/kotlin/com/tradeflow/feature/settings
```

### Step 2: Create build.gradle.kts Files (60 min)

#### `settings.gradle.kts` (update)
```kotlin
rootProject.name = "TradeFlow"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":exchange:coinbase")
include(":feature:dashboard")
include(":feature:trading")
include(":feature:settings")
```

#### `core/domain/build.gradle.kts` (NEW)
```kotlin
plugins {
    kotlin("jvm")  // Pure Kotlin, NO Android
}

dependencies {
    // Core Kotlin only
    implementation(libs.kotlinx.coroutines.core)

    // Technical Analysis (for DecisionEngine)
    implementation(libs.ta4j.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

#### `core/data/build.gradle.kts` (NEW)
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tradeflow.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    // Modules
    implementation(project(":core:domain"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room (local database)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Security (encrypted credentials)
    implementation(libs.security.crypto)

    // Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.room.testing)
}
```

#### `exchange/coinbase/build.gradle.kts` (NEW)
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tradeflow.exchange.coinbase"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    // Modules - ONLY depends on :core:domain
    implementation(project(":core:domain"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Ktor (HTTP/WebSocket client)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // JWT ES256 (Coinbase authentication)
    implementation(libs.nimbus.jose.jwt)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.ktor.client.mock)
}
```

#### `core/ui/build.gradle.kts` (NEW)
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.tradeflow.core.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Modules
    implementation(project(":core:domain"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Vico Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)
}
```

#### `feature/dashboard/build.gradle.kts` (NEW)
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tradeflow.feature.dashboard"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Modules
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
```

#### `feature/trading/build.gradle.kts` (NEW)
```kotlin
// Same as dashboard, but namespace = "com.tradeflow.feature.trading"
// Add: implementation(libs.work.runtime.ktx) for TradingService
```

#### `feature/settings/build.gradle.kts` (NEW)
```kotlin
// Same as dashboard, but namespace = "com.tradeflow.feature.settings"
// Add: implementation(libs.datastore.preferences) for settings
```

#### `app/build.gradle.kts` (UPDATE)
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "com.tradeflow"
    // ... existing config ...
}

dependencies {
    // Feature modules
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:trading"))
    implementation(project(":feature:settings"))

    // Exchange module (for DI wiring ONLY)
    implementation(project(":exchange:coinbase"))

    // Core modules
    implementation(project(":core:ui"))  // For theme

    // NOTE: :core:domain and :core:data are transitive dependencies
    // through features - NO direct dependency here

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Compose (for MainActivity and NavHost)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    // Timber (app-level logging)
    implementation(libs.timber)

    // Firebase (app-level)
    implementation(platform(libs.firebase.bom))
}
```

### Step 3: Migrate Existing Files (20 min)

| Current Location | New Location | Notes |
|-----------------|--------------|-------|
| `app/MainActivity.kt` | **Keep in `:app`** | Hosts NavHost |
| `app/TradeFlowApp.kt` | **Keep in `:app`** | Application class |
| `app/di/AppModule.kt` | **Keep in `:app`** | App-level DI (binds exchange) |
| `app/di/DatabaseModule.kt` | **Move to `:core:data/di/`** | Database is data layer |
| `app/di/NetworkModule.kt` | **DELETE** | Will recreate in `:exchange:coinbase/di/` |
| `app/data/local/AppDatabase.kt` | **Move to `:core:data/local/database/`** | Data layer |
| `app/data/local/PlaceholderEntity.kt` | **DELETE** | No longer needed |

### Step 4: Update Package Names (15 min)

**Before:**
```kotlin
package com.dpart.tradeflow.di
```

**After:**
```kotlin
package com.tradeflow.core.data.di  // for modules in :core:data
package com.tradeflow              // for :app
```

**Migration Script:**
```kotlin
// In DatabaseModule.kt (moved to :core:data)
package com.tradeflow.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.tradeflow.core.data.local.database.AppDatabase
// ... rest of file
```

### Step 5: Update Imports (10 min)

**Before (in AppModule.kt):**
```kotlin
import com.dpart.tradeflow.data.local.AppDatabase
```

**After:**
```kotlin
import com.tradeflow.core.data.local.database.AppDatabase
```

### Step 6: Verify Build (5 min)

```bash
./gradlew clean
./gradlew :app:assembleDebug
```

**Expected output:**
```
BUILD SUCCESSFUL in 45s
```

---

## Acceptance Criteria

### Module Creation
- [ ] All 8 modules created with correct directory structure
- [ ] All `build.gradle.kts` files created and configured (8 module + 1 root)
- [ ] `settings.gradle.kts` includes all 8 modules
- [ ] Package names follow convention: `com.tradeflow.<module>.<layer>`

### Dependency Graph
- [ ] `:core:domain` has **ZERO** Android dependencies (verify in build.gradle.kts)
- [ ] `:core:domain` uses `kotlin("jvm")` plugin (NOT Android library)
- [ ] `:core:data` depends only on `:core:domain`
- [ ] `:core:ui` depends only on `:core:domain`
- [ ] `:exchange:coinbase` depends **ONLY** on `:core:domain` (CRITICAL)
- [ ] `:feature:*` modules depend on `:core:domain`, `:core:data`, `:core:ui` (NOT `:exchange:*`)
- [ ] `:app` depends on features, `:exchange:coinbase`, `:core:ui`
- [ ] No circular dependencies (build fails if violated)
- [ ] Verify with: `./gradlew :exchange:coinbase:dependencies` (should only show :core:domain)

### File Migration
- [ ] `DatabaseModule.kt` moved to `:core:data/di/`
- [ ] `NetworkModule.kt` deleted (will recreate in `:exchange:coinbase/di/`)
- [ ] `AppDatabase.kt` moved to `:core:data/local/database/`
- [ ] `PlaceholderEntity.kt` deleted
- [ ] `MainActivity.kt` and `TradeFlowApp.kt` remain in `:app`
- [ ] `AppModule.kt` updated to import from `:exchange:coinbase`
- [ ] All imports updated to new package structure

### Build & Verification
- [ ] `./gradlew clean` succeeds
- [ ] `./gradlew :core:domain:build` succeeds (pure Kotlin, JVM tests)
- [ ] `./gradlew :core:data:build` succeeds
- [ ] `./gradlew :core:ui:build` succeeds
- [ ] `./gradlew :exchange:coinbase:build` succeeds
- [ ] `./gradlew :feature:dashboard:build` succeeds
- [ ] `./gradlew :app:assembleDebug` succeeds
- [ ] App launches on device/emulator
- [ ] No "Unresolved reference" errors in IDE
- [ ] Gradle sync completes without errors

### Documentation
- [ ] Update `CLAUDE.md` - change "Single-module" to "Multi-module"
- [ ] Update `docs/roadmap.md` - add modularization to Phase 0
- [ ] Update ticket 01-domain-models - change "Blocked by" to "None"
- [ ] Create placeholder `README.md` in each module explaining its purpose

---

## Testing Strategy

### During Migration
1. Build each module individually to catch dependency violations early
2. Verify `:core:domain` has NO Android imports (`./gradlew :core:domain:dependencies`)
3. Check dependency graph: `./gradlew :app:dependencies --configuration debugCompileClasspath`

### After Migration
1. Run full clean build: `./gradlew clean build`
2. Launch app and verify "TradeFlow" text still shows
3. Check Hilt DI still works (app doesn't crash)
4. Verify Firebase Crashlytics still initializes

---

## Rollback Plan

If migration fails:
1. `git reset --hard HEAD` (discard all changes)
2. OR: `git checkout -b backup/pre-modularization` (create backup branch first)
3. Analyze error and retry with smaller steps

---

## Post-Migration Benefits

### Immediate
- ✅ Clear architecture enforced by Gradle
- ✅ Fast builds (only changed modules rebuild)
- ✅ Pure Kotlin domain (easier testing)

### Phase 0 (Foundation)
- ✅ Domain models in `:core:domain` (no Android deps)
- ✅ Decision engine easily unit tested
- ✅ Room database in `:core:data` (isolated)

### Phase 1 (Coinbase)
- ✅ All Coinbase code in `:core:data/exchange/coinbase/`
- ✅ Easy to add Kraken: `:core:data/exchange/kraken/`
- ✅ Swap exchanges by changing DI binding only

### Phase 2 (UI)
- ✅ Features developed in parallel
- ✅ Shared theme in `:core:ui` (DRY)
- ✅ UI tests isolated per feature

### Phase 3 (Service)
- ✅ TradingService in `:feature:trading` (clear ownership)
- ✅ Service depends on domain interfaces only

---

## Dependencies Added

None - all dependencies already in `gradle/libs.versions.toml`. Just redistributing them across modules.

---

## File Count Delta

**Before:** 7 files in 1 module
**After:** ~16 files across 8 modules (6 existing + 9 build.gradle.kts + 1 settings.gradle.kts update)

---

## Estimated Timeline

| Task | Time | Complexity |
|------|------|------------|
| Create directory structure | 20 min | Low |
| Write build.gradle.kts files | 60 min | Medium |
| Migrate existing files | 20 min | Low |
| Update package names | 15 min | Low |
| Fix imports | 15 min | Low |
| Build & verify | 15 min | Low |
| Update docs | 15 min | Low |
| **TOTAL** | **~2.5 hours** | **Medium** |

---

## Why This Structure?

### `:core:domain` - Pure Kotlin
- **No Android dependencies** → Easier testing (JUnit, not AndroidJUnit)
- **Domain models** → Exchange-agnostic
- **Interfaces** → Swappable implementations

### `:core:data` - Data Layer
- **Room database** → Local persistence
- **Coinbase client** → Exchange API (isolated in `exchange/coinbase/`)
- **Repositories** → Implement domain interfaces

### `:core:ui` - Shared UI
- **Material 3 theme** → Consistent design
- **Reusable components** → DRY principle
- **Compose extensions** → Helper functions

### `:exchange:coinbase` - Coinbase Integration
- **JWT generator** → ES256 signing
- **REST client** → Orders, candles, accounts
- **WebSocket client** → Real-time ticker
- **DTOs & mappers** → API ↔ domain conversion

### `:feature:*` - Features
- **Dashboard** → Main trading screen
- **Trading** → TradingService + controls
- **Settings** → Credentials + config

### `:app` - Minimal Wiring
- **MainActivity** → Entry point
- **Navigation** → Compose NavHost
- **Application class** → Timber + Hilt init
- **AppModule** → Binds CoinbaseRepository → ExchangeRepository

---

## References

- **Android modularization guide:** https://developer.android.com/topic/modularization
- **Now in Android (Google sample):** https://github.com/android/nowinandroid
- **TradeFlow roadmap:** `docs/roadmap.md`

---

## Notes

- This is **ticket 00** because it **blocks all other tickets**
- Modularization NOW vs after Phase 0: **50x easier now** (7 files vs 100+ files)
- Clean architecture is **enforced by Gradle**, not just convention
- Pure Kotlin domain → **10x faster tests** (no Android emulator needed)
- **Coinbase as separate module** → Easy to swap exchanges in future
- **8 modules total:** app, 3 core, 1 exchange, 3 features

---

**CRITICAL:** Complete this ticket BEFORE starting Ticket 01 (Domain Models).
Otherwise, we'll have to refactor 100+ files instead of 7.

**EXCHANGE SWAPPING:** With this structure, swapping Coinbase for Kraken is:
1. Create `:exchange:kraken` module
2. Update `:app` dependency: `:exchange:coinbase` → `:exchange:kraken`
3. Update DI binding in `AppModule.kt`
4. **Done** - zero changes to domain, data, UI, or features

---

## Post-Implementation Notes

**Completed:** 2026-01-07
**PR:** https://github.com/partene-darius-andrei/TradeFlow/pull/5

### Implementation Summary

All 8 modules created successfully with dependency enforcement working. Build succeeds after resolving Hilt/Kotlin compatibility issues.

### Key Challenges & Solutions

**1. Hilt/Kotlin Version Incompatibility**
- **Problem:** Hilt 2.57.2 max supports Kotlin 2.2.0, but project was on Kotlin 2.3.0
- **Solution:** Downgraded Kotlin to 2.1.0 with KSP 2.1.0-1.0.29
- **Additional:** Added `resolutionStrategy` in root build.gradle.kts to force Kotlin stdlib 2.1.0 (prevents Compose BOM/Vico from upgrading)

**2. Java Records Support (ta4j-core)**
- **Problem:** ta4j 0.22.0 uses Java 17 Records, incompatible with Java 11 and minSdk 24
- **Solution:** Upgraded all modules to Java 17 and minSdk 29

**3. Missing Dependencies**
- Added navigation, firebase-analytics, firebase-crashlytics, testing libraries to gradle/libs.versions.toml

### Final Module Dependency Graph

```
┌─────────────────────────────────────────┐
│               :app                      │
│  (Only knows about Coinbase for DI)    │
└───┬────┬────┬────┬─────────────┬───────┘
    │    │    │    │             │
    v    v    v    v             v
┌────┐┌────┐┌────┐┌────┐   ┌─────────┐
│dash││trad││sett││ui  │   │:exchange│
│    ││    ││    ││    │   │:coinbase│
└─┬──┘└─┬──┘└─┬──┘└─┬──┘   └────┬────┘
  │     │     │     │           │
  └─────┴─────┴─────┼───────────┘
                    │
        ┌───────────┼───────────┐
        v           v           v
   ┌────────┐  ┌────────┐  ┌────────┐
   │:core:  │  │:core:  │  │:core:  │
   │ domain │  │  data  │  │   ui   │
   └────────┘  └───┬────┘  └───┬────┘
                   │           │
                   └───────────┘
                       │
                       v
                  ┌────────┐
                  │:core:  │
                  │ domain │
                  └────────┘
```

### Acceptance Criteria Status

✅ All 8 modules created with correct structure
✅ All build.gradle.kts files configured
✅ settings.gradle.kts updated
✅ :core:domain has ZERO Android dependencies
✅ :core:domain uses kotlin("jvm") plugin
✅ :exchange:coinbase depends ONLY on :core:domain
✅ Feature modules depend on :core:domain, :core:data, :core:ui
✅ No circular dependencies
✅ DatabaseModule moved to :core:data/di/
✅ NetworkModule moved to :exchange:coinbase/di/
✅ All builds succeed
✅ App launches successfully

### Build Configuration

- **Kotlin:** 2.1.0 (downgraded from 2.3.0)
- **KSP:** 2.1.0-1.0.29
- **Java:** 17 (upgraded from 11)
- **minSdk:** 29 (upgraded from 24)
- **Hilt:** 2.57.2
- **Compose BOM:** 2025.12.01
- **Room:** 2.8.4
- **Ktor:** 3.3.3

### Next Steps

With modularization complete, can now proceed with:
- Ticket 01: Domain Models (now unblocked)
- Ticket 02: Repository Interfaces
- Phase 0A implementation