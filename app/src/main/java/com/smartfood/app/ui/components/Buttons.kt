package com.smartfood.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.ui.theme.SmartfoodTheme
import com.smartfood.app.utils.figmaShadow

/**
 * Nombre: Buttons.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componentes atómicos de botones de acción de Smartfood.
 */

/**
 * Componente atómico de Botón Primario interactivo.
 *
 * Muestra un botón con bordes redondeados y estilo visual según el Design System del mockup inicial,
 * utilizando la sombra reutilizable de [figmaShadow].
 *
 * @param text Texto principal a mostrar en el botón cuando no está en estado de carga.
 * @param onClick Callback que se ejecuta al presionar el botón.
 * @param modifier Modificador opcional para ajustar el diseño exterior.
 * @param enabled Estado de habilitación del botón.
 * @param isLoading Indica si el botón se encuentra en un estado de procesamiento asíncrono.
 * @param loadingText Texto base a mostrar durante la animación de carga.
 */
@Composable
fun SmartFoodPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = text
) {
    val buttonShape = RoundedCornerShape(10.dp)
    
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotCount by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots_count"
    )

    val displayText = if (isLoading) {
        val dots = ".".repeat(dotCount.toInt() % 4)
        "$loadingText$dots"
    } else {
        text
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(51.dp)
            .figmaShadow(
                color = Color(0x4D000000), // #000000 con 30% de opacidad (0x4D)
                borderRadius = 10.dp,
                blurRadius = 4.dp,
                offsetY = 4.dp
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary, 
                contentColor = MaterialTheme.colorScheme.onSecondary, 
                disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartFoodPrimaryButtonPreview() {
    SmartfoodTheme {
        SmartFoodPrimaryButton(
            text = "Iniciar Sesión",
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
