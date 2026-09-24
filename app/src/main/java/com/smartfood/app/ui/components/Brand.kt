package com.smartfood.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.unit.dp
import com.smartfood.app.R

/** Logotipo en texto: "Smart" + "Food" (en color secundario/naranja). */
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
        style = MaterialTheme.typography.displayLarge, // Mapeado a Wordmark (40.sp, Bold)
        modifier = modifier,
    )
}

/** Encabezado de marca: círculo con el logo (100 dp) y logotipo debajo. */
@Composable
fun SmartFoodBrandHeader(
    modifier: Modifier = Modifier,
    smartColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp), // Medida exacta de Figma
    ) {
        Box(
            modifier = Modifier
                .size(100.dp) // Medida exacta de Figma para el círculo contenedor
                .background(color = Color.White, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logo_smartfood_circle),
                contentDescription = null,
                tint = Color.Unspecified, // Respeta los colores del SVG
                modifier = Modifier.size(64.dp), // Ajusta este tamaño para que el icono encaje bien dentro del círculo
            )
        }
        SmartFoodWordmark(smartColor = smartColor)
    }
}
