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

/** Enlace de texto usando el color secundario y la tipografía de labelLarge. */
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

/** Frase + enlace en una línea, p. ej. "¿No tienes cuenta? Regístrate". */
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
