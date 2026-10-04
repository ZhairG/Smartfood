package com.smartfood.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.theme.OrangeMain
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme

/**
 * Nombre: StepIndicator.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componente atómico de indicador de pasos animado según diseño de Figma ("Indicador de pasos").
 * Transiciona de forma fluida mediante resortes (spring physics) entre puntos circulares e indicadores de píldora
 * cuando el usuario navega entre los pasos del flujo de recuperación de contraseña.
 *
 * @param currentStep Índice del paso activo actual (0-indexed).
 * @param totalSteps Cantidad total de pasos en la secuencia (por defecto 3).
 * @param modifier Modificador opcional.
 */
@Composable
fun SmartFoodStepIndicator(
    currentStep: Int,
    totalSteps: Int = 3,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { stepIndex ->
            val isActive = stepIndex == currentStep

            val animatedWidth by animateDpAsState(
                targetValue = if (isActive) 38.dp else 12.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "step_width_$stepIndex"
            )

            val animatedColor by animateColorAsState(
                targetValue = if (isActive) OrangeMain else OrangeMain.copy(alpha = 0.65f),
                animationSpec = tween(durationMillis = 300),
                label = "step_color_$stepIndex"
            )

            Box(
                modifier = Modifier
                    .height(12.dp)
                    .width(animatedWidth)
                    .background(
                        color = animatedColor,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14532D, name = "Paso 1 Activo")
@Composable
private fun StepIndicatorStep1Preview() {
    SmartfoodTheme {
        SmartFoodStepIndicator(currentStep = 0)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14532D, name = "Paso 2 Activo")
@Composable
private fun StepIndicatorStep2Preview() {
    SmartfoodTheme {
        SmartFoodStepIndicator(currentStep = 1)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14532D, name = "Paso 3 Activo")
@Composable
private fun StepIndicatorStep3Preview() {
    SmartfoodTheme {
        SmartFoodStepIndicator(currentStep = 2)
    }
}
