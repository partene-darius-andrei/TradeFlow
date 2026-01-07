# 🧭 App Navigation Setup

**Ticket:** 24
**Module:** `:app`
**Priority:** HIGH
**Effort:** Medium
**Status:** Ready for Implementation
**Blocked by:** 21 (Theme), 23 (Login), 25 (Dashboard), 26 (Settings)
**Blocks:** None (enables all screen navigation)

---

## Objective

Set up Jetpack Compose Navigation with:
- NavHost configuration
- Bottom navigation bar (Dashboard, Settings)
- Login flow (check credentials → Login or Dashboard)
- Screen routes and deep linking structure

---

## Context

TradeFlow has simple navigation:
1. **Check credentials** on app launch
2. **Show Login** if no credentials exist
3. **Show Dashboard** if credentials exist
4. **Bottom navigation** between Dashboard and Settings

**Navigation graph:**
```
App Launch
    │
    ├─ No credentials? → LoginScreen
    │                       │
    │                       └─ Save success → Dashboard
    │
    └─ Has credentials? → Dashboard
                             │
                             └─ Bottom Nav ↔ Settings
```

---

## Files to Create

```
app/src/main/java/com/dpart/tradeflow/
├── navigation/
│   ├── Screen.kt           # Sealed class for routes
│   ├── AppNavHost.kt       # NavHost setup
│   └── BottomNavBar.kt     # Bottom navigation UI
└── MainActivity.kt         # Update to use AppNavHost
```

---

## Implementation

### 1. Screen Routes (Screen.kt)

```kotlin
package com.dpart.tradeflow.navigation

/**
 * Sealed class representing all app screens
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object Settings : Screen("settings")
}

/**
 * Screens that should show bottom navigation
 */
val screensWithBottomNav = setOf(
    Screen.Dashboard.route,
    Screen.Settings.route
)
```

---

### 2. Bottom Navigation Bar (BottomNavBar.kt)

```kotlin
package com.dpart.tradeflow.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Bottom navigation item data
 */
data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

/**
 * Bottom navigation items
 */
val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Dashboard,
        icon = Icons.Default.Dashboard,
        label = "Dashboard"
    ),
    BottomNavItem(
        screen = Screen.Settings,
        icon = Icons.Default.Settings,
        label = "Settings"
    )
)

/**
 * Bottom navigation bar component
 */
@Composable
fun BottomNavBar(
    navController: NavController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = currentRoute == item.screen.route,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            // Pop up to start destination to avoid stack buildup
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            // Avoid multiple copies of same destination
                            launchSingleTop = true
                            // Restore state when navigating back
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
```

---

### 3. NavHost Setup (AppNavHost.kt)

```kotlin
package com.dpart.tradeflow.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dpart.tradeflow.presentation.dashboard.DashboardScreen
import com.dpart.tradeflow.presentation.login.LoginScreen
import com.dpart.tradeflow.presentation.settings.SettingsScreen
import com.tradeflow.core.domain.auth.CredentialStore
import com.tradeflow.core.ui.theme.TradeFlowTheme
import kotlinx.coroutines.launch

/**
 * Main app navigation host
 */
@Composable
fun AppNavHost(
    credentialStore: CredentialStore,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Determine start destination based on credentials
    LaunchedEffect(Unit) {
        scope.launch {
            val hasCredentials = credentialStore.hasCredentials()
            startDestination = if (hasCredentials) {
                Screen.Dashboard.route
            } else {
                Screen.Login.route
            }
        }
    }

    // Wait for start destination to be determined
    if (startDestination == null) {
        // Show loading or splash screen
        return
    }

    TradeFlowTheme {
        AppScaffold(
            navController = navController,
            startDestination = startDestination!!
        )
    }
}

/**
 * Scaffold with conditional bottom navigation
 */
@Composable
private fun AppScaffold(
    navController: NavHostController,
    startDestination: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Show bottom nav only on Dashboard and Settings
            if (currentRoute in screensWithBottomNav) {
                BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Login screen
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            // Remove login from back stack
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Dashboard screen
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            // Settings screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            // Clear back stack on logout
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
```

---

### 4. Update MainActivity

```kotlin
package com.dpart.tradeflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dpart.tradeflow.navigation.AppNavHost
import com.tradeflow.core.domain.auth.CredentialStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var credentialStore: CredentialStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavHost(credentialStore = credentialStore)
        }
    }
}
```

---

## Testing

### Manual Testing Checklist

1. **First Launch (No Credentials):**
   - [ ] App shows Login screen
   - [ ] No bottom navigation visible
   - [ ] After login success, navigates to Dashboard
   - [ ] Back button does NOT go back to Login

2. **Subsequent Launch (Has Credentials):**
   - [ ] App shows Dashboard directly
   - [ ] Bottom navigation visible
   - [ ] Can navigate between Dashboard and Settings

3. **Bottom Navigation:**
   - [ ] Clicking Dashboard navigates to Dashboard
   - [ ] Clicking Settings navigates to Settings
   - [ ] Selected item is highlighted
   - [ ] Icons and labels display correctly

4. **Logout Flow:**
   - [ ] Logout in Settings navigates to Login
   - [ ] Back button does NOT go back to Settings
   - [ ] Bottom navigation hidden on Login screen

5. **State Management:**
   - [ ] Dashboard state preserved when navigating to Settings
   - [ ] Settings state preserved when navigating to Dashboard
   - [ ] Rotation preserves navigation state

---

## Acceptance Criteria

- [ ] Navigation works between all screens
- [ ] Bottom navigation shows on Dashboard and Settings only
- [ ] Start destination determined by credential existence
- [ ] Login success navigates to Dashboard
- [ ] Logout navigates to Login and clears back stack
- [ ] Back button behavior is correct (no back to Login after success)
- [ ] Screen state is preserved during navigation
- [ ] No navigation bugs or crashes

---

## Notes

### Navigation Best Practices

1. **Single Activity:** All screens in one Activity (modern Android pattern)
2. **Type-safe routes:** Using sealed class prevents typos
3. **State preservation:** `saveState` and `restoreState` keep screen state
4. **Back stack management:** `popUpTo` prevents unwanted back navigation
5. **Bottom nav pattern:** Material 3 best practice for 2-5 top-level destinations

### Future Enhancements

When more screens are added:
- **Order details:** Deep link from notification
- **Chart view:** Full-screen chart from Dashboard
- **Logs viewer:** From Settings → Logs

Routes would become:
```kotlin
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object Settings : Screen("settings")
    data object OrderDetails : Screen("order/{orderId}") // Future
    data object Logs : Screen("logs")                   // Future
}
```

---

## Dependencies

```kotlin
// app/build.gradle.kts
implementation(libs.androidx.navigation.compose)
implementation(libs.hilt.navigation.compose)
implementation(project(":core:domain"))  // CredentialStore
implementation(project(":core:ui"))      // Theme
```

**Version (from libs.versions.toml):**
```toml
[versions]
androidxNavigation = "2.8.5"

[libraries]
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "androidxNavigation" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }
```

---

## Related Tickets

- **Blocked by:** 21 (Theme), 23 (Login), 25 (Dashboard), 26 (Settings)
- **Integrates:** All presentation layer tickets
- **Reference:** 20 (UI Design Overview)
