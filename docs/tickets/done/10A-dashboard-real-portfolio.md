# 📊 Dashboard - Real Portfolio Data from Coinbase

**Ticket:** 10A
**Module:** `:app`, `:exchange:coinbase`
**Priority:** HIGH
**Effort:** Medium
**Status:** Ready for Implementation
**Blocked by:** 10 (Dashboard UI)
**Blocks:** Full REST API Client (Ticket 13)

---

## Objective

Connect Dashboard to real Coinbase API to display actual portfolio data instead of mock values.

**Scope:**
- Implement minimal Coinbase REST API (accounts endpoint only)
- Create ViewModel for Dashboard state management
- Update PortfolioCard to show real balances
- Test authentication with live API

---

## Context

Dashboard UI is complete with mock data. Now we need to:
1. Verify JWT authentication works with real Coinbase API
2. Fetch actual account balances
3. Display real portfolio value

This is a **minimal implementation** focused on:
- Testing auth works
- Getting real data on screen
- Foundation for full REST client (Ticket 13)

---

## Implementation Plan

### Phase 1: Coinbase API Client (Minimal)

**Files to create:**
```
exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
├── api/
│   └── CoinbaseApiClient.kt           # Ktor HTTP client wrapper
├── dto/
│   ├── AccountDto.kt                  # Account response DTO
│   └── AccountsResponseDto.kt         # Wrapper for accounts list
├── mapper/
│   └── AccountMapper.kt               # DTO → Domain mapping
└── repository/
    └── CoinbaseRepository.kt          # Implements ExchangeRepository (partial)
```

**Implementation:**

#### 1. DTOs (AccountDto.kt)
```kotlin
@Serializable
data class AccountsResponseDto(
    val accounts: List<AccountDto>,
    val has_next: Boolean,
    val cursor: String?,
    val size: Int
)

@Serializable
data class AccountDto(
    val uuid: String,
    val name: String,
    val currency: String,
    val available_balance: AvailableBalanceDto,
    val default: Boolean,
    val active: Boolean,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String?,
    val type: String,
    val ready: Boolean,
    val hold: AvailableBalanceDto
)

@Serializable
data class AvailableBalanceDto(
    val value: String,
    val currency: String
)
```

#### 2. Mapper (AccountMapper.kt)
```kotlin
fun AccountDto.toDomain(): Balance {
    return Balance(
        currency = currency,
        available = BigDecimal(available_balance.value),
        hold = BigDecimal(hold.value)
    )
}
```

#### 3. API Client (CoinbaseApiClient.kt)
```kotlin
class CoinbaseApiClient @Inject constructor(
    private val httpClient: HttpClient,
    private val authProvider: AuthTokenProvider,
    private val json: Json
) {
    private val baseUrl = "https://api.coinbase.com"

    suspend fun getAccounts(): Result<AccountsResponseDto> = runCatching {
        val path = "/api/v3/brokerage/accounts"
        val token = authProvider.generateRestToken("GET", path)

        httpClient.get("$baseUrl$path") {
            header("Authorization", "Bearer $token")
        }.body()
    }
}
```

#### 4. Repository (CoinbaseRepository.kt)
```kotlin
class CoinbaseRepository @Inject constructor(
    private val apiClient: CoinbaseApiClient
) : ExchangeRepository {

    override suspend fun getBalances(): Result<List<Balance>> = runCatching {
        val response = apiClient.getAccounts().getOrThrow()
        response.accounts
            .filter { it.active && it.ready }
            .map { it.toDomain() }
    }

    // Other methods throw NotImplementedError for now
    override suspend fun getCandles(...) = TODO("Ticket 13")
    override suspend fun placeBracketOrder(...) = TODO("Ticket 13")
    // ... etc
}
```

#### 5. DI Module (ExchangeModule.kt)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ExchangeModule {

    @Provides
    @Singleton
    fun provideCoinbaseApiClient(
        httpClient: HttpClient,
        authProvider: AuthTokenProvider,
        json: Json
    ): CoinbaseApiClient = CoinbaseApiClient(httpClient, authProvider, json)

    @Provides
    @Singleton
    fun provideExchangeRepository(
        apiClient: CoinbaseApiClient
    ): ExchangeRepository = CoinbaseRepository(apiClient)
}
```

---

### Phase 2: Dashboard ViewModel

**File:** `app/src/main/java/com/dpart/tradeflow/presentation/dashboard/DashboardViewModel.kt`

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ExchangeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getBalances()
                .onSuccess { balances ->
                    val btc = balances.find { it.currency == "BTC" }
                    val usd = balances.find { it.currency == "USD" }

                    _uiState.update { it.copy(
                        isLoading = false,
                        btcBalance = btc?.available ?: BigDecimal.ZERO,
                        usdBalance = usd?.available ?: BigDecimal.ZERO,
                        totalValue = calculateTotalValue(btc, usd)
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load portfolio"
                    )}
                }
        }
    }

    private fun calculateTotalValue(btc: Balance?, usd: Balance?): BigDecimal {
        // For now, just return USD balance
        // TODO: Fetch BTC price and calculate total
        return usd?.available ?: BigDecimal.ZERO
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val btcBalance: BigDecimal = BigDecimal.ZERO,
    val usdBalance: BigDecimal = BigDecimal.ZERO,
    val totalValue: BigDecimal = BigDecimal.ZERO
)
```

---

### Phase 3: Update Dashboard UI

**Update DashboardScreen.kt:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TradeFlow") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = TradeFlowSpacing.md)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(TradeFlowSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(TradeFlowSpacing.sm))

            if (uiState.isLoading) {
                LoadingIndicator()
            } else if (uiState.error != null) {
                ErrorDisplay(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadPortfolio() }
                )
            } else {
                PortfolioCard(
                    btcBalance = uiState.btcBalance,
                    usdBalance = uiState.usdBalance,
                    totalValue = uiState.totalValue
                )
            }

            ModeCard()
            ServiceCard()
            OrdersList()

            Spacer(modifier = Modifier.height(TradeFlowSpacing.md))
        }
    }
}
```

**Update PortfolioCard.kt:**
```kotlin
@Composable
fun PortfolioCard(
    btcBalance: BigDecimal,
    usdBalance: BigDecimal,
    totalValue: BigDecimal
) {
    StatusCard(title = "Portfolio") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PriceDisplay(
                price = totalValue,
                previousPrice = totalValue, // No change for now
                style = MaterialTheme.typography.displayMedium
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Live Data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(TradeFlowSpacing.md))

        AssetRow(
            asset = "BTC",
            amount = btcBalance.toPlainString(),
            value = "—" // TODO: Calculate USD value
        )
        AssetRow(
            asset = "USD",
            amount = "—",
            value = "$${usdBalance.formatCurrency()}"
        )
    }
}
```

---

## Testing Checklist

### Manual Testing
1. **Build succeeds:**
   - [ ] No compilation errors
   - [ ] All imports resolve correctly

2. **Authentication:**
   - [ ] JWT token generates without errors
   - [ ] API call returns 200 (not 401/403)

3. **Data display:**
   - [ ] Real BTC balance displays
   - [ ] Real USD balance displays
   - [ ] Loading state shows while fetching
   - [ ] Error state shows if API fails

4. **Error handling:**
   - [ ] Invalid credentials show error
   - [ ] Network errors handled gracefully
   - [ ] Retry button works

---

## Acceptance Criteria

- [ ] Coinbase API client authenticates successfully
- [ ] Real account balances display in PortfolioCard
- [ ] Loading state shows during API call
- [ ] Error state shows with retry button on failure
- [ ] No hardcoded mock data in portfolio display
- [ ] Build passes on CI/CD
- [ ] Ready for PR review

---

## Notes

### What This Ticket Does
- Implements **minimal** Coinbase API (accounts endpoint only)
- Tests authentication with real API
- Shows real portfolio data on Dashboard

### What This Ticket Does NOT Do
- Full REST API implementation (that's Ticket 13)
- WebSocket real-time updates (that's Ticket 14)
- Price fetching for BTC/USD conversion
- Historical candles
- Order placement

### Future Work
After this ticket, Ticket 13 will expand CoinbaseRepository with:
- Candle fetching
- Order placement/cancellation
- Order listing
- Full error handling
- Rate limiting

---

## Dependencies

**Existing:**
- ✅ JWT generator (Ticket 07-JWT)
- ✅ Credential injection (BuildConfig)
- ✅ Ktor HTTP client
- ✅ Domain models (Balance)
- ✅ Dashboard UI skeleton (Ticket 10)

**New:**
- Kotlinx Serialization (already in project)
- Hilt ViewModel (already in project)

---

## Related Tickets

- **Builds on:** 10 (Dashboard UI Skeleton)
- **Unblocks:** 13 (Full REST API Client)
- **Tests:** 07-JWT (JWT Generator)
