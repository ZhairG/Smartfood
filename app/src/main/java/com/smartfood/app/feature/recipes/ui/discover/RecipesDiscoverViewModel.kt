package com.smartfood.app.feature.recipes.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import com.smartfood.app.feature.recipes.domain.model.RecipeSuggestion
import com.smartfood.app.feature.recipes.domain.usecase.GetRecipesUseCase
import com.smartfood.app.feature.recipes.domain.usecase.RankRecipesForMeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.RecipeCatalogData
import com.smartfood.app.feature.recipes.domain.usecase.SearchRecipesUseCase
import com.smartfood.app.feature.recipes.domain.model.Store
import com.smartfood.app.feature.recipes.domain.usecase.GetPreferredStoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Nombre: RecipesDiscoverViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de Descubrir (spec 009, HU-RIA-01, T007). Lee el catálogo una vez, observa el inventario y
 * delega la búsqueda y el orden en los casos de uso; no repite reglas del dominio. "Para ti" usa la utilidad de cada
 * receta con el inventario actual (CA-RIA-01-02).
 *
 * @param getRecipes Lectura del catálogo.
 * @param searchRecipes Búsqueda por nombre o ingrediente (CA-RIA-01-04).
 * @param rankRecipes Orden y filtros (CA-RIA-01-02 y 01-03).
 * @param observeInventory Inventario actual para calcular costo y rescate.
 * @param getPreferredStore "Mi supermercado" del Perfil para los precios (CA-RIA-04-07).
 */
@HiltViewModel
class RecipesDiscoverViewModel @Inject constructor(
    getRecipes: GetRecipesUseCase,
    private val searchRecipes: SearchRecipesUseCase,
    private val rankRecipes: RankRecipesForMeUseCase,
    observeInventory: ObserveInventoryUseCase,
    getPreferredStore: GetPreferredStoreUseCase,
) : ViewModel() {

    private val catalog = MutableStateFlow<CatalogState>(CatalogState.Loading)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(RecipeFilter.FOR_YOU)

    private val _effects = Channel<RecipesDiscoverEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso (navegación al detalle), recolectados con `ObserveEffects` desde el grafo. */
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            catalog.value = getRecipes().fold(
                onSuccess = { CatalogState.Ready(it) },
                onFailure = { CatalogState.Error },
            )
        }
    }

    /**
     * Estado de la pantalla. Si el inventario no se puede leer, las recetas se ordenan con el inventario vacío en
     * lugar de cerrar la app.
     */
    val uiState: StateFlow<RecipesDiscoverUiState> = combine(
        catalog,
        observeInventory().catch { emit(emptyList()) },
        query,
        filter,
        // D1 mientras se lee el Perfil; se vuelve a leer cada vez que la pantalla se observa de nuevo.
        flow { emit(Store.D1); emit(getPreferredStore()) },
    ) { catalogState, sections, text, selectedFilter, store ->
        when (catalogState) {
            CatalogState.Loading -> RecipesDiscoverUiState(isLoading = true, query = text, filter = selectedFilter)
            CatalogState.Error -> RecipesDiscoverUiState(
                isLoading = false,
                hasError = true,
                query = text,
                filter = selectedFilter,
            )

            is CatalogState.Ready -> {
                val inventory = sections.flatMap { section -> section.items }
                val found = searchRecipes(catalogState.data.recipes, catalogState.data.ingredients, text)
                val suggestions = rankRecipes(found, catalogState.data.ingredients, inventory, selectedFilter, store)
                RecipesDiscoverUiState(
                    isLoading = false,
                    query = text,
                    filter = selectedFilter,
                    recipes = suggestions.map { it.toCard() },
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMs),
        initialValue = RecipesDiscoverUiState(isLoading = true)
    )

    fun onEvent(event: RecipesDiscoverEvent) {
        when (event) {
            is RecipesDiscoverEvent.QueryChanged -> query.value = event.query
            RecipesDiscoverEvent.ClearQuery -> query.value = ""
            is RecipesDiscoverEvent.FilterSelected -> filter.value = event.filter
            is RecipesDiscoverEvent.RecipeClicked ->
                viewModelScope.launch { _effects.send(RecipesDiscoverEffect.OpenRecipe(event.recipeId)) }
        }
    }

    private fun RecipeSuggestion.toCard() = RecipeCardUi(
        id = recipe.id,
        name = recipe.name,
        photo = recipe.photo,
        minutes = recipe.totalMinutes,
        kcal = recipe.kcal,
        costCop = costCop,
        expiringIngredient = expiringIngredientName,
    )

    /** Estado de lectura del catálogo. */
    private sealed interface CatalogState {
        data object Loading : CatalogState
        data class Ready(val data: RecipeCatalogData) : CatalogState
        data object Error : CatalogState
    }

    private companion object {
        /** Igual que las demás pantallas: se deja de observar un rato después de salir. */
        const val StopTimeoutMs = 5_000L
    }
}
