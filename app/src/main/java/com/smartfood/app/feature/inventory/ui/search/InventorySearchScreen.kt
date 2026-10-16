/**
 * Nombre: InventorySearchScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla de búsqueda del inventario (spec 006, HU-INV-06 y HU-INV-07, CA-INV-06-01 a 06-08 y
 * CA-INV-07-01 a 07-09). Es stateless: recibe el estado y avisa por eventos, y adapta sus medidas a los tokens del
 * Design System (RNF-INV-06). Usa `SmartFoodTopBar` (Figma `476:5960`) y reutiliza `InventoryItemRow` y
 * `CategoryChips` de `ui/components/`. La tarjeta "Usa tu voz" solo se muestra si hay servicio (CA-INV-07-08) y el
 * aviso de voz (`VoiceSearchOverlay`) se dibuja encima cuando escucha, procesa o falla.
 */
package com.smartfood.app.feature.inventory.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.ui.components.CategoryChips
import com.smartfood.app.feature.inventory.ui.components.InventoryItemRow
import com.smartfood.app.feature.inventory.ui.components.PermissionRationale
import com.smartfood.app.feature.inventory.ui.components.VoiceSearchCard
import com.smartfood.app.feature.inventory.ui.components.VoiceSearchOverlay

/**
 * Muestra la búsqueda del inventario.
 *
 * @param state Estado de la pantalla (texto, filtro, resultados, voz y carga).
 * @param onEvent Se llama con cada acción del usuario.
 * @param modifier Modificador opcional.
 * @param showPermissionRationale `true` para mostrar el aviso de permiso de micrófono denegado (CA-INV-07-06).
 * @param onOpenSettings Se llama al tocar "Abrir ajustes" en el aviso de permiso.
 * @param onDismissPermissionRationale Se llama al cerrar el aviso de permiso.
 */
@Composable
fun InventorySearchScreen(
    state: InventorySearchUiState,
    onEvent: (InventorySearchEvent) -> Unit,
    modifier: Modifier = Modifier,
    showPermissionRationale: Boolean = false,
    onOpenSettings: () -> Unit = {},
    onDismissPermissionRationale: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SmartFoodTopBar(
                title = stringResource(id = R.string.inventory_title),
                onBack = { onEvent(InventorySearchEvent.BackClicked) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.spacing.medium)
            ) {
                SearchField(
                    query = state.query,
                    onQueryChanged = { onEvent(InventorySearchEvent.QueryChanged(it)) },
                    onClearQuery = { onEvent(InventorySearchEvent.ClearQuery) }
                )

                if (state.isVoiceAvailable) {
                    VoiceSearchCard(onClick = { onEvent(InventorySearchEvent.VoiceClicked) })
                }

                CategoryChips(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { onEvent(InventorySearchEvent.CategorySelected(it)) }
                )

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                        state.isEmptyResult -> Text(
                            text = stringResource(id = R.string.search_no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = MaterialTheme.spacing.medium)
                        )

                        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = state.results, key = { it.item.id }) { result ->
                                InventoryItemRow(
                                    item = result.item,
                                    status = result.status,
                                    onEditClicked = { id -> onEvent(InventorySearchEvent.ResultClicked(id)) },
                                    showExpiryLabel = true
                                )
                            }
                        }
                    }
                }
            }
        }

        VoiceSearchOverlay(
            state = state.voice,
            onRetry = { onEvent(InventorySearchEvent.VoiceRetry) },
            onDismiss = { onEvent(InventorySearchEvent.VoiceDismissed) }
        )

        if (showPermissionRationale) {
            PermissionRationale(
                icon = Icons.Outlined.Mic,
                title = stringResource(id = R.string.search_voice_permission_title),
                message = stringResource(id = R.string.search_voice_permission_body),
                onOpenSettings = onOpenSettings,
                onDismiss = onDismissPermissionRationale
            )
        }
    }
}

/**
 * Barra de texto de la búsqueda (CA-INV-06-01, CA-INV-06-07).
 *
 * @param query Texto actual de la barra.
 * @param onQueryChanged Se llama con cada carácter escrito.
 * @param onClearQuery Se llama al tocar la "x", que aparece solo con texto (CA-INV-06-07).
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
    val clearButton: (@Composable () -> Unit)? = if (query.isEmpty()) {
        null
    } else {
        {
            IconButton(onClick = onClearQuery) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(id = R.string.cd_clear_search)
                )
            }
        }
    }

    SmartFoodTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = stringResource(id = R.string.inventory_search_placeholder),
        leadingContent = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingContent = clearButton,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.small)
    )
}
