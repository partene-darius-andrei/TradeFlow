# Coinbase API Testing Plan

**Goal:** Test that Coinbase authentication works and we can fetch real data from the API.

**Current Status:**
- ✅ Dashboard UI skeleton complete (Ticket 10)
- ✅ JWT token generator complete (ES256 signing)
- ✅ Credentials injection system complete (BuildConfig)
- ❌ No REST API implementation yet
- ❌ No way to fetch real data from Coinbase

---

## What's Needed for Minimal Testing

### Option 1: Full REST API Client (Recommended)

**Ticket:** 13 - REST API Client
**Module:** `:exchange:coinbase`
**Status:** In backlog, needs to be moved to "refined"

**What it provides:**
- Complete `CoinbaseRepository` implementing `ExchangeRepository`
- All essential endpoints:
  - `GET /api/v3/brokerage/accounts` - Get account balances
  - `GET /api/v3/brokerage/products/{id}/candles` - Get historical data
  - `POST /api/v3/brokerage/orders` - Place orders
  - `GET /api/v3/brokerage/orders/historical/batch` - List orders
- Rate limiting and error handling
- Domain model mapping

**Benefits:**
- Production-ready implementation
- Tests authentication thoroughly
- Can fetch real portfolio data
- Can display real prices
- Foundation for all future features

**Implementation effort:** Large (as noted in ticket)

---

### Option 2: Minimal Test Endpoint (Quick & Dirty)

**NOT A TICKET** - Just a quick test implementation

**What to add:**
```kotlin
// In :exchange:coinbase module
class CoinbaseTestApi @Inject constructor(
    private val httpClient: HttpClient,
    private val authProvider: AuthTokenProvider
) {
    suspend fun testAuthentication(): Result<AccountsResponse> = runCatching {
        val path = "/api/v3/brokerage/accounts"
        val token = authProvider.generateRestToken("GET", path)

        httpClient.get("https://api.coinbase.com$path") {
            header("Authorization", "Bearer $token")
        }.body()
    }
}
```

**Add test button to Dashboard:**
- Add "Test Coinbase API" button to ServiceCard
- On click, call `testAuthentication()`
- Display result (success/error) in UI

**Benefits:**
- Very quick to implement (~30 minutes)
- Tests authentication immediately
- Can verify credentials work
- Shows real account data

**Drawbacks:**
- Throwaway code
- Doesn't follow architecture
- Will be replaced by full REST client anyway

---

## Recommendation

**Pull Ticket 13 into "refined" status** and implement the full REST API client.

**Reasoning:**
- JWT generator is already done (blocker complete)
- We need this for the app to work anyway
- Better to do it right once than quick test + full implementation
- Can test auth while building production code
- Dashboard can be updated incrementally as endpoints become available

**Testing approach:**
1. Implement `getAccounts()` endpoint first (simplest)
2. Add simple "Fetch Portfolio" button to dashboard
3. Display result to verify auth works
4. Continue implementing other endpoints
5. Gradually replace mock data with real API calls

---

## Next Steps

**If going with Option 1 (Recommended):**
1. Move `13-rest-api-client.md` from `backlog/` to `refined/`
2. Start implementation with `getAccounts()` endpoint
3. Test auth with real API call
4. Implement remaining endpoints
5. Update Dashboard to use real data

**If going with Option 2 (Quick test):**
1. Create `CoinbaseTestApi.kt` in `:exchange:coinbase`
2. Add DI binding in `AuthModule`
3. Inject into MainActivity or create simple test screen
4. Add "Test API" button
5. Verify auth works
6. Then proceed with Ticket 13

---

## Current Blockers for Testing

**None!** All prerequisites are complete:
- ✅ JWT token generator (Ticket 07-JWT)
- ✅ Credential injection (BuildConfig)
- ✅ Ktor HTTP client configured
- ✅ Hilt DI setup
- ✅ Domain models defined

**Only missing:** The actual REST API implementation (Ticket 13)
