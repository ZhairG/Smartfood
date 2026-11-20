package com.smartfood.app.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.smartfood.app.core.designsystem.theme.SmartFoodMotion

/**
 * Nombre: AnimatedMessage.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Mensaje en línea (error o aviso) que aparece y se va con expansión y disolvencia de 150 ms, sin mover de
 * golpe los campos de abajo (spec 007, CA-ANI-04-03). Mientras se va, sigue mostrando el último mensaje.
 *
 * @param T Tipo del mensaje (texto, error tipado…).
 * @param message Mensaje vigente; `null` lo oculta.
 * @param modifier Modificador del contenedor animado.
 * @param notice Si es un aviso de resultado (300 ms, CA-ANI-04-04) en lugar de un error en línea (150 ms).
 * @param content Dibuja el mensaje.
 */
@Composable
fun <T : Any> AnimatedMessage(
    message: T?,
    modifier: Modifier = Modifier,
    notice: Boolean = false,
    content: @Composable (T) -> Unit,
) {
    val last = remember { LastValue<T>() }
    if (message != null) last.value = message
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = if (notice) {
            fadeIn(SmartFoodMotion.standard()) + expandVertically(SmartFoodMotion.standard())
        } else {
            fadeIn(SmartFoodMotion.focus()) + expandVertically(SmartFoodMotion.focus())
        },
        exit = if (notice) {
            fadeOut(SmartFoodMotion.standard()) + shrinkVertically(SmartFoodMotion.standard())
        } else {
            fadeOut(SmartFoodMotion.focus()) + shrinkVertically(SmartFoodMotion.focus())
        }
    ) {
        last.value?.let { content(it) }
    }
}

/** Último mensaje no nulo; no es estado de Compose porque solo se lee durante la salida. */
private class LastValue<T> {
    var value: T? = null
}
