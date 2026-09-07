package com.kotomichi.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC8372E), // Shu 朱
    primaryContainer = Color(0xFFFBE5E2),
    secondary = Color(0xFF3F6B4B), // Matsuba 松葉 success
    secondaryContainer = Color(0xFFDCEADF),
    tertiary = Color(0xFFB8842A), // Yamabuki 山吹 warning
    tertiaryContainer = Color(0xFFF6E8C8),
    error = Color(0xFFA8322A), // Enji 臙脂
    errorContainer = Color(0xFFF6D9D6),
    background = Color(0xFFFAF7F2), // Washi 和紙
    surface = Color(0xFFFFFFFF), // Washi surface
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF7A1E18),
    onSecondary = Color.White,
    onSecondaryContainer = Color(0xFF1E3A28),
    onTertiary = Color.White,
    onTertiaryContainer = Color(0xFF5A3D0A),
    onError = Color.White,
    onErrorContainer = Color(0xFF5E120E),
    onBackground = Color(0xFF1C1B1A), // Sumi 墨
    onSurface = Color(0xFF1C1B1A), // Sumi 墨
    outline = Color(0xFF726E68), // Nezumi 鼠
    outlineVariant = Color(0xFFE5E0D8), // Nezumi line
    surfaceVariant = Color(0xFFF2EDE5),
    onSurfaceVariant = Color(0xFF726E68), // Nezumi 鼠
    inverseSurface = Color(0xFF302B28),
    inverseOnSurface = Color(0xFFF0EBE4),
    inversePrimary = Color(0xFFFFB4A9),
    surfaceTint = Color(0xFFC8372E)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE85C4F), // Shu 朱
    primaryContainer = Color(0xFF8A2A22),
    secondary = Color(0xFF6FAE7D), // Matsuba 松葉 success
    secondaryContainer = Color(0xFF2C4E38),
    tertiary = Color(0xFFE0A526), // Yamabuki 山吹 warning
    tertiaryContainer = Color(0xFF6B4A00),
    error = Color(0xFFD9564B), // Enji 臙脂
    errorContainer = Color(0xFF6E1A14),
    background = Color(0xFF121110), // Washi 和紙
    surface = Color(0xFF1C1B1A), // Washi surface
    onPrimary = Color(0xFF5A120C),
    onPrimaryContainer = Color(0xFFFFDAD5),
    onSecondary = Color(0xFF12351E),
    onSecondaryContainer = Color(0xFFCFE8D4),
    onTertiary = Color(0xFF3D2C00),
    onTertiaryContainer = Color(0xFFFFE3A3),
    onError = Color(0xFF440A06),
    onErrorContainer = Color(0xFFFFDAD5),
    onBackground = Color(0xFFEDEAE5), // Sumi 墨
    onSurface = Color(0xFFEDEAE5), // Sumi 墨
    outline = Color(0xFFA39E96), // Nezumi 鼠
    outlineVariant = Color(0xFF33312E), // Nezumi line
    surfaceVariant = Color(0xFF262421),
    onSurfaceVariant = Color(0xFFA39E96), // Nezumi 鼠
    inverseSurface = Color(0xFFEDEAE5),
    inverseOnSurface = Color(0xFF2B2826),
    inversePrimary = Color(0xFFC8372E),
    surfaceTint = Color(0xFFE85C4F)
)

data class KotomichiColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val text: Color,
    val textMuted: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val onPrimary: Color
)

private val LightKotomichiColors = KotomichiColors(
    primary = Color(0xFFC8372E), // Shu 朱
    background = Color(0xFFFAF7F2), // Washi 和紙
    surface = Color(0xFFFFFFFF), // Washi surface
    text = Color(0xFF1C1B1A), // Sumi 墨
    textMuted = Color(0xFF726E68), // Nezumi 鼠
    border = Color(0xFFE5E0D8), // Nezumi line
    success = Color(0xFF3F6B4B), // Matsuba 松葉
    warning = Color(0xFFB8842A), // Yamabuki 山吹
    error = Color(0xFFA8322A), // Enji 臙脂
    onPrimary = Color.White
)

private val DarkKotomichiColors = KotomichiColors(
    primary = Color(0xFFE85C4F), // Shu 朱
    background = Color(0xFF121110), // Washi 和紙
    surface = Color(0xFF1C1B1A), // Washi surface
    text = Color(0xFFEDEAE5), // Sumi 墨
    textMuted = Color(0xFFA39E96), // Nezumi 鼠
    border = Color(0xFF33312E), // Nezumi line
    success = Color(0xFF6FAE7D), // Matsuba 松葉
    warning = Color(0xFFE0A526), // Yamabuki 山吹
    error = Color(0xFFD9564B), // Enji 臙脂
    onPrimary = Color(0xFF5A120C)
)

val LocalKotomichiColors = staticCompositionLocalOf { LightKotomichiColors }

val MaterialTheme.kotomichi: KotomichiColors
    @Composable get() = LocalKotomichiColors.current

@Composable
fun KotomichiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val kotomichiColors = if (darkTheme) DarkKotomichiColors else LightKotomichiColors
    CompositionLocalProvider(LocalKotomichiColors provides kotomichiColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)