package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.annotations.concurrent.Background
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme

/**
 * Nombre: Brand.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componentes de identidad de marca (Wordmark y Encabezado de marca).
 */

/**
 * Componente atómico de isotipo de texto "SmartFood".
 * Renderiza "Smart" en el color indicado y "Food" en el color secundario (naranja).
 *
 * @param modifier Modificador opcional para personalizar el diseño.
 * @param smartColor Color para la palabra "Smart". Por defecto toma el valor de `onPrimaryContainer`.
 */
@Composable
fun SmartFoodWordmark(
    modifier: Modifier = Modifier,
    smartColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = smartColor)) { append("Smart") }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append("Food") }
        },
        style = MaterialTheme.typography.displayLarge,
        modifier = modifier,
    )
}

/**
 * Encabezado de marca de la aplicación.
 * Muestra el ícono del logo dentro de un contenedor circular blanco y el isotipo [SmartFoodWordmark] debajo.
 *
 * @param modifier Modificador opcional para ajustar el diseño.
 * @param smartColor Color para el segmento "Smart" del isotipo.
 */
@Composable
fun SmartFoodBrandHeader(
    modifier: Modifier = Modifier,
    smartColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(color = Color.White, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logo_smartfood_circle),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(96.dp),
            )
        }
        SmartFoodWordmark(smartColor = smartColor)
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartFoodBrandHeaderPreview() {
    SmartfoodTheme {
        SmartFoodBrandHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 77.dp, bottom = 53.dp),
            smartColor = MaterialTheme.colorScheme.onBackground
        )
    }
}
