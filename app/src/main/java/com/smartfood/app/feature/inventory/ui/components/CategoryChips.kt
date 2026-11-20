/**
 * Nombre: CategoryChips.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Filtros por categoría del inventario (spec 006, CA-INV-06-01 y CA-INV-06-03). El chip activo se
 * anuncia como seleccionado para TalkBack, igual que en el resto de la app (RNF-INV-04). Vive en `ui/components/`
 * porque lo comparten las pantallas del inventario (la búsqueda, #42).
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.animation.animateColorAsState
import com.smartfood.app.core.designsystem.theme.SmartFoodMotion
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.smartFoodColors
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
    val container by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.smartFoodColors.softFill, SmartFoodMotion.standard(), label = "container")
    val label by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, SmartFoodMotion.standard(), label = "label")
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.smartFoodColors.divider, SmartFoodMotion.standard(), label = "border")
    FilterChip(
        selected = selected,
        onClick = onClick,
        // Figma 476:3213: 13 px, SemiBold en la elegida y Medium en las demás.
        label = {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        },
        shape = CircleShape,
        // Los colores cambian en 300 ms (spec 007, CA-ANI-05-03): el mismo valor animado sirve para los dos estados.
        colors = FilterChipDefaults.filterChipColors(
            containerColor = container,
            labelColor = label,
            selectedContainerColor = container,
            selectedLabelColor = label
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = border,
            selectedBorderColor = border
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun CategoryChipsPreview() {
    SmartfoodTheme { CategoryChips(selectedCategory = FoodCategory.DAIRY, onCategorySelected = {}) }
}
