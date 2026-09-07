package com.kotomichi.ui.auth

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.imePadding
import com.kotomichi.di.get
import com.kotomichi.model.LoginRequest
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiDialog
import com.kotomichi.ui.components.KotomichiTopBar
import com.kotomichi.ui.components.KotomichiTopBarVariant
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.usecase.AuthUseCase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val authUseCase: AuthUseCase = get()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    val handleLogin: () -> Unit = {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Email dan kata sandi wajib diisi"
            showErrorDialog = true
        } else {
            isLoading = true
            errorMessage = null

            scope.launch {
                try {
                    authUseCase.login(LoginRequest(email, password))
                    onLoginSuccess()
                } catch (e: Exception) {
                    errorMessage = "Login gagal: ${e.message}"
                    showErrorDialog = true
                } finally {
                    isLoading = false
                }
            }
        }
    }

    BackHandler {
        showExitDialog = true
    }

    Scaffold(
        topBar = {
            KotomichiTopBar(
                title = "Masuk ke Kotomichi",
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(KotomichiSpacing.xl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KotomichiCard(
                    variant = KotomichiCardVariant.Filled,
                    contentPadding = PaddingValues(KotomichiSpacing.xl)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Kotomichi",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "言道 - Jalan Kata",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(KotomichiSpacing.sm))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorMessage != null && email.isNotBlank()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Kata Sandi") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (showPassword) "Sembunyikan kata sandi" else "Tampilkan kata sandi"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { handleLogin() }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorMessage != null && password.isNotBlank()
                        )

                        Button(
                            onClick = handleLogin,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(KotomichiDimens.buttonSpinnerSize),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = KotomichiDimens.buttonSpinnerStroke
                                )
                            } else {
                                Text("Masuk", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Belum punya akun? ")
                            TextButton(onClick = onNavigateToRegister) {
                                Text("Daftar", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        TextButton(onClick = onNavigateToForgotPassword) {
                            Text("Lupa Kata Sandi?", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    if (showErrorDialog) {
        KotomichiDialog(
            title = "Error",
            text = errorMessage ?: "Terjadi kesalahan",
            confirmLabel = "OK",
            onConfirm = { showErrorDialog = false },
            onDismiss = { showErrorDialog = false }
        )
    }

    if (showExitDialog) {
        KotomichiDialog(
            title = "Keluar Aplikasi",
            text = "Yakin ingin keluar dari aplikasi?",
            confirmLabel = "Keluar",
            dismissLabel = "Batal",
            isDestructive = true,
            onConfirm = {
                showExitDialog = false
                (context as? Activity)?.finishAffinity()
            },
            onDismiss = { showExitDialog = false }
        )
    }
}