package com.smartfood.app.core.designsystem.util

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Nombre: KeyboardExt.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Efectos de teclado compartidos por los formularios (spec 002, HU-UI-02).
 */

/**
 * Quita el foco del campo activo cuando el usuario cierra el teclado (botón atrás o gesto), sin desplazar
 * la pantalla (CA-UI-02-06). Solo reacciona a la transición de visible a oculto, así que no interfiere al
 * pasar de un campo a otro.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClearFocusOnKeyboardDismiss() {
    val isImeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    val wasVisible = remember { mutableStateOf(isImeVisible) }

    LaunchedEffect(isImeVisible) {
        if (wasVisible.value && !isImeVisible) focusManager.clearFocus()
        wasVisible.value = isImeVisible
    }
}
