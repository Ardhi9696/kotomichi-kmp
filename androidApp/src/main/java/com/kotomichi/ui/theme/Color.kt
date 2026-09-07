package com.kotomichi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC8372E), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD4), onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF77574A), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCB), onSecondaryContainer = Color(0xFF2C160B),
    tertiary = Color(0xFF39608F), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E4FF), onTertiaryContainer = Color(0xFF001C38),
    error = Color(0xFF9C3A2E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD4), onErrorContainer = Color(0xFF410001),
    background = Color(0xFFFFF8F6), onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F6), onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DED9), onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857370), outlineVariant = Color(0xFFD8C2BE),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4A8), onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A), onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFE7BEAC), onSecondary = Color(0xFF44291D),
    secondaryContainer = Color(0xFF5D3F32), onSecondaryContainer = Color(0xFFFFDBCB),
    tertiary = Color(0xFFA3C9FE), onTertiary = Color(0xFF00325A),
    tertiaryContainer = Color(0xFF1C4775), onTertiaryContainer = Color(0xFFD3E4FF),
    error = Color(0xFFFFB4A8), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD4),
    background = Color(0xFF1A1110), onBackground = Color(0xFFF1DFDB),
    surface = Color(0xFF1A1110), onSurface = Color(0xFFF1DFDB),
    surfaceVariant = Color(0xFF534341), onSurfaceVariant = Color(0xFFD8C2BE),
    outline = Color(0xFFA08C88), outlineVariant = Color(0xFF534341),
)

data class ExtendedColorScheme(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColorScheme(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)
}

val LightExtendedColors = ExtendedColorScheme(
    success = Color(0xFF3F6B4B),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFC2F0CB),
    onSuccessContainer = Color(0xFF002109)
)

val DarkExtendedColors = ExtendedColorScheme(
    success = Color(0xFFA0D2A8),
    onSuccess = Color(0xFF0A3915),
    successContainer = Color(0xFF23512D),
    onSuccessContainer = Color(0xFFBCEEC4)
)

val MaterialTheme.extendedColors: ExtendedColorScheme
    @Composable get() = LocalExtendedColors.current