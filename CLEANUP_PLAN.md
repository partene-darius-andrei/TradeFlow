# TradeFlow Nuclear Cleanup Plan

**Created:** 2026-01-10
**Objective:** Delete ALL UI, keep ONLY backtesting business logic, add comprehensive comments everywhere, remove dead code

---

## PHASE 1: DELETE UI CODE (19 files + 3 modules)

### 1.1 Delete App Presentation Layer (7 files)
**Risk:** [safe] - Pure UI code, no business logic
```
app/src/main/java/com/dpart/tradeflow/presentation/
├── dashboard/DashboardScreen.kt
├── dashboard/DashboardViewModel.kt
├── dashboard/components/ModeCard.kt
├── dashboard/components/OrdersList.kt
├── dashboard/components/PortfolioCard.kt
├── dashboard/components/ServiceCard.kt
└── settings/SettingsScreen.kt
```

### 1.2 Delete Navigation Layer (3 files)
**Risk:** [safe] - Navigation-only code
```
app/src/main/java/com/dpart/tradeflow/navigation/
├── AppNavHost.kt
├── BottomNavBar.kt
└── Screen.kt
```

### 1.3 Simplify App Entry Points (2 files)
**Risk:** [safe] - Keep minimal MainActivity with "Hello" Compose screen
```
app/src/main/java/com/dpart/tradeflow/
├── MainActivity.kt - SIMPLIFY to just show "Hello" in Compose
└── TradeFlowApp.kt - KEEP (needed for Android app)
```

**Action:** Rewrite MainActivity to:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    Text("Hello")
                }
            }
        }
    }
}
```

### 1.4 Delete ENTIRE core:ui Module (11 files)
**Risk:** [safe] - Pure Compose components and theme
```
core/ui/src/main/kotlin/com/tradeflow/core/ui/
├── component/ErrorDisplay.kt
├── component/LoadingButton.kt
├── component/ModeIndicator.kt
├── component/PriceDisplay.kt
├── component/StatusCard.kt
├── theme/Color.kt
├── theme/Spacing.kt
├── theme/Theme.kt
├── theme/ThemePreview.kt
├── theme/Typography.kt
└── extension/BigDecimalExt.kt
```

### 1.5 Remove Empty Feature Modules (3 modules)
**Risk:** [safe] - No source code exists
```
feature/dashboard/  (delete entire module)
feature/trading/    (delete entire module)
feature/settings/   (delete entire module)
```

**UPDATE:** Remove from `settings.gradle.kts`:
```kotlin
// DELETE these lines:
include(":feature:dashboard")
include(":feature:trading")
include(":feature:settings")
```

**UPDATE:** Remove from root `build.gradle.kts` if referenced

### 1.6 Clean Up App Module Dependencies
**Risk:** [safe] - Keep MINIMAL Compose dependencies for "Hello" screen

**KEEP (minimal Compose):**
- `androidx.compose.ui:ui` (core)
- `androidx.compose.material3:material3` (for MaterialTheme)
- `androidx.activity:activity-compose` (for setContent)
- `androidx.compose.ui:ui-tooling-preview` (for @Preview)

**REMOVE:**
- Navigation Compose (no navigation needed)
- Timber (logging not needed)
- Lifecycle ViewModel (no ViewModels)
- ViewModel Compose integration
- Any references to `:core:ui`, `:feature:dashboard`, `:feature:trading`, `:feature:settings`

---

## PHASE 2: REMOVE DEAD CODE

### 2.1 Investigate & Possibly Delete DI Modules (2 files)
**Risk:** [moderate] - Need to verify not used by tests

Check if these are redundant:
```
app/src/main/java/com/dpart/tradeflow/di/CredentialsModule.kt
app/src/main/kotlin/com/dpart/tradeflow/di/ConfigurationModule.kt
```

**Action:**
1. Search for usages in test code
2. If used by tests, KEEP and COMMENT
3. If not used, DELETE

### 2.2 Clean Up Stub Repository (1 file)
**Risk:** [safe] - Remove TODO methods not needed for backtesting

`exchange/coinbase/repository/CoinbaseRepository.kt`

Currently has 10 TODO methods. For backtesting we only need:
- `getBalances()` - ✅ Implemented
- `getCandles()` - TODO (NEEDED for backtesting if loading real data)

**Action:**
- Keep `getBalances()` and `getCandles()`
- DELETE all other TODO methods (market orders, limit orders, cancel, bracket orders)
- Add comprehensive comments

### 2.3 Clean Up App Module (After UI deletion)
**Risk:** [safe] - May be empty after UI deletion

After deleting UI code, check if `app` module has ANY remaining source files.

**Action:**
- If app module is empty, consider deleting it entirely
- If it has DI modules needed by tests, keep those files only

---

## PHASE 3: ADD COMPREHENSIVE COMMENTS (69 files)

### 3.1 Core Domain - Configuration (8 files)
**Risk:** [safe] - Documentation only
```
core/domain/config/
├── AdaptiveOptimizer.kt - Adaptive risk profile switching logic
├── ExecutionParameters.kt - Order execution configuration
├── RiskParameters.kt - Risk limits and thresholds
├── RiskProfile.kt - Risk profiles (AGGRESSIVE, BALANCED, CONSERVATIVE, ULTRA_CONSERVATIVE)
├── StrategyParameters.kt - Trading strategy parameters
├── TechnicalParameters.kt - Technical indicator configuration (SMA, ADX, ATR)
├── TradingConfig.kt - Main configuration aggregator
└── TradingConfigDefaults.kt - Default configuration values
```

**Comment Requirements:**
- File header: Purpose, key components, usage examples
- Class KDoc: Responsibility, dependencies, lifecycle
- Method KDoc: Parameters, return values, side effects, edge cases
- Property KDoc: Purpose, valid ranges, default values

### 3.2 Core Domain - Technical Analysis (1 file)
```
core/domain/indicator/
└── TechnicalAnalysisService.kt - SMA, ADX, ATR calculations using ta4j
```

**Comment Requirements:**
- Explain single-pass optimization
- Document indicator formulas (SMA, ADX, ATR)
- Document ta4j integration
- Edge cases: insufficient candles, null handling

### 3.3 Core Domain - Models (10 files)
```
core/domain/model/
├── AuthTokenProvider.kt - JWT token generation interface
├── Balance.kt - Account balance with buy power calculations
├── Candle.kt - OHLCV candle data model
├── CredentialStore.kt - Credential storage interface
├── Decision.kt - Trading decisions (Wait, Defense, Trend, Range)
├── ExchangeError.kt - Error types (Network, Auth, RateLimit, etc.)
├── ExecutionResult.kt - Order execution results
├── Order.kt - Order model with status tracking
├── Portfolio.kt - Portfolio aggregate with P&L calculations
└── Ticker.kt - Real-time price ticker
```

**Comment Requirements:**
- Data class: Purpose of each property
- Methods: Business logic explanation
- Sealed classes: When to use each subtype
- Calculations: Formula documentation

### 3.4 Core Domain - Optimization (1 file)
```
core/domain/optimization/
└── GeneticOptimizer.kt - Genetic algorithm for parameter optimization
```

**Comment Requirements:**
- Algorithm explanation (selection, crossover, mutation)
- Fitness function details
- Parameter ranges and constraints
- Convergence criteria

### 3.5 Core Domain - Repository Interfaces (3 files)
```
core/domain/repository/
├── BracketOrderRepository.kt - Order operations interface
├── ExchangeRepository.kt - Exchange operations interface
└── ExchangeWebSocket.kt - WebSocket real-time data interface
```

**Comment Requirements:**
- Interface contract (expected behavior)
- Error handling expectations
- Threading model (suspend functions)

### 3.6 Core Domain - Risk Management (4 files)
```
core/domain/risk/
├── RiskConfig.kt - Risk configuration (max loss, position size limits)
├── RiskManager.kt - Position sizing, drawdown monitoring, validation
└── model/
    ├── DrawdownStatus.kt - Drawdown status types
    ├── PlaceOrderRequest.kt - Order request with validation
    └── RiskCheck.kt - Risk check results
```

**Comment Requirements:**
- Risk formulas (position sizing, Kelly criterion)
- Drawdown calculation methodology
- Circuit breaker logic
- Validation rules

### 3.7 Core Domain - Trading Strategy (3 files)
```
core/domain/strategy/
├── DecisionEngine.kt - Decision engine interface
├── StrategyConfig.kt - Strategy configuration
└── TradingDecisionEngine.kt - STATEFUL engine with 3-candle hysteresis
```

**Comment Requirements:**
- **CRITICAL:** Document hysteresis logic in detail
- Mode switching rules (ADX thresholds)
- State machine diagram in comments
- Edge cases: whipsaw prevention, insufficient data

### 3.8 Core Domain - Synthetic Data (3 files)
```
core/domain/synthetic/
├── JumpDiffusionGenerator.kt - Jump diffusion model for crisis scenarios
├── MarketGenerator.kt - Market data generation interface
└── StationaryBootstrapGenerator.kt - Bootstrap sampling for realistic data
```

**Comment Requirements:**
- Statistical model explanation
- Parameter interpretation
- Use cases (stress testing, optimization)

### 3.9 Core Domain - Use Cases (3 files)
```
core/domain/usecase/
├── TradeOrchestrator.kt - Main trading cycle orchestrator
├── UpdatePortfolioUseCase.kt - Portfolio state aggregation
└── model/TradingContext.kt - Trading context model
```

**Comment Requirements:**
- Orchestration flow diagram in comments
- Dependencies and call graph
- Error handling strategy
- State management

### 3.10 Core Data - All Files (15 files)
```
core/data/
├── di/ (3 files) - DI modules
├── local/dao/ (4 files) - Room DAOs
├── local/database/ (1 file) - Database setup
├── local/entity/ (4 files) - Database entities
├── mapper/ (1 file) - Entity/Domain mapping
├── repository/ (2 files) - Repository implementations
└── security/ (1 file) - Credential storage
```

**Comment Requirements:**
- Database schema documentation
- Migration strategy (future-proof)
- Thread safety notes
- Caching strategy

### 3.11 Exchange Coinbase - All Files (8 files)
```
exchange/coinbase/
├── api/CoinbaseApiClient.kt - Retrofit API client
├── auth/CoinbaseJwtGenerator.kt - JWT token generation
├── di/ (3 files) - DI modules
├── dto/AccountDto.kt - API response DTOs
├── mapper/AccountMapper.kt - DTO/Domain mapping
└── repository/CoinbaseRepository.kt - Repository implementation (partial)
```

**Comment Requirements:**
- API authentication flow
- Rate limiting considerations
- Error handling and retries
- DTO/Domain mapping rationale

### 3.12 Test Code - All Files (11 files)
```
core/domain/test/
├── optimization/ (3 files) - Optimization tests
├── risk/RiskManagerTest.kt - 22 unit tests
├── simulator/SimulatedExchange.kt - Backtesting simulator
├── strategy/ (3 files) - Strategy and backtest tests
├── synthetic/ (2 files) - Data generation tests
└── util/BinanceDataLoader.kt - Historical data loader
```

**Comment Requirements:**
- Test purpose and scenario
- Setup/teardown explanation
- Assertions rationale
- Edge cases covered

---

## PHASE 4: UPDATE BUILD FILES

### 4.1 Update settings.gradle.kts
Remove:
```kotlin
include(":feature:dashboard")
include(":feature:trading")
include(":feature:settings")
include(":core:ui")  // If we delete the entire module
```

### 4.2 Update app/build.gradle.kts
**KEEP** minimal Compose for "Hello" screen:
- `androidx.compose.ui:ui`
- `androidx.compose.material3:material3`
- `androidx.activity:activity-compose`
- `androidx.compose.ui:ui-tooling-preview`

**REMOVE** unnecessary dependencies:
- `androidx.navigation:navigation-compose`
- `timber` (UI logging only)
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `androidx.lifecycle:lifecycle-runtime-compose`

**REMOVE** module dependencies:
- `implementation(project(":core:ui"))`
- `implementation(project(":feature:dashboard"))`
- `implementation(project(":feature:trading"))`
- `implementation(project(":feature:settings"))`

### 4.3 Update root build.gradle.kts
Remove any references to deleted modules

---

## PHASE 5: VERIFICATION

### 5.1 Build Verification
```bash
./gradlew clean
./gradlew :core:domain:test
./gradlew :core:data:test
./gradlew :exchange:coinbase:test
```

### 5.2 Test Execution
Run all backtesting tests:
```bash
./gradlew :core:domain:test --tests "com.tradeflow.core.domain.strategy.*"
./gradlew :core:domain:test --tests "com.tradeflow.core.domain.optimization.*"
./gradlew :core:domain:test --tests "com.tradeflow.core.domain.synthetic.*"
```

### 5.3 Dead Code Check
After all changes, verify NO references to:
- Compose imports (`androidx.compose.*`) **EXCEPT** in MainActivity (minimal "Hello" screen)
- ViewModel imports (`androidx.lifecycle.ViewModel`)
- Navigation imports (`androidx.navigation.*`)
- Deleted modules (`:feature:*`, `:core:ui`)
- Dashboard, Settings, or other deleted UI components

---

## EXECUTION CHECKLIST

- [ ] Phase 1.1: Delete presentation layer (7 files)
- [ ] Phase 1.2: Delete navigation layer (3 files)
- [ ] Phase 1.3: Simplify MainActivity to "Hello" screen
- [ ] Phase 1.4: Delete core:ui module (11 files)
- [ ] Phase 1.5: Remove empty feature modules (3 modules)
- [ ] Phase 1.6: Clean up app dependencies (keep minimal Compose)
- [ ] Phase 2.1: Investigate DI modules (2 files)
- [ ] Phase 2.2: Clean up CoinbaseRepository stubs
- [ ] Phase 2.3: Check if app module is empty
- [ ] Phase 3.1: Comment domain/config (8 files)
- [ ] Phase 3.2: Comment domain/indicator (1 file)
- [ ] Phase 3.3: Comment domain/model (10 files)
- [ ] Phase 3.4: Comment domain/optimization (1 file)
- [ ] Phase 3.5: Comment domain/repository (3 files)
- [ ] Phase 3.6: Comment domain/risk (4 files)
- [ ] Phase 3.7: Comment domain/strategy (3 files) **CRITICAL**
- [ ] Phase 3.8: Comment domain/synthetic (3 files)
- [ ] Phase 3.9: Comment domain/usecase (3 files)
- [ ] Phase 3.10: Comment core/data (15 files)
- [ ] Phase 3.11: Comment exchange/coinbase (8 files)
- [ ] Phase 3.12: Comment test code (11 files)
- [ ] Phase 4.1: Update settings.gradle.kts
- [ ] Phase 4.2: Update app/build.gradle.kts
- [ ] Phase 4.3: Update root build.gradle.kts
- [ ] Phase 5.1: Build verification
- [ ] Phase 5.2: Test execution
- [ ] Phase 5.3: Dead code check

---

## ESTIMATED IMPACT

**Files Deleted:** 20 files + 3 modules
**Files Simplified:** 2 files (MainActivity + TradeFlowApp)
**Files Commented:** 69 files (58 main + 11 test)
**Lines to Delete:** ~1,400 lines
**Comments to Add:** ~2,000 lines (estimated)

**Net Result:** Backtesting engine with minimal "Hello" UI and comprehensive documentation

---

## RISKS & MITIGATION

### Risk 1: DI Modules Used by Tests
**Mitigation:** Search for usages before deleting

### Risk 2: Accidentally Delete Business Logic
**Mitigation:** Triple-check each file before deletion, follow plan strictly

### Risk 3: Build Breakage
**Mitigation:** Incremental commits, test after each phase

### Risk 4: Orphaned Dependencies
**Mitigation:** Clean up build.gradle.kts files thoroughly

---

## COMMIT STRATEGY

**Branch:** `claude/nuclear-cleanup`

**Commits:**
1. "Delete all complex UI code, simplify MainActivity to 'Hello'"
2. "Delete core:ui module"
3. "Remove empty feature modules"
4. "Remove dead code and stubs"
5. "Add comprehensive comments to domain/config"
6. "Add comprehensive comments to domain/indicator"
7. "Add comprehensive comments to domain/model"
8. "Add comprehensive comments to domain/optimization"
9. "Add comprehensive comments to domain/repository"
10. "Add comprehensive comments to domain/risk"
11. "Add comprehensive comments to domain/strategy (CRITICAL)"
12. "Add comprehensive comments to domain/synthetic"
13. "Add comprehensive comments to domain/usecase"
14. "Add comprehensive comments to core/data layer"
15. "Add comprehensive comments to exchange/coinbase"
16. "Add comprehensive comments to all test files"
17. "Clean up build files and dependencies"
18. "Verify build and run all tests"

---

## NEXT STEPS AFTER CLEANUP

After this cleanup, the project will be a pure backtesting engine. To run backtests:

```bash
# Run all strategy tests
./gradlew :core:domain:test --tests "com.tradeflow.core.domain.strategy.*"

# Run optimization tests
./gradlew :core:domain:test --tests "com.tradeflow.core.domain.optimization.*"

# Run specific backtest
./gradlew :core:domain:test --tests "com.tradeflow.core.domain.strategy.HistoricalBacktestTest"
```

You can then iterate on:
1. Strategy parameters (hysteresis, ADX thresholds, SMA periods)
2. Risk parameters (position sizing, max drawdown)
3. Optimization algorithms (genetic parameters)
4. Synthetic data generation (stress scenarios)

Until backtesting results are satisfactory.
