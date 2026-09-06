package com.kotomichi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kotomichi.ui.common.KotomichiTheme
import com.kotomichi.navigation.AppNavHost
import com.kotomichi.di.get
import com.kotomichi.usecase.AuthUseCase
import kotlinx.coroutines.flow.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val authUseCase: AuthUseCase by viewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotomichiTheme {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isAuthenticated by authUseCase.isAuthenticated.collectAsStateWithLifecycle(false)
                    AppNavHost(isAuthenticated = isAuthenticated)
                }
            }
        }
    }
}