package com.smartfood.app.feature.recipes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.core.designsystem.theme.GreyDivider

/**
 * Nombre: RecipeFilterPill.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Píldora de filtro de Recetas (Figma `557:3044`) y de Guardadas (`633:2723`): verde oscuro la elegida y
 * clara con borde gris las demás (spec 010, CA-SAV-02-02).
 *
 * @param text Texto del filtro.
 * @param selected `true` si es el filtro elegido.
 * @param onClick Se llama al tocarla.
 */
@Composable
fun RecipeFilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else GreyDivider,
                shape = CircleShape
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = FilterSidePadding, vertical = FilterVerticalPadding)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

// Medidas del Figma 557:3044. No son de formulario, así que no van en FormMetrics.
private val FilterSidePadding = 12.dp
private val FilterVerticalPadding = 7.dp
