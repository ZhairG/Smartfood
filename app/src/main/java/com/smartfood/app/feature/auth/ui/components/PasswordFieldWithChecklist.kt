package com.smartfood.app.feature.auth.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.components.AnchoredPopup
import com.smartfood.app.core.designsystem.components.SmartFoodPasswordField
import com.smartfood.app.feature.auth.domain.usecase.PasswordValidationResult

/**
 * Nombre: PasswordFieldWithChecklist.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Campo de contraseña que, mientras está enfocado, muestra los requisitos en un popup encima del
 * campo (spec 002, HU-UI-03). El popup no ocupa espacio en el formulario ni roba el foco.
 *
 * @param label Etiqueta del campo.
 * @param value Contraseña escrita.
 * @param onValueChange Callback al cambiar el texto.
 * @param validation Cumplimiento de cada regla de la política de contraseña.
 * @param modifier Modificador opcional.
 * @param passwordsMatch Si no es `null`, agrega la regla "¿Coinciden?" (paso 3 de la recuperación).
 * @param enabled Habilita o deshabilita el campo.
 * @param isError Resalta el campo con borde de error.
 * @param imeAction Acción de la tecla del teclado.
 * @param keyboardActions Acciones del teclado.
 */
@Composable
fun PasswordFieldWithChecklist(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    validation: PasswordValidationResult,
    modifier: Modifier = Modifier,
    passwordsMatch: Boolean? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }

    AnchoredPopup(
        visible = isFocused,
        modifier = modifier,
        maxPopupWidth = CHECKLIST_WIDTH,
        popupContent = {
            PasswordRequirementChecklist(
                hasMinimumLength = validation.hasMinimumLength,
                hasUppercase = validation.hasUppercase,
                hasSpecialCharacter = validation.hasSpecialCharacter,
                passwordsMatch = passwordsMatch,
                // Margen para que la sombra de la tarjeta no quede recortada por la ventana del popup.
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    ) {
        SmartFoodPasswordField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            isError = isError,
            imeAction = imeAction,
            keyboardActions = keyboardActions,
            onFocusChanged = { isFocused = it }
        )
    }
}

/** Ancho del checklist en Figma (componente "Checklist Crear contraseña", nodo 572:1299). */
private val CHECKLIST_WIDTH = 283.dp
