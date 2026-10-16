package com.smartfood.app.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.core.designsystem.theme.GreyDark
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.util.figmaShadow

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
 * @param leadingIcon Ícono opcional a la izquierda del texto, como el "+" de "Añadir al Inventario" (Figma
 * `476:5726`, spec 006 T026). El color naranja habilitado y gris deshabilitado ya es el del Figma de inventario.
 * @param destructive Variante destructiva: fondo `error` y contenido `onError`, para el botón de confirmar de
 * `SmartFoodConfirmDialog` (Figma `476:3815`, CA-INV-04-01). Por defecto es el naranja del tema.
 */
@Composable
fun SmartFoodPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = text,
    leadingIcon: ImageVector? = null,
    destructive: Boolean = false
) {
    val buttonShape = RoundedCornerShape(10.dp)

    // Colores cerrados: nadie pasa colores sueltos al botón (plan §8, #80).
    val containerColor =
        if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    val contentColor =
        if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary
    
    // La animación de los puntos solo existe mientras carga: en reposo el botón no recompone cada frame.
    val displayText = if (isLoading) {
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
        "$loadingText${".".repeat(dotCount.toInt() % 4)}"
    } else {
        text
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LocalFormMetrics.current.buttonHeight)
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
            // Figma (CA-UI-07-07): deshabilitado en gris; mientras carga conserva su color y no es clicable.
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = if (isLoading) containerColor else GreyDark,
                // Mientras carga conserva su texto (`onError` o `onSecondary`); deshabilitado en reposo, el gris.
                disabledContentColor = if (isLoading) contentColor else MaterialTheme.colorScheme.onSecondary,
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (leadingIcon != null && !isLoading) {
                // Decorativo: el texto ya dice qué hace el botón.
                Icon(imageVector = leadingIcon, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
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

@Preview(showBackground = true)
@Composable
private fun SmartFoodPrimaryButtonDestructivePreview() {
    SmartfoodTheme {
        SmartFoodPrimaryButton(
            text = "Eliminar",
            onClick = {},
            destructive = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
