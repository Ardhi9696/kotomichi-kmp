package com.kotomichi.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kotomichi.ui.auth.LoginScreen
import com.kotomichi.ui.auth.RegisterScreen
import com.kotomichi.ui.dashboard.DashboardScreen
import com.kotomichi.ui.learn.LearnScreen
import com.kotomichi.ui.review.ReviewScreen

@Composable
fun AppNavHost(isAuthenticated: Boolean) {
    val navController = rememberNavController()
    val startRoute = if (isAuthenticated) "dashboard" else "login"
    
    NavHost(navController, startDestination = startRoute) {
        composable("login") {
            LoginScreen(onLoginSuccess = { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } })
        }
        composable("register") {
            RegisterScreen(onRegisterSuccess = { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } })
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateToLearn = { deckId -> navController.navigate("learn/$deckId") },
                onNavigateToReview = { navController.navigate("review") }
            )
        }
        composable(
            route = "learn/{deckId}",
            arguments = listOf(androidx.navigation.navArgument("deckId") { type = androidx.navigation.NavType.LongType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: 0L
            LearnScreen(deckId = deckId, onComplete = { navController.navigateUp() })
        }
        composable("review") {
            ReviewScreen(onComplete = { navController.navigateUp() })
        }
    }
}