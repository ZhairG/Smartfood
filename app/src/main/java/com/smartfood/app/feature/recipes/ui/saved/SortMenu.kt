package com.smartfood.app.feature.recipes.ui.saved

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.smartfood.app.R
import com.smartfood.app.feature.inventory.ui.components.OptionSheet
import com.smartfood.app.feature.recipes.domain.model.SavedSort

/**
 * Nombre: SortMenu.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Menú "Ordenar" de Guardadas (spec 010, CA-SAV-02-05, T007). El Figma no lo define (desviación §9), así
 * que usa la hoja de selección del inventario.
 *
 * @param selected Orden elegido.
 * @param onConfirm Se llama con el orden confirmado.
 * @param onDismiss Se llama al cerrar el menú sin confirmar.
 */
@Composable
fun SortMenu(
    selected: SavedSort,
    onConfirm: (SavedSort) -> Unit,
    onDismiss: () -> Unit,
) {
    OptionSheet(
        title = stringResource(id = R.string.saved_sort),
        options = SavedSort.entries,
        selected = selected,
        label = { sort ->
            stringResource(
                id = when (sort) {
                    SavedSort.RECENT -> R.string.saved_sort_recent
                    SavedSort.NAME -> R.string.saved_sort_name
                }
            )
        },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
