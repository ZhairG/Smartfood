/**
 * Nombre: InventorySearchScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla de búsqueda del inventario (spec 006, HU-INV-06, CA-INV-06-01 a 06-08). Es stateless: recibe
 * el estado y avisa por eventos, y adapta sus medidas a los tokens del Design System (RNF-INV-06). Usa
 * `SmartFoodTopBar` (Figma `476:5960`) y reutiliza `InventoryItemRow` y `CategoryChips` de `ui/components/`. La
 * tarjeta "Usa tu voz" se muestra con el micrófono **desactivado**: la escucha llega con el #43 (T035).
 */
package com.smartfood.app.feature.inventory.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.ui.components.CategoryChips
import com.smartfood.app.feature.inventory.ui.components.InventoryItemRow

/**
 * Muestra la búsqueda del inventario.
 *
 * @param state Estado de la pantalla (texto, filtro, resultados y carga).
 * @param onEvent Se llama con cada acción del usuario.
 * @param modifier Modificador opcional.
 */
@Composable
fun InventorySearchScreen(
    state: InventorySearchUiState,
    onEvent: (InventorySearchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
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

            VoiceSearchCard(
                isVoiceAvailable = state.isVoiceAvailable,
                onClick = { onEvent(InventorySearchEvent.VoiceClicked) }
            )

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

/**
 * Tarjeta "Usa tu voz" de la búsqueda (CA-INV-06-01). El micrófono queda visible y **desactivado** hasta que el #43
 * (T035) enlace la escucha, para que TalkBack lo anuncie como desactivado.
 *
 * @param isVoiceAvailable Si la voz está disponible; en el #42 siempre es `false`.
 * @param onClick Se llama al tocar la tarjeta o su micrófono.
 * @param modifier Modificador opcional.
 */
@Composable
private fun VoiceSearchCard(
    isVoiceAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.small)
            .clickable(enabled = isVoiceAvailable, onClick = onClick),
        shape = RoundedCornerShape(MaterialTheme.spacing.inputRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(id = R.string.search_voice_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = stringResource(id = R.string.search_voice_card_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
            FilledIconButton(
                onClick = onClick,
                enabled = isVoiceAvailable,
                modifier = Modifier.size(MicButtonSize),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(id = R.string.cd_mic_button)
                )
            }
        }
    }
}

// Medidas del Figma (`476:3190`): el micrófono es un círculo de 60 px centrado en la tarjeta. No son medidas de
// formulario, así que no van en FormMetrics.
private val MicButtonSize = 60.dp
