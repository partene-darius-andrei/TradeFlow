package com.dpart.tradeflow.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dpart.tradeflow.presentation.dashboard.DashboardScreen
import com.dpart.tradeflow.presentation.login.LoginScreen
import com.dpart.tradeflow.presentation.settings.SettingsScreen
import com.tradeflow.core.domain.auth.CredentialStore
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    navController: NavHostController,
    credentialStore: CredentialStore
) {
    val scope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (startDestination != null) {
        Scaffold(
            bottomBar = {
                if (currentRoute in screensWithBottomNav) {
                    BottomNavBar(navController = navController)
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination!!,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.Dashboard.route) {
                        DashboardScreen()
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}
