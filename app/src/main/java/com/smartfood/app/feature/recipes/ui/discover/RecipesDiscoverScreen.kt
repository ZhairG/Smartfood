package com.smartfood.app.feature.recipes.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenSoft
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipeCard
import com.smartfood.app.feature.recipes.ui.components.RecipeFilterPill
import com.smartfood.app.feature.recipes.ui.components.RecipeSearchField
import com.smartfood.app.feature.recipes.ui.components.RecipesHeader

/**
 * Nombre: RecipesDiscoverScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla Descubrir de la pestaña Recetas (spec 009, HU-RIA-01, CA-RIA-01-01 a 01-04, T007), fiel al
 * Figma `557:3028`: encabezado verde, búsqueda, tarjeta "¿Qué cocinamos hoy?", filtros y tarjetas de receta con foto,
 * nombre, tiempo, kcal, costo y ♡ (spec 010, CA-SAV-01-01). Es stateless: recibe el estado y avisa los eventos.
 *
 * @param state Estado calculado por el ViewModel.
 * @param onEvent Acciones de la persona.
 * @param modifier Modificador opcional.
 * @param onSuggestRecipes Abre "Sugerencia con IA"; `null` hasta que exista el flujo (#115) y el botón queda
 *   deshabilitado.
 */
@Composable
fun RecipesDiscoverScreen(
    state: RecipesDiscoverUiState,
    onEvent: (RecipesDiscoverEvent) -> Unit,
    modifier: Modifier = Modifier,
    onSuggestRecipes: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        RecipesHeader()
        LazyVerticalGrid(
            columns = GridCells.Fixed(CardColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(CardGap),
            verticalArrangement = Arrangement.spacedBy(ContentGap)
        ) {
            item(key = "buscador", span = { GridItemSpan(maxLineSpan) }) {
                RecipeSearchField(
                    query = state.query,
                    placeholder = stringResource(id = R.string.recipes_search_placeholder),
                    onQueryChanged = { onEvent(RecipesDiscoverEvent.QueryChanged(it)) },
                    onClearQuery = { onEvent(RecipesDiscoverEvent.ClearQuery) }
                )
            }
            item(key = "tarjeta_ia", span = { GridItemSpan(maxLineSpan) }) {
                SuggestCard(onSuggestRecipes = onSuggestRecipes)
            }
            item(key = "filtros", span = { GridItemSpan(maxLineSpan) }) {
                RecipeFilters(
                    selected = state.filter,
                    onSelected = { onEvent(RecipesDiscoverEvent.FilterSelected(it)) }
                )
            }

            when {
                state.isLoading -> item(key = "cargando", span = { GridItemSpan(maxLineSpan) }) { Loading() }
                state.hasError -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    Message(stringResource(id = R.string.recipes_load_error))
                }

                state.recipes.isEmpty() -> item(key = "vacio", span = { GridItemSpan(maxLineSpan) }) {
                    Message(stringResource(id = R.string.recipes_empty))
                }

                else -> items(items = state.recipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        card = recipe,
                        isFavorite = state.favoriteIds.contains(recipe.id),
                        onClick = { onEvent(RecipesDiscoverEvent.RecipeClicked(recipe.id)) },
                        onFavoriteToggled = { onEvent(RecipesDiscoverEvent.FavoriteToggled(recipe.id)) }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta "¿Qué cocinamos hoy?" del Figma (`557:3039`).
 *
 * @param onSuggestRecipes Acción de "Sugerirme recetas"; `null` deja el botón deshabilitado.
 */
@Composable
private fun SuggestCard(onSuggestRecipes: (() -> Unit)?) {
    val shape = RoundedCornerShape(CardRadius)
    Column(
        verticalArrangement = Arrangement.spacedBy(HeroGap),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = HeroElevation, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(HeroPadding)
    ) {
        Text(
            text = stringResource(id = R.string.recipes_hero_title),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = stringResource(id = R.string.recipes_hero_body),
            // Figma: #DCFCE7, el token GreenSoft.
            color = GreenSoft,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp)
        )
        RecipeActionButton(
            text = stringResource(id = R.string.recipes_suggest),
            onClick = { onSuggestRecipes?.invoke() },
            enabled = onSuggestRecipes != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Filtros del Figma (`557:3044`): "Para ti", "≤ 30 min" y "Económicas". */
@Composable
private fun RecipeFilters(
    selected: RecipeFilter,
    onSelected: (RecipeFilter) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(FilterGap)) {
        RecipeFilter.entries.forEach { filter ->
            RecipeFilterPill(
                text = filter.label(),
                selected = filter == selected,
                onClick = { onSelected(filter) }
            )
        }
    }
}

@Composable
private fun Loading() {
    val loading = stringResource(id = R.string.cd_recipes_loading)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MessageVerticalPadding)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { contentDescription = loading }
        )
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MessageVerticalPadding)
    )
}

@Composable
private fun RecipeFilter.label(): String = stringResource(
    id = when (this) {
        RecipeFilter.FOR_YOU -> R.string.recipes_filter_for_you
        RecipeFilter.UNDER_30 -> R.string.recipes_filter_under_30
        RecipeFilter.ECONOMIC -> R.string.recipes_filter_economic
    }
)

// Medidas del Figma 557:3028. No son de formulario, así que no van en FormMetrics.
private const val CardColumns = 2
private val ContentPadding = 20.dp
private val ContentGap = 16.dp
private val CardGap = 12.dp
private val CardRadius = 20.dp
private val CardElevation = 3.dp
private val HeroPadding = 16.dp
private val HeroGap = 12.dp
private val HeroElevation = 3.dp
private val FilterGap = 8.dp
private val MessageVerticalPadding = 32.dp

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun RecipesDiscoverScreenPreview() {
    SmartfoodTheme {
        RecipesDiscoverScreen(
            state = RecipesDiscoverUiState(
                isLoading = false,
                recipes = listOf(
                    RecipeCardUi(
                        id = "pollo-guisado",
                        name = "Pollo guisado con arroz",
                        photo = "receta_pollo_guisado",
                        minutes = 35,
                        kcal = 520,
                        costCop = 8_400,
                        expiringIngredient = "Pollo"
                    ),
                    RecipeCardUi(
                        id = "pasta-verde",
                        name = "Pasta verde rápida",
                        photo = "receta_pasta_verde",
                        minutes = 25,
                        kcal = 480,
                        costCop = 6_200
                    )
                )
            ),
            onEvent = {}
        )
    }
}
