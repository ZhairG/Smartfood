package com.smartfood.app.feature.recipes.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.theme.GreyDivider
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.SurfaceLight
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import com.smartfood.app.feature.recipes.ui.components.RecipePhoto
import com.smartfood.app.feature.recipes.ui.components.RecipesHeader
import com.smartfood.app.feature.recipes.ui.components.formatCop

/**
 * Nombre: RecipesDiscoverScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla Descubrir de la pestaña Recetas (spec 009, HU-RIA-01, CA-RIA-01-01 a 01-04, T007), fiel al
 * Figma `557:3028`: encabezado verde, búsqueda, tarjeta "¿Qué cocinamos hoy?", filtros y tarjetas de receta con foto,
 * nombre, tiempo, kcal y costo. Sin ♡ (spec §9). Es stateless: recibe el estado y avisa los eventos.
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
                SearchField(
                    query = state.query,
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
                        onClick = { onEvent(RecipesDiscoverEvent.RecipeClicked(recipe.id)) }
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
            color = MaterialTheme.colorScheme.primaryContainer,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp)
        )
        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.recipes_suggest),
            onClick = { onSuggestRecipes?.invoke() },
            enabled = onSuggestRecipes != null
        )
    }
}

/** Búsqueda del Figma (`557:3035`): píldora gris con la lupa y la "x" al escribir. */
@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
    val placeholder = stringResource(id = R.string.recipes_search_placeholder)
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SearchHeight)
                    .background(color = SurfaceLight, shape = CircleShape)
                    .padding(horizontal = SearchSidePadding)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_search_bar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SearchIconSize)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = SearchTextGap)
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_figma_clear),
                            contentDescription = stringResource(id = R.string.cd_recipes_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(ClearIconSize)
                        )
                    }
                }
            }
        }
    )
}

/** Filtros del Figma (`557:3044`): "Para ti", "≤ 30 min" y "Económicas". */
@Composable
private fun RecipeFilters(
    selected: RecipeFilter,
    onSelected: (RecipeFilter) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(FilterGap)) {
        RecipeFilter.entries.forEach { filter ->
            FilterPill(
                text = filter.label(),
                selected = filter == selected,
                onClick = { onSelected(filter) }
            )
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else GreyDivider,
                shape = CircleShape
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = FilterSidePadding, vertical = FilterVerticalPadding)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

/**
 * Tarjeta de receta del Figma (`557:3051`).
 *
 * @param card Datos de la receta.
 * @param onClick Abre el detalle.
 */
@Composable
private fun RecipeCard(
    card: RecipeCardUi,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(CardRadius)
    val highlighted = card.expiringIngredient != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = CardElevation, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (highlighted) Modifier.border(1.dp, MaterialTheme.colorScheme.secondary, shape) else Modifier
            )
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        RecipePhoto(
            photo = card.photo,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PhotoAspectRatio)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(CardTextGap),
            modifier = Modifier.padding(horizontal = CardTextSidePadding, vertical = CardTextVerticalPadding)
        ) {
            if (card.expiringIngredient != null) {
                Text(
                    text = stringResource(id = R.string.recipes_expiring_badge, card.expiringIngredient.lowercase()),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // El Figma deja la línea del aviso vacía para que los nombres queden alineados.
                Spacer(modifier = Modifier.height(BadgeLineHeight))
            }
            Text(
                text = card.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    id = R.string.recipes_card_meta,
                    card.minutes,
                    card.kcal,
                    formatCop(card.costCop)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp),
                maxLines = 2
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
private val PhotoAspectRatio = 173f / 145f
private val CardTextSidePadding = 10.dp
private val CardTextVerticalPadding = 8.dp
private val CardTextGap = 5.dp
private val BadgeLineHeight = 12.dp
private val HeroPadding = 16.dp
private val HeroGap = 12.dp
private val HeroElevation = 3.dp
private val SearchHeight = 48.dp
private val SearchSidePadding = 16.dp
private val SearchTextGap = 8.dp
private val SearchIconSize = 20.dp
private val ClearIconSize = 18.dp
private val FilterGap = 8.dp
private val FilterSidePadding = 12.dp
private val FilterVerticalPadding = 7.dp
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
