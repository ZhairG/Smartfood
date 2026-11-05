package com.smartfood.app.feature.recipes.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenSoft
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.ui.components.RecipePhoto
import com.smartfood.app.feature.recipes.ui.components.SavedRecipeCard

/**
 * Pantalla Guardadas (spec 010, T007). Muestra la lista de recetas favoritas y propias con
 * chips de filtro, búsqueda y orden.
 *
 * @param state Estado calculado por el ViewModel.
 * @param onEvent Acciones de la persona.
 * @param onSortMenuOpen Abre el menú de orden.
 * @param onSortMenuClose Cierra el menú de orden.
 */
@Composable
fun SavedRecipesScreen(
    state: SavedRecipesUiState,
    onEvent: (SavedRecipesEvent) -> Unit,
    onSortMenuOpen: () -> Unit,
    onSortMenuClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Encabezado Guardadas con ordenar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.saved_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                )
                IconButton(
                    onClick = onSortMenuOpen,
                ) {
                    Text(
                        text = "⋮",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                    )
                }
            }
        }

        // Buscador
        androidx.compose.material3.TextField(
            value = state.query,
            onValueChange = { onEvent(SavedRecipesEvent.QueryChanged(it)) },
            label = { Text(text = stringResource(id = R.string.saved_search_placeholder)) },

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SavedFilterChip(
                text = stringResource(id = R.string.saved_chip_favorites),
                selected = state.filter == SavedFilter.FAVORITES,
                onClick = { onEvent(SavedRecipesEvent.FilterChanged(SavedFilter.FAVORITES)) },
            )
            SavedFilterChip(
                text = stringResource(id = R.string.saved_chip_own),
                selected = state.filter == SavedFilter.OWN,
                onClick = { onEvent(SavedRecipesEvent.FilterChanged(SavedFilter.OWN)) },
            )
            SavedFilterChip(
                text = stringResource(id = R.string.saved_chip_recent),
                selected = state.filter == SavedFilter.RECENT,
                onClick = { onEvent(SavedRecipesEvent.FilterChanged(SavedFilter.RECENT)) },
            )
        }

        // Contenido
        if (state.isEmptyOverall) {
            EmptySavedState()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = state.cards,
                    key = { it.id },
                ) { card ->
                    SavedRecipeCard(
                        card = card,
                        onClick = { onEvent(SavedRecipesEvent.CardClicked(card.id)) },
                        onFavoriteToggled = { onEvent(SavedRecipesEvent.FavoriteToggled(card.id)) },
                    )
                }
            }
        }

        // Aviso "Sin conexión"
        if (state.hasPendingSync) {
            OfflineBanner()
        }
    }
}

/** Tarjeta de filtro (chip). */
@Composable
private fun SavedFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

/** Estado vacío de Guardadas. */
@Composable
private fun EmptySavedState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.saved_empty_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.saved_empty_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        androidx.compose.material3.Button(
            onClick = { },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(text = stringResource(id = R.string.saved_empty_button))
        }
    }
}

/** Banner "Sin conexión". */
@Composable
private fun OfflineBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = GreenSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.saved_offline_title),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = stringResource(id = R.string.saved_offline_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
