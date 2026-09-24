package com.smartfood.app.ui.theme

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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

    outline = GreyDivider,

    error = ErrorRed,
    onError = White
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

    outline = BorderDark,

    error = ErrorRed,
    onError = White
)

@Composable
fun SmartfoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    val context = LocalContext.current

    // Adapta automáticamente los colores de las barras de estado (TopBar) y navegación (NavBar)
    DisposableEffect(darkTheme) {
        val activity = context as? ComponentActivity
        activity?.enableEdgeToEdge(
            // TopBar (Status Bar): Siempre con íconos claros (SurfaceLight style) sobre el fondo verde oscuro
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            // NavBar (Navigation Bar): 
            // - En modo oscuro: Íconos claros (SurfaceLight) sobre fondo oscuro
            // - En modo claro: Íconos oscuros (SurfaceDark) sobre fondo claro
            navigationBarStyle = if (darkTheme) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            }
        )
        onDispose {}
    }

    // Inyectamos nuestro LocalSpacing en el árbol de Compose
    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
