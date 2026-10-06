package com.smartfood.app.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.smartfood.app.core.designsystem.theme.GreenLight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.util.SystemBarEffect
import kotlinx.coroutines.delay

/**
 * Nombre: SplashScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla de carga inicial (Splash Screen) de la aplicación Smartfood.
 * Utiliza [Scaffold] para mantener el orden estructural del layout, muestra el logotipo centrado
 * y navega al [destination] resuelto por [SplashViewModel] cuando han pasado al menos
 * [MIN_SPLASH_DURATION_MS] (CA-03-04).
 *
 * @param destination Destino resuelto según la sesión, o `null` si aún se está consultando.
 * @param onNavigate Callback invocado una sola vez con el destino final.
 * @param modifier Modificador opcional para la personalización del contenedor base.
 */
@Composable
fun SplashScreen(
    destination: SplashDestination?,
    onNavigate: (SplashDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    SystemBarEffect(isLightBackground = true)

    var minimumTimeElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(MIN_SPLASH_DURATION_MS)
        minimumTimeElapsed = true
    }
    LaunchedEffect(minimumTimeElapsed, destination) {
        if (minimumTimeElapsed && destination != null) onNavigate(destination)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GreenLight,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(GreenLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logo_smartfood_circle),
                contentDescription = stringResource(id = R.string.cd_smartfood_logo),
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(width = 128.dp, height = 135.dp)
                    .testTag("splash_logo")
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun SplashScreenPreview() {
    SmartfoodTheme {
        SplashScreen(destination = null, onNavigate = {})
    }
}

/** Tiempo mínimo que el splash permanece visible, aunque la sesión se resuelva antes (CA-03-04). */
const val MIN_SPLASH_DURATION_MS = 2000L
