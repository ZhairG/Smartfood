package com.smartfood.app.utils

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nombre: ShadowExt.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Función de extensión de [Modifier] para aplicar sombras difuminadas personalizadas estilo Figma.
 */

/**
 * Aplica una sombra difuminada tipo Figma con radio de esquina, desenfoque (blur) y desplazamientos (X/Y).
 *
 * @param color Color de la sombra incluyendo su nivel de transparencia/opacidad.
 * @param borderRadius Radio de las esquinas del rectángulo redondeado de la sombra.
 * @param blurRadius Radio del filtro de desenfoque gaussiano ([BlurMaskFilter]).
 * @param offsetY Desplazamiento vertical de la sombra.
 * @param offsetX Desplazamiento horizontal de la sombra.
 * @return [Modifier] con la sombra dibujada en la capa de fondo.
 */
fun Modifier.figmaShadow(
    color: Color = Color(0x4D000000),
    borderRadius: Dp = 10.dp,
    blurRadius: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()

        frameworkPaint.color = color.toArgb()

        if (blurRadius.toPx() > 0f) {
            frameworkPaint.maskFilter = BlurMaskFilter(
                blurRadius.toPx(),
                BlurMaskFilter.Blur.NORMAL
            )
        }

        val left = offsetX.toPx()
        val top = offsetY.toPx()
        val right = size.width + offsetX.toPx()
        val bottom = size.height + offsetY.toPx()
        val radius = borderRadius.toPx()

        canvas.nativeCanvas.drawRoundRect(
            left, top, right, bottom,
            radius, radius,
            frameworkPaint
        )
    }
}
