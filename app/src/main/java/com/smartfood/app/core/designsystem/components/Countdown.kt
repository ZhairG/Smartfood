package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.ThemeMode
import java.util.Locale

/**
 * Nombre: Countdown.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Componentes de cuenta regresiva (spec 003, HU-REC-04). Una píldora con ícono de reloj y dígitos
 * tabulares en los colores de acento (`secondaryContainer`/`onSecondaryContainer`: naranja claro en modo claro y
 * naranja oscuro en modo oscuro). Ocupa el alto de una línea de texto, así que no cambia el alto del formulario
 * (CA-REC-04-04).
 */

/** Formatea [seconds] como `m:ss` (por ejemplo, `0:42` o `37:12`). Los valores negativos se muestran como `0:00`. */
fun formatCountdown(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%d:%02d", safeSeconds / 60, safeSeconds % 60)
}

/**
 * Píldora de cuenta regresiva con ícono de reloj.
 *
 * @param text Texto visible, por ejemplo "Reenviar en 0:42".
 * @param modifier Modificador opcional.
 * @param fontSize Tamaño del texto; el ícono usa el mismo tamaño.
 * @param contentDescription Descripción para lectores de pantalla; por defecto se lee [text].
 */
@Composable
fun SmartFoodCountdownChip(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    contentDescription: String? = null,
) {
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp)
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Timer,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(with(LocalDensity.current) { fontSize.toDp() })
        )
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            // Dígitos de ancho fijo: la píldora no cambia de ancho mientras corre la cuenta.
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        )
    }
}

/**
 * Variante de [SmartFoodPromptLink] mientras la acción no está disponible: el enlace se reemplaza por una
 * [SmartFoodCountdownChip] con el mismo tamaño de texto.
 *
 * @param prompt Texto informativo (ej. "¿No recibiste el código?").
 * @param countdown Texto de la píldora (ej. "Reenviar en 0:42").
 * @param modifier Modificador base para la fila.
 * @param fontSize Tamaño de ambos textos.
 * @param contentDescription Descripción de la píldora para lectores de pantalla.
 */
@Composable
fun SmartFoodPromptCountdown(
    prompt: String,
    countdown: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    contentDescription: String? = null,
) {
    SmartFoodPromptRow(prompt = prompt, modifier = modifier, fontSize = fontSize) {
        SmartFoodCountdownChip(text = countdown, fontSize = fontSize, contentDescription = contentDescription)
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartFoodCountdownPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
            SmartfoodTheme(themeMode = mode) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    SmartFoodPromptCountdown(
                        prompt = "¿No recibiste el código?",
                        countdown = "Reenviar en ${formatCountdown(42)}",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
