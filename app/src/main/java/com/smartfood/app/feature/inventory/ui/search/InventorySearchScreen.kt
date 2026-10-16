/**
 * Nombre: InventorySearchScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla de búsqueda del inventario (spec 006, HU-INV-06, CA-INV-06-01 a 06-08). Es stateless: recibe
 * el estado y avisa por eventos, y adapta sus medidas a los tokens del Design System (RNF-INV-06). La tarjeta
 * "Usa tu voz" se muestra pero todavía no escucha, porque la voz es del #43 (T035).
 */
package com.smartfood.app.feature.inventory.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
import com.smartfood.app.core.designsystem.theme.spacing

/**
 * Muestra la búsqueda del inventario.
 *
 * @param state Estado de la pantalla (texto, filtro, resultados y carga).
 * @param onEvent Se llama con cada acción del usuario.
 */
@Composable
fun InventorySearchScreen(
    state: InventorySearchUiState,
    onEvent: (InventorySearchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchHeader(
            query = state.query,
            onQueryChanged = { onEvent(InventorySearchEvent.QueryChanged(it)) },
            onClearQuery = { onEvent(InventorySearchEvent.ClearQueryClicked) },
            onBack = { onEvent(InventorySearchEvent.BackClicked) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MaterialTheme.spacing.medium)
        ) {
            VoiceSearchCard(onClick = { onEvent(InventorySearchEvent.VoiceClicked) })

            CategoryFilterChips(
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

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        items(items = state.results, key = { it.item.id }) { result ->
                            InventorySearchResultRow(
                                result = result,
                                onClick = { onEvent(InventorySearchEvent.ResultClicked(result.item.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Barra superior de la búsqueda: flecha atrás y barra de texto (CA-INV-06-01).
 *
 * @param query Texto actual de la barra.
 * @param onQueryChanged Se llama con cada carácter escrito.
 * @param onClearQuery Se llama al tocar la "x", que aparece solo con texto (CA-INV-06-07).
 * @param onBack Se llama al tocar la flecha atrás.
 */
@Composable
private fun SearchHeader(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBack: () -> Unit,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.cd_navigate_back)
            )
        }

        SmartFoodTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = stringResource(id = R.string.inventory_search_placeholder),
            leadingContent = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingContent = clearButton,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Tarjeta "Usa tu voz" de la búsqueda (CA-INV-06-01). Por ahora solo avisa que se tocó: la escucha llega con el
 * #43 (T035).
 *
 * @param onClick Se llama al tocar la tarjeta o su micrófono.
 */
@Composable
private fun VoiceSearchCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.small)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MaterialTheme.spacing.inputRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.search_voice_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(id = R.string.search_voice_card_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(id = R.string.cd_mic_button),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
