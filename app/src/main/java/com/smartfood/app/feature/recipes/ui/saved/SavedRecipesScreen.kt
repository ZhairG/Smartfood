package com.smartfood.app.feature.recipes.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.GreenSoft
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedRecipeCard as SavedRecipeCardData
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipeFilterPill
import com.smartfood.app.feature.recipes.ui.components.RecipeSearchField
import com.smartfood.app.feature.recipes.ui.components.RecipesHeader
import com.smartfood.app.feature.recipes.ui.components.SavedRecipeCard
import com.smartfood.app.feature.recipes.ui.components.recipeCardShadow

/**
 * Nombre: SavedRecipesScreen.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pantalla Guardadas (spec 010, HU-SAV-02, T007), fiel al Figma `633:2723`: encabezado verde con "Ordenar",
 * búsqueda, chips, tarjetas en dos columnas, aviso "Sin conexión" y botón "+". Sin recetas guardadas muestra el vacío
 * del Figma `557:2585`, que no tiene buscador, chips, "Ordenar" ni "+". Es stateless: recibe el estado y avisa los
 * eventos.
 *
 * @param state Estado calculado por el ViewModel.
 * @param onEvent Acciones de la persona.
 * @param modifier Modificador opcional.
 */
@Composable
fun SavedRecipesScreen(
    state: SavedRecipesUiState,
    onEvent: (SavedRecipesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showsList = !state.isLoading && !state.hasError && !state.isEmptyOverall
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        RecipesHeader(
            title = stringResource(id = R.string.saved_title),
            action = if (showsList) {
                { SortAction(onClick = { onEvent(SavedRecipesEvent.SortClicked) }) }
            } else {
                null
            }
        )
        state.deleted?.let { deleted ->
            DeletedRecipeBanner(
                name = deleted.recipe.name,
                secondsLeft = state.undoSecondsLeft,
                onUndo = { onEvent(SavedRecipesEvent.UndoClicked) },
                modifier = Modifier.padding(start = ContentPadding, end = ContentPadding, top = ContentPadding)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                state.isLoading -> Loading()
                state.hasError -> Message(
                    text = stringResource(id = R.string.saved_load_error),
                    modifier = Modifier.padding(ContentPadding)
                )
                state.isEmptyOverall -> EmptySavedState(onCreate = { onEvent(SavedRecipesEvent.AddClicked) })
                else -> {
                    SavedList(state = state, onEvent = onEvent)
                    FloatingActionButton(
                        onClick = { onEvent(SavedRecipesEvent.AddClicked) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(MaterialTheme.spacing.large),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_figma_add),
                            contentDescription = stringResource(id = R.string.cd_saved_add),
                            modifier = Modifier.size(AddIconSize)
                        )
                    }
                }
            }
        }
    }
    if (state.isSortMenuOpen) {
        SortMenu(
            selected = state.sort,
            onConfirm = { onEvent(SavedRecipesEvent.SortChanged(it)) },
            onDismiss = { onEvent(SavedRecipesEvent.SortDismissed) }
        )
    }
}

/** "Ordenar" del encabezado (Figma `557:2740`). */
@Composable
private fun SortAction(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = SortTouchHeight)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Text(
            text = stringResource(id = R.string.saved_sort),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

/**
 * Lista con búsqueda, chips, tarjetas y aviso. Todo va dentro de la cuadrícula para que "Sin conexión" quede debajo de
 * las tarjetas también cuando no caben en la pantalla (CA-SAV-02-09).
 */
@Composable
private fun SavedList(
    state: SavedRecipesUiState,
    onEvent: (SavedRecipesEvent) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(CardColumns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ContentPadding,
            end = ContentPadding,
            top = ContentPadding,
            bottom = FloatingActionButtonSpace
        ),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
        verticalArrangement = Arrangement.spacedBy(ContentGap)
    ) {
        item(key = "buscador", span = { GridItemSpan(maxLineSpan) }) {
            RecipeSearchField(
                query = state.query,
                placeholder = stringResource(id = R.string.saved_search_placeholder),
                onQueryChanged = { onEvent(SavedRecipesEvent.QueryChanged(it)) },
                onClearQuery = { onEvent(SavedRecipesEvent.ClearQuery) }
            )
        }
        item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
            SavedChips(
                selected = state.filter,
                onSelected = { onEvent(SavedRecipesEvent.FilterChanged(it)) }
            )
        }
        if (state.cards.isEmpty()) {
            item(key = "vacio", span = { GridItemSpan(maxLineSpan) }) {
                Message(text = emptyChipText(state))
            }
        } else {
            items(items = state.cards, key = { it.id }) { card ->
                SavedRecipeCard(
                    card = card,
                    onClick = { onEvent(SavedRecipesEvent.CardClicked(card.id)) },
                    onFavoriteToggled = { onEvent(SavedRecipesEvent.FavoriteToggled(card.id)) }
                )
            }
        }
        if (state.hasPendingSync) {
            item(key = "sin_conexion", span = { GridItemSpan(maxLineSpan) }) {
                OfflineCard()
            }
        }
    }
}

/** Chips "Favoritas", "Creadas por mí" y "Recientes" (Figma `633:2723`). */
@Composable
private fun SavedChips(
    selected: SavedFilter,
    onSelected: (SavedFilter) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
        SavedFilter.entries.forEach { filter ->
            RecipeFilterPill(
                text = filter.label(),
                selected = filter == selected,
                onClick = { onSelected(filter) }
            )
        }
    }
}

@Composable
private fun SavedFilter.label(): String = stringResource(
    id = when (this) {
        SavedFilter.FAVORITES -> R.string.saved_chip_favorites
        SavedFilter.OWN -> R.string.saved_chip_own
        SavedFilter.RECENT -> R.string.saved_chip_recent
    }
)

/** Texto de un chip sin tarjetas: sin coincidencias (CA-SAV-02-04) o sin recetas (CA-SAV-02-08). */
@Composable
private fun emptyChipText(state: SavedRecipesUiState): String = stringResource(
    id = when {
        state.query.isNotBlank() -> R.string.saved_no_results
        state.filter == SavedFilter.OWN -> R.string.saved_empty_own
        else -> R.string.saved_empty_favorites
    }
)

/** Vacío del Figma `557:2596`: sin ninguna receta guardada (CA-SAV-02-07). */
@Composable
private fun EmptySavedState(onCreate: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EmptyGap, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ContentPadding)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_figma_book),
            contentDescription = null,
            tint = GreenCheck,
            modifier = Modifier
                .size(EmptyIconSize)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            text = stringResource(id = R.string.saved_empty_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(id = R.string.saved_empty_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = EmptyBodyLineHeight)
        )
        RecipeActionButton(
            text = stringResource(id = R.string.saved_empty_button),
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Tarjeta "Sin conexión" del Figma `633:2723` (CA-SAV-02-09). */
@Composable
private fun OfflineCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recipeCardShadow(),
        colors = CardDefaults.cardColors(containerColor = GreenSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(OfflineRadius),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(OfflineGap),
            modifier = Modifier.padding(OfflinePadding)
        ) {
            Text(
                text = stringResource(id = R.string.saved_offline_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = offlineTextStyle().copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = stringResource(id = R.string.saved_offline_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = offlineTextStyle()
            )
        }
    }
}

@Composable
private fun offlineTextStyle(): TextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp)

@Composable
private fun Loading() {
    val loading = stringResource(id = R.string.cd_saved_loading)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { contentDescription = loading }
        )
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MessageVerticalPadding)
    )
}

// Medidas del Figma 633:2723 y 557:2585. No son de formulario, así que no van en FormMetrics.
private const val CardColumns = 2
private val ContentPadding = 20.dp
private val ContentGap = 16.dp
private val CardGap = 12.dp
private val ChipGap = 8.dp
private val SortTouchHeight = 48.dp
private val AddIconSize = 22.dp
private val EmptyGap = 14.dp
private val EmptyIconSize = 74.dp
private val EmptyBodyLineHeight = 19.sp
private val OfflineRadius = 20.dp
private val OfflinePadding = 16.dp
private val OfflineGap = 8.dp
private val MessageVerticalPadding = 32.dp

/** Espacio al final de la lista para que el botón "+" no tape la última fila (FAB de 56 dp más su margen). */
private val FloatingActionButtonSpace = 88.dp

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SavedRecipesScreenPreview() {
    SmartfoodTheme {
        SavedRecipesScreen(
            state = SavedRecipesUiState(
                isLoading = false,
                hasPendingSync = true,
                cards = listOf(
                    SavedRecipeCardData(
                        id = "pasta-verde",
                        name = "Pasta verde rápida",
                        photo = "receta_pasta_verde",
                        servings = 2,
                        minutes = 20,
                        isFavorite = true,
                        isOwn = false,
                        lastChangeMillis = 1_000L,
                        isPendingSync = true
                    )
                )
            ),
            onEvent = {}
        )
    }
}
