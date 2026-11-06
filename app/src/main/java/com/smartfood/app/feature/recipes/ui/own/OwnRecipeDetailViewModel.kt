package com.smartfood.app.feature.recipes.ui.own

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.repository.OwnRecipesRepository
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.feature.recipes.domain.usecase.ObserveFavoriteIdsUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Nombre: OwnRecipeDetailViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Estado del detalle de una receta propia (spec 010, CA-SAV-04-05, T014). Observa la receta, así que al
 * volver de "Editar receta" ya muestra los cambios.
 *
 * @param savedStateHandle Argumento `recipeId` de `Route.OwnRecipeDetail`.
 * @param recipes Recetas propias.
 * @param catalog Nombres y unidades de los ingredientes.
 * @param observeFavoriteIds Favoritas, para pintar el ♡.
 * @param toggleFavorite Marca o quita la favorita.
 */
@HiltViewModel
class OwnRecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    recipes: OwnRecipesRepository,
    catalog: RecipeCatalog,
    observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle.get<String>(RECIPE_ID_KEY)) {
        "OwnRecipeDetail requiere recipeId"
    }

    /** Estado de la pantalla. */
    val uiState: StateFlow<OwnRecipeDetailUiState> = combine(
        recipes.observeRecipes().map { list -> list.firstOrNull { it.id == recipeId } },
        flow { emit(catalog.ingredients()) },
        observeFavoriteIds().catch { emit(emptySet()) },
    ) { recipe, ingredients, favoriteIds ->
        OwnRecipeDetailUiState(
            isLoading = false,
            recipe = recipe,
            ingredients = ingredients,
            isFavorite = recipeId in favoriteIds,
        )
    }.catch { emit(OwnRecipeDetailUiState(isLoading = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), OwnRecipeDetailUiState())

    /** Marca o quita el ♡ de la receta. */
    fun onFavoriteToggled() {
        viewModelScope.launch { toggleFavorite(recipeId, FavoriteSource.OWN, !uiState.value.isFavorite) }
    }

    companion object {
        /** Nombre del argumento en la ruta. */
        const val RECIPE_ID_KEY = "recipeId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Estado del detalle.
 *
 * @property isLoading `true` hasta recibir los primeros datos.
 * @property recipe La receta, o `null` si no existe.
 * @property ingredients Ingredientes del catálogo por id.
 * @property isFavorite `true` si tiene ♡.
 */
data class OwnRecipeDetailUiState(
    val isLoading: Boolean = true,
    val recipe: OwnRecipe? = null,
    val ingredients: Map<String, Ingredient> = emptyMap(),
    val isFavorite: Boolean = false,
)
