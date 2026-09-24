package com.smartfood.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Componente atómico de Botón Primario interactivo.
 *
 * Utiliza los colores de [MaterialTheme.colorScheme.secondary] para el fondo y la tipografía `titleLarge`.
 * Incluye una animación infinita de puntos suspensivos ("...", "..", ".") durante el estado de carga
 * que se activa automáticamente al pasar [isLoading] a `true`.
 *
 * @param text El texto principal que se mostrará en el botón cuando no esté cargando.
 * @param onClick Acción que se ejecutará al hacer clic en el botón. No se ejecutará si está cargando o deshabilitado.
 * @param modifier Modificador opcional para ajustar el diseño exterior.
 * @param enabled Estado de habilitación del botón. Por defecto es `true`.
 * @param isLoading Indica si el botón se encuentra procesando una acción asíncrona. Deshabilita los clics al ser `true`.
 * @param loadingText Texto que se mostrará base seguido de los puntos suspensivos cuando [isLoading] sea `true`. Por defecto, toma el valor de [text].
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
    val shape = RoundedCornerShape(10.dp)
    
    // Animación infinita para los puntos suspensivos
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotCount by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f, // Va de 0 a 3 (los enteros)
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

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary, 
            contentColor = MaterialTheme.colorScheme.onSecondary, 
            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(51.dp) 
            .shadow(
                elevation = 4.dp, 
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f),
            ),
    ) {
        Text(text = displayText, style = MaterialTheme.typography.titleLarge)
    }
}
