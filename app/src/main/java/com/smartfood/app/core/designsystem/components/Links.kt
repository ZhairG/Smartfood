package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme

/**
 * Nombre: Links.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componentes atómicos para enlaces interactivos y llamadas a la acción en texto.
 */

/**
 * Componente atómico de Texto enlazable con el estilo del mockup inicial.
 * Aplica color secundario, peso [FontWeight.SemiBold] (Figma) y rol semántico [Role.Button].
 *
 * @param text Texto visible del enlace.
 * @param onClick Callback ejecutado al presionar sobre el texto.
 * @param modifier Modificador opcional.
 * @param textAlign Alineación del texto.
 * @param fontSize Tamaño del texto; 15 sp por defecto.
 * @param color Color del enlace; el secundario (naranja) por defecto.
 */
@Composable
fun SmartFoodTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    fontSize: TextUnit = 15.sp,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    Text(
        text = text,
        textAlign = textAlign,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/**
 * Componente que combina un texto informativo previo (prompt) con un enlace interactivo [SmartFoodTextLink].
 *
 * @param prompt Texto informativo (ej. "¿No tienes cuenta?").
 * @param action Texto clickeable para la acción (ej. "Regístrate").
 * @param onClick Callback ejecutado al presionar sobre el enlace de la acción.
 * @param modifier Modificador base para la fila.
 * @param fontSize Tamaño de ambos textos; Figma usa 15, 16 o 18 sp según la pantalla.
 */
@Composable
fun SmartFoodPromptLink(
    prompt: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prompt,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SmartFoodTextLink(text = action, onClick = onClick, fontSize = fontSize)
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartFoodLinksPreview() {
    SmartfoodTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SmartFoodTextLink(
                text = "¿Olvidaste tu contraseña?",
                onClick = {}
            )
            SmartFoodPromptLink(
                prompt = "¿No tienes cuenta?",
                action = "Regístrate",
                onClick = {}
            )
        }
    }
}
