package com.kotomichi.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.kotomichi.di.get
import com.kotomichi.model.ResetPasswordRequest
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiDialog
import com.kotomichi.ui.components.KotomichiTopBar
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.usecase.AuthUseCase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val authUseCase: AuthUseCase = get()
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val handleSubmit: () -> Unit = {
        if (email.isBlank()) {
            errorMessage = "Masukkan alamat email Anda"
            showDialog = true
        } else {
            isLoading = true
            scope.launch {
                try {
                    authUseCase.resetPassword(ResetPasswordRequest(email))
                    successMessage = "Tautan reset kata sandi telah dikirim ke $email.\nPeriksa kotak masuk (dan folder spam) Anda."
                } catch (e: Exception) {
                    errorMessage = "Gagal mengirim: ${e.message}"
                } finally {
                    isLoading = false
                    showDialog = true
                }
            }
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            KotomichiTopBar(
                title = "Lupa Kata Sandi",
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                    .padding(KotomichiSpacing.xl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KotomichiCard(
                    variant = KotomichiCardVariant.Filled,
                    contentPadding = PaddingValues(KotomichiSpacing.xl)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
                    ) {
                        Text(
                            text = "Reset Kata Sandi",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Masukkan email yang terdaftar. Kami akan mengirimkan tautan untuk mengatur ulang kata sandi Anda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = handleSubmit,
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
                                Text("Kirim Tautan Reset", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        KotomichiDialog(
            title = if (successMessage != null) "Berhasil" else "Error",
            text = successMessage ?: errorMessage ?: "Terjadi kesalahan",
            confirmLabel = "OK",
            onConfirm = {
                showDialog = false
                if (successMessage != null) onBack()
            },
            onDismiss = {
                showDialog = false
                if (successMessage != null) onBack()
            }
        )
    }
}