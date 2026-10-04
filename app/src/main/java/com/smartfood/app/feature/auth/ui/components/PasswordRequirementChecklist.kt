package com.smartfood.app.feature.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.GreyNeutral35
import com.smartfood.app.core.designsystem.theme.OrangeMain
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.White
import com.smartfood.app.core.designsystem.util.figmaShadow

/**
 * Nombre: PasswordRequirementChecklist.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Tarjeta con los criterios de seguridad de la contraseña, con el estilo del componente
 * "Checklist Crear contraseña" de Figma (nodo 572:1299, spec 002 CA-UI-07-04): tarjeta blanca con borde y sombra,
 * ítems de 15 sp en negro, círculo con check verde si se cumple y X naranja si no. Se muestra como popup
 * encima del campo de contraseña enfocado (HU-UI-03).
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .figmaShadow(
                color = Color(0x80000000), // Figma: 0 4 4, negro al 50 %
                borderRadius = 20.dp,
                blurRadius = 4.dp,
                offsetY = 4.dp
            )
            .background(color = White, shape = cardShape)
            .border(width = 1.dp, color = GreyNeutral35, shape = cardShape)
            .padding(start = 10.dp, end = 10.dp, top = 22.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (isSatisfied) R.drawable.ic_requirement_met else R.drawable.ic_requirement_unmet
            ),
            contentDescription = null,
            tint = if (isSatisfied) GreenCheck else OrangeMain,
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
