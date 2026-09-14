package com.overscroll.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.overscroll.app.ui.history.HistoryScreen
import com.overscroll.app.ui.home.HomeScreen
import com.overscroll.app.ui.onboarding.OnboardingScreen
import com.overscroll.app.ui.onboarding.checkPermissions
import com.overscroll.app.ui.settings.SettingsScreen

/**
 * Top-level navigation graph.
 *
 * Start destination is determined by permission state:
 * - If all permissions are granted → go directly to Home
 * - Otherwise → show Onboarding first
 */
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val initialPermissions = remember { checkPermissions(context) }
    val startDestination = if (initialPermissions.allGranted) {
        NavRoutes.HOME
    } else {
        NavRoutes.ONBOARDING
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                onAllPermissionsGranted = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
            )
        }

        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

/** Route constants for type safety */
object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}
