package com.smartfood.app.feature.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenMain
import com.smartfood.app.core.designsystem.theme.OrangeMain
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.util.figmaShadow

/**
 * Nombre: PasswordRequirementChecklist.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componente atómico de tarjeta que muestra los criterios de seguridad de contraseña
 * con indicadores visuales dinámicos basados en la especificación de Figma (nodo 572:1299).
 *
 * @param hasMinimumLength Indica si la contraseña tiene al menos 8 caracteres.
 * @param hasUppercase Indica si la contraseña contiene al menos una letra mayúscula.
 * @param hasSpecialCharacter Indica si la contraseña contiene un número o carácter especial.
 * @param modifier Modificador opcional para la personalización del contenedor.
 * @param passwordsMatch Indica opcionalmente si las contraseñas coinciden. Si es nulo, no se muestra la regla.
 */
@Composable
fun PasswordRequirementChecklist(
    hasMinimumLength: Boolean,
    hasUppercase: Boolean,
    hasSpecialCharacter: Boolean,
    modifier: Modifier = Modifier,
    passwordsMatch: Boolean? = null
) {
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .figmaShadow(
                color = Color(0x33000000),
                borderRadius = 20.dp,
                blurRadius = 4.dp,
                offsetY = 4.dp
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = cardShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = cardShape
            )
            .padding(vertical = 18.dp, horizontal = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RequirementItem(
                text = stringResource(id = R.string.password_req_min_length),
                isSatisfied = hasMinimumLength
            )
            RequirementItem(
                text = stringResource(id = R.string.password_req_uppercase),
                isSatisfied = hasUppercase
            )
            RequirementItem(
                text = stringResource(id = R.string.password_req_special_char),
                isSatisfied = hasSpecialCharacter
            )
            if (passwordsMatch != null) {
                RequirementItem(
                    text = stringResource(id = R.string.password_req_match),
                    isSatisfied = passwordsMatch
                )
            }
        }
    }
}

/**
 * Ítem individual de regla de contraseña.
 *
 * @param text Descripción del requisito.
 * @param isSatisfied Estado de cumplimiento de la regla.
 * @param modifier Modificador opcional.
 */
@Composable
private fun RequirementItem(
    text: String,
    isSatisfied: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isSatisfied) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
            contentDescription = null,
            tint = if (isSatisfied) GreenMain else OrangeMain,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordRequirementChecklistUnfulfilledPreview() {
    SmartfoodTheme {
        PasswordRequirementChecklist(
            hasMinimumLength = false,
            hasUppercase = false,
            hasSpecialCharacter = false,
            passwordsMatch = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordRequirementChecklistSatisfiedPreview() {
    SmartfoodTheme {
        PasswordRequirementChecklist(
            hasMinimumLength = true,
            hasUppercase = true,
            hasSpecialCharacter = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
