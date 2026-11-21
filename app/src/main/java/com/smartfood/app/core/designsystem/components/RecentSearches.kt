package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme

/**
 * Nombre: RecentSearches.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: «Recientes» bajo una barra de búsqueda (spec 014, HU-LOC-02): el título, «Borrar» y hasta 5 chips de
 * texto con la forma y la letra de los filtros del Figma (`476:3190`). Es una desviación aprobada (spec 014 §9): el
 * Figma no tiene búsquedas recientes ni un ícono de reloj, así que los chips no llevan ícono. Usa roles de color, así
 * que se adapta al modo oscuro. Si [items] está vacío, no dibuja nada (CA-LOC-02-01).
 *
 * @param items Búsquedas recientes, la última primero.
 * @param onItemClick Se llama con el texto del chip tocado (CA-LOC-02-05).
 * @param onClear Se llama al tocar «Borrar» (CA-LOC-02-06).
 * @param modifier Modificador opcional.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartFoodRecentSearches(
    items: List<String>,
    onItemClick: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.search_recent_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() }
            )
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(id = R.string.search_recent_clear),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
            items.forEach { text ->
                AssistChip(
                    onClick = { onItemClick(text) },
                    label = { Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    shape = CircleShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(ChipBorder, MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

// Separación y borde de los filtros del Figma (476:3190).
private val ChipGap = 8.dp
private val ChipBorder = 1.dp

@Preview(showBackground = true)
@Composable
private fun SmartFoodRecentSearchesPreview() {
    SmartfoodTheme {
        SmartFoodRecentSearches(items = listOf("pollo", "arroz con leche", "lentejas"), onItemClick = {}, onClear = {})
    }
}
