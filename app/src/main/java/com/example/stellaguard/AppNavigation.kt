package com.example.stellaguard

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stellaguard.auth.AuthViewModel
import com.example.stellaguard.auth.LoginScreen
import com.example.stellaguard.auth.RegisterScreen
import com.example.stellaguard.home.HomeScreen
import com.example.stellaguard.home.LeaderboardScreen // NOVI IMPORT

// PROMENA 1: Dodajemo novu konstantu za rutu
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val LEADERBOARD = "leaderboard" // NOVA RUTA
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()

    val startDestination = if (authViewModel.isUserLoggedIn()) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                // PROMENA 2: Povezujemo akciju iz HomeScreen-a sa navigacijom
                onNavigateToLeaderboard = { navController.navigate(Routes.LEADERBOARD) }
            )
        }

        // PROMENA 3: Definišemo novi ekran u navigacionom grafu
        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}