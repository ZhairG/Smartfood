package com.smartfood.app.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.ui.theme.SmartfoodTheme
import com.smartfood.app.utils.SystemBarEffect
import kotlinx.coroutines.delay

/**
 * Nombre: SplashScreen.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pantalla de carga inicial (Splash Screen) de la aplicación Smartfood.
 * Muestra el logotipo centrado y conmuta a la siguiente pantalla tras un tiempo de espera.
 *
 * @param onNavigateNext Callback invocado tras finalizar la temporización inicial para navegar a la siguiente pantalla.
 * @param modifier Modificador opcional para la personalización del contenedor base.
 */
@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    SystemBarEffect(isLightBackground = true)
    LaunchedEffect(key1 = true) {
        delay(2000)
        onNavigateNext()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0FDF4)),
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

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun SplashScreenPreview() {
    SmartfoodTheme {
        SplashScreen(onNavigateNext = {})
    }
}
