package com.codebutler.farebot.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Blue Grey tonal palette
private val BlueGrey10 = Color(0xFF0E1214)
private val BlueGrey20 = Color(0xFF1C2529)
private val BlueGrey30 = Color(0xFF2B373D)
private val BlueGrey40 = Color(0xFF3B4A52)
private val BlueGrey50 = Color(0xFF4D5F69)
private val BlueGrey60 = Color(0xFF607D8B)
private val BlueGrey70 = Color(0xFF7E97A4)
private val BlueGrey80 = Color(0xFF9FB1BC)
private val BlueGrey90 = Color(0xFFC1CDD4)
private val BlueGrey95 = Color(0xFFDDE4E8)
private val BlueGrey99 = Color(0xFFF6F8FA)

// Secondary: deeper blue grey
private val SecondaryDark = Color(0xFF455A64)
private val SecondaryLight = Color(0xFFB0BEC5)

// Tertiary: warm accent (muted amber) for contrast
private val Tertiary40 = Color(0xFF8B6E47)
private val Tertiary80 = Color(0xFFD4B896)
private val Tertiary90 = Color(0xFFEEDCC8)
private val TertiaryDark20 = Color(0xFF3D2E1A)

// Error colors
private val Error40 = Color(0xFFBA1A1A)
private val Error80 = Color(0xFFFFB4AB)
private val Error90 = Color(0xFFFFDAD6)
private val ErrorDark20 = Color(0xFF690005)

private val LightColorScheme = lightColorScheme(
    primary = BlueGrey60,
    onPrimary = Color.White,
    primaryContainer = BlueGrey90,
    onPrimaryContainer = BlueGrey10,
    inversePrimary = BlueGrey80,
    secondary = SecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = BlueGrey95,
    onSecondaryContainer = BlueGrey20,
    tertiary = Tertiary40,
    onTertiary = Color.White,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = TertiaryDark20,
    background = BlueGrey99,
    onBackground = BlueGrey10,
    surface = BlueGrey99,
    onSurface = BlueGrey10,
    surfaceVariant = BlueGrey95,
    onSurfaceVariant = BlueGrey40,
    surfaceTint = BlueGrey60,
    inverseSurface = BlueGrey20,
    inverseOnSurface = BlueGrey95,
    outline = BlueGrey50,
    outlineVariant = BlueGrey90,
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = ErrorDark20,
    scrim = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueGrey80,
    onPrimary = BlueGrey20,
    primaryContainer = BlueGrey40,
    onPrimaryContainer = BlueGrey90,
    inversePrimary = BlueGrey60,
    secondary = SecondaryLight,
    onSecondary = BlueGrey20,
    secondaryContainer = BlueGrey30,
    onSecondaryContainer = BlueGrey90,
    tertiary = Tertiary80,
    onTertiary = TertiaryDark20,
    tertiaryContainer = Tertiary40,
    onTertiaryContainer = Tertiary90,
    background = BlueGrey10,
    onBackground = BlueGrey90,
    surface = BlueGrey10,
    onSurface = BlueGrey90,
    surfaceVariant = BlueGrey30,
    onSurfaceVariant = BlueGrey80,
    surfaceTint = BlueGrey80,
    inverseSurface = BlueGrey90,
    inverseOnSurface = BlueGrey20,
    outline = BlueGrey70,
    outlineVariant = BlueGrey40,
    error = Error80,
    onError = ErrorDark20,
    errorContainer = Error40,
    onErrorContainer = Error90,
    scrim = Color.Black,
)

@Composable
fun FareBotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}
