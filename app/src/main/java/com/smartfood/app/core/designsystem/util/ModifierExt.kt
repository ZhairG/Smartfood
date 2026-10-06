package com.smartfood.app.core.designsystem.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * <build>
 * <b>Nombre:</b> ModifierExt.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 24/09/2026<br>
 * <b>Descripción:</b> Funciones de extensión utilitarias para [Modifier] reutilizables en toda la capa de UI.<br>
 * </build>
 */

/**
 * Agrega un detector de gestos que remueve el foco de los campos de texto al tocar cualquier área libre fuera de ellos.
 *
 * @param focusManager Administrador de foco actual ([FocusManager]).
 * @param onTap Callback adicional opcional ejecutado al tocar la pantalla.
 * @return [Modifier] configurado para limpiar el foco.
 */
fun Modifier.clearFocusOnTap(
    focusManager: FocusManager,
    onTap: (() -> Unit)? = null
): Modifier = this.pointerInput(focusManager, onTap) {
    detectTapGestures(onTap = {
        focusManager.clearFocus()
        onTap?.invoke()
    })
}

/**
 * Aplica el estilo de fondo, borde y redondeado estándar para los contenedores de los campos de texto.
 *
 * @param shape Forma geométrica de las esquinas del campo.
 * @param borderWidth Grosor del borde en dp.
 * @param borderColor Color del borde.
 * @param backgroundColor Color de fondo de la caja.
 * @return [Modifier] con fondo y borde encadenados.
 */
fun Modifier.inputBorderAndBackground(
    shape: Shape = RoundedCornerShape(10.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color,
    backgroundColor: Color
): Modifier = this
    .background(backgroundColor, shape)
    .border(borderWidth, borderColor, shape)

/**
 * Aplica la curvatura superior y fondo del panel principal del formulario (45.dp por defecto).
 *
 * @param backgroundColor Color de superficie del panel.
 * @param topCornerRadius Radio de curvatura de las esquinas superiores.
 * @return [Modifier] configurado con la forma de panel.
 */
fun Modifier.panelTopCorners(
    backgroundColor: Color,
    topCornerRadius: Dp = 45.dp
): Modifier {
    val shape = RoundedCornerShape(topStart = topCornerRadius, topEnd = topCornerRadius)
    return this
        .background(color = backgroundColor, shape = shape)
        .clip(shape)
}
