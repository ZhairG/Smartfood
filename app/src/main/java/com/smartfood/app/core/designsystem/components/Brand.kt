package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme

/**
 * <build>
 * <b>Nombre:</b> Brand.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 23/09/2026<br>
 * <b>Descripción:</b> Componentes de identidad de marca (Wordmark y Encabezado de marca).<br>
 * </build>
 */

/**
 * Componente atómico de isotipo de texto "SmartFood".
 * Renderiza "Smart" en el color indicado y "Food" en el color secundario (naranja).
 *
 * @param modifier Modificador opcional para personalizar el diseño.
 * @param smartColor Color para la palabra "Smart". Por defecto toma el valor de `onPrimaryContainer`.
 * @param fontSize Tamaño del texto; por defecto el de `displayLarge` (Figma).
 * @param lineHeight Alto de línea; por defecto el de `displayLarge` (Figma).
 */
@Composable
fun SmartFoodWordmark(
    modifier: Modifier = Modifier,
    smartColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    fontSize: TextUnit = MaterialTheme.typography.displayLarge.fontSize,
    lineHeight: TextUnit = MaterialTheme.typography.displayLarge.lineHeight,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = smartColor)) { append("Smart") }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append("Food") }
        },
        style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize, lineHeight = lineHeight),
        modifier = modifier,
    )
}

/**
 * Encabezado de marca de la aplicación.
 * Muestra el ícono del logo dentro de un contenedor circular blanco y el isotipo [SmartFoodWordmark].
 * Su tamaño y disposición (columna o fila) salen de [LocalFormMetrics].
 *
 * @param modifier Modificador opcional para ajustar el diseño.
 * @param smartColor Color para el segmento "Smart" del isotipo.
 */
@Composable
fun SmartFoodBrandHeader(
    modifier: Modifier = Modifier,
    smartColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val metrics = LocalFormMetrics.current
    val logo: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(metrics.logoSize)
                .background(color = Color.White, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logo_smartfood_circle),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(metrics.logoSize * LOGO_ICON_RATIO),
            )
        }
    }
    val wordmark: @Composable () -> Unit = {
        SmartFoodWordmark(
            smartColor = smartColor,
            fontSize = metrics.wordmarkFontSize,
            lineHeight = metrics.wordmarkLineHeight
        )
    }

    // En pantallas bajas el logo y el nombre van en una fila para ahorrar alto (CA-UI-01-05).
    if (metrics.inlineBrand) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            logo()
            wordmark()
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            logo()
            wordmark()
        }
    }
}

/** Proporción del ícono respecto al círculo blanco que lo contiene (96/100 en Figma). */
private const val LOGO_ICON_RATIO = 0.96f

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
