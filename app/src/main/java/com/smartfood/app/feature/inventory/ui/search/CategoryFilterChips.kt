/**
 * Nombre: CategoryFilterChips.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Filtros de categoría de la búsqueda (spec 006, CA-INV-06-01 y CA-INV-06-03). El chip activo se
 * anuncia como seleccionado para TalkBack, igual que en el resto de la app (RNF-INV-04).
 */
package com.smartfood.app.feature.inventory.ui.search

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.domain.model.FoodCategory

/**
 * Nombre visible de cada categoría del inventario.
 */
@get:StringRes
internal val FoodCategory.titleRes: Int
    get() = when (this) {
        FoodCategory.FRUITS -> R.string.category_fruits
        FoodCategory.DAIRY -> R.string.category_dairy
        FoodCategory.PROTEINS -> R.string.category_proteins
        FoodCategory.VEGETABLES -> R.string.category_vegetables
        FoodCategory.GRAINS -> R.string.category_grains
        FoodCategory.OTHER -> R.string.category_other
    }

/**
 * Muestra la fila de filtros por categoría de la búsqueda.
 *
 * @param selectedCategory Categoría elegida, o `null` si el filtro activo es "Todos".
 * @param onCategorySelected Se llama cuando el usuario toca un filtro; `null` para "Todos".
 */
@Composable
fun CategoryFilterChips(
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
