# 📱 APP: Main Application & Navigation

Effort level: Medium
Priority: High
Status: Not started
Blocked by: FEATURE: Dashboard ViewModel (Logic), FEATURE: Settings ViewModel (Logic)
Module: :app

## Objective

Set up the main application with Hilt DI and navigation.

## Module

`:app`

## Application Class

```kotlin
@HiltAndroidApp
class TradeFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize logging, crash reporting, etc.
    }
}
```

## Hilt Modules

### ExchangeModule (Binds active exchange)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ExchangeModule {
    
    @Binds
    @Singleton
    abstract fun bindExchangeRepository(
        impl: CoinbaseRepository
    ): ExchangeRepository
    
    @Binds
    @Singleton
    abstract fun bindBracketOrderRepository(
        impl: CoinbaseRepository
    ): BracketOrderRepository
    
    @Binds
    @Singleton
    abstract fun bindExchangeWebSocket(
        impl: CoinbaseWebSocket
    ): ExchangeWebSocket
    
    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(
        impl: CoinbaseJwtGenerator
    ): AuthTokenProvider
    
    @Binds
    @Singleton
    abstract fun bindCredentialStore(
        impl: SecureCredentialStore
    ): CredentialStore
}
```

### DomainModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    
    @Provides
    @Singleton
    fun provideDecisionEngine(): DecisionEngine = TradingDecisionEngine()
    
    @Provides
    @Singleton
    fun provideRiskManager(): RiskManager = TradingRiskManager()
}
```

### DatabaseModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TradeFlowDatabase =
        Room.databaseBuilder(
            context,
            TradeFlowDatabase::[class.java](http://class.java),
            "tradeflow.db"
        ).build()
    
    @Provides
    fun provideOrderDao(db: TradeFlowDatabase): OrderDao = db.orderDao()
    
    @Provides
    fun providePortfolioDao(db: TradeFlowDatabase): PortfolioDao = db.portfolioDao()
}
```

## Navigation

```kotlin
@Composable
fun TradeFlowNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController, startDestination = "dashboard") {
        composable("dashboard") {
            val viewModel: DashboardViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            DashboardScreen(
                state = state,
                onStartEngine = viewModel::startEngine,
                onStopEngine = viewModel::stopEngine,
                onEmergencyStop = viewModel::emergencyStop,
                onCancelOrder = viewModel::cancelOrder,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SettingsScreen(
                state = state,
                onApiKeyChange = viewModel::updateApiKey,
                onPrivateKeyChange = viewModel::updatePrivateKey,
                onProductChange = viewModel::updateProduct,
                onSave = viewModel::saveCredentials,
                onTestConnection = viewModel::testConnection,
                onClearCredentials = viewModel::clearCredentials,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

## File Structure

```
app/src/main/kotlin/com/tradeflow/
├── TradeFlowApplication.kt
├── MainActivity.kt
├── navigation/
│   └── TradeFlowNavGraph.kt
└── di/
    ├── ExchangeModule.kt
    ├── DomainModule.kt
    └── DatabaseModule.kt
```

## Key Point: Exchange Binding

The `:app` module is the ONLY place that knows about `:exchange:coinbase`.

To swap exchanges, only change `ExchangeModule.kt`.

## Acceptance Criteria

- [ ]  Hilt DI configured correctly
- [ ]  Navigation works between screens
- [ ]  Exchange implementation swappable via DI
- [ ]  App launches and shows dashboard