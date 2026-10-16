/**
 * Nombre: CategoryChips.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Filtros por categoría del inventario (spec 006, CA-INV-06-01 y CA-INV-06-03). El chip activo se
 * anuncia como seleccionado para TalkBack, igual que en el resto de la app (RNF-INV-04). Vive en `ui/components/`
 * porque lo comparten las pantallas del inventario (la búsqueda, #42).
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.domain.model.FoodCategory

/**
 * Muestra la fila de filtros por categoría con "Todos" al inicio.
 *
 * @param selectedCategory Categoría elegida, o `null` si el filtro activo es "Todos".
 * @param onCategorySelected Se llama cuando el usuario toca un filtro; `null` para "Todos".
 * @param modifier Modificador opcional.
 */
@Composable
fun CategoryChips(
    selectedCategory: FoodCategory?,
    onCategorySelected: (FoodCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        item(key = "category_all") {
            CategoryChip(
                text = stringResource(id = R.string.category_all),
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }

        items(items = FoodCategory.entries, key = { it.name }) { category ->
            CategoryChip(
                text = stringResource(id = category.titleRes),
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

/**
 * @param text Nombre visible del filtro.
 * @param selected `true` si es el filtro activo (CA-INV-06-03).
 * @param onClick Se llama al tocar el filtro.
 */
@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Figma 476:3190: píldoras redondas; la elegida en verde oscuro y las demás claras con borde gris.
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(text = text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}
