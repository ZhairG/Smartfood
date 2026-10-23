package com.smartfood.app.feature.recipes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.core.designsystem.theme.GreyLight
import com.smartfood.app.core.designsystem.util.figmaShadow

/**
 * Nombre: OutlineActionButton.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Botón blanco con borde y texto verde de las pantallas de recetas, como en el Figma: "Anterior" de la
 * cocina guiada (`557:2797`), "Cancelar" de "Analizando opciones" (`666:3558`), "Ver recetas económicas" (`557:2532`) y
 * "Contactar soporte" (`557:2476`). Mismo alto, radio y sombra que [RecipeActionButton] (spec 009, T019 y T016).
 *
 * @param text Texto del botón.
 * @param onClick Acción al tocarlo.
 * @param modifier Modificador opcional; el ancho lo pone quien lo usa.
 * @param enabled `false` lo pone en gris y lo deshabilita.
 */
@Composable
fun OutlineActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(ButtonRadius)
    // Atenuado con colores, no con transparencia, para no cortar la sombra (como RecipeActionButton).
    val content = if (enabled) MaterialTheme.colorScheme.primary else GreyLight
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(ButtonHeight)
            .figmaShadow(borderRadius = ButtonRadius)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(ButtonBorder, content, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

// Medidas del Figma (557:2797). No son de formulario, así que no van en FormMetrics.
private val ButtonHeight = 48.dp
private val ButtonRadius = 10.dp
private val ButtonBorder = 1.dp
