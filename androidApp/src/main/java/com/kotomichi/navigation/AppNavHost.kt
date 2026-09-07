package com.kotomichi.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kotomichi.ui.auth.ForgotPasswordScreen
import com.kotomichi.ui.auth.LoginScreen
import com.kotomichi.ui.auth.RegisterScreen
import com.kotomichi.ui.learn.LearnScreen
import com.kotomichi.ui.main.MainScreen
import com.kotomichi.ui.review.ReviewScreen

@Composable
fun AppNavHost(isAuthenticated: Boolean) {
    val navController = rememberNavController()
    val startRoute = if (isAuthenticated) "main" else "login"

    LaunchedEffect(isAuthenticated) {
        val current = navController.currentBackStackEntry?.destination?.route
        if (isAuthenticated && (current == "login" || current == "register")) {
            navController.navigate("main") {
                popUpTo("login") { inclusive = true }
            }
        } else if (!isAuthenticated) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("main") { popUpTo("login") { inclusive = true } } },
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate("main") { popUpTo("login") { inclusive = true } } },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable("main") {
            MainScreen(
                onNavigateToLearnDeck = { deckId -> navController.navigate("learn/$deckId") },
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