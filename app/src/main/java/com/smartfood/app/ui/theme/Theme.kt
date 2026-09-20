package com.smartfood.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = GreenDark,
    onPrimary = White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,

    secondary = OrangeMain,
    onSecondary = White,
    secondaryContainer = OrangeLight,
    onSecondaryContainer = OrangeDark,

    background = GreenDark,
    onBackground = White,

    surface = SurfaceLight,
    onSurface = GreenDark,
    surfaceVariant = White,
    onSurfaceVariant = GreyNeutral,

    outline = GreyDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenMain,
    onPrimary = White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,

    secondary = OrangeMain,
    onSecondary = White,
    secondaryContainer = OrangeDark,
    onSecondaryContainer = OrangeLight,

    background = GreenDark,
    onBackground = White,

    surface = SurfaceDark,
    onSurface = White,
    surfaceVariant = InputDark,
    onSurfaceVariant = TextMuted,

    outline = BorderDark
)

@Composable
fun SmartfoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Desactivado por defecto para mantener consistencia con la marca
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
