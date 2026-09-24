package com.smartfood.app.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartfood.app.ui.theme.SmartfoodTheme
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import com.smartfood.app.R

/**
 * Figma: "Pantalla de carga" (nodo 99:2647), 412x917.
 * El logo (128x135) queda exactamente centrado en el frame.
 */
@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Retraso de 2000ms especificado en Figma (Después de la demora: 2000ms -> Navegar)
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
            contentDescription = "Logo de SmartFood",
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
