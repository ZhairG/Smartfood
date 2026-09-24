package com.smartfood.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Componente atómico de Texto enlazable.
 *
 * Utiliza el color secundario de la aplicación para resaltar que es interactivo y aplica
 * el rol semántico [Role.Button] para mejorar la accesibilidad (TalkBack).
 *
 * @param text Texto subrayable o visible.
 * @param onClick Acción que se ejecuta al presionar.
 * @param modifier Modificador de tamaño y posición opcional.
 */
@Composable
fun SmartFoodTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge, // Mapeado a Link (15.sp, SemiBold)
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/**
 * Componente compuesto por una frase genérica de invitación (prompt) y un componente interactivo
 * [SmartFoodTextLink] (acción). Se renderizan en una sola fila.
 *
 * Ejemplo de uso: "¿No tienes cuenta? Regístrate"
 *
 * @param prompt Frase no clickeable (Ej. "¿No tienes cuenta?").
 * @param action Texto clickeable para el [SmartFoodTextLink] (Ej. "Regístrate").
 * @param onClick Acción desencadenada cuando se toca el parámetro [action].
 * @param modifier Modificador base para el contenedor (Row).
 */
@Composable
fun SmartFoodPromptLink(
    prompt: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp), // Medida de Figma
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prompt, 
            style = MaterialTheme.typography.labelLarge, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SmartFoodTextLink(text = action, onClick = onClick)
    }
}
