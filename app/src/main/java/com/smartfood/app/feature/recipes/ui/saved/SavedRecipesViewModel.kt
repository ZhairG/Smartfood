package com.smartfood.app.feature.recipes.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedSort
import com.smartfood.app.feature.recipes.domain.usecase.ObserveFavoriteIdsUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ObserveSavedRecipesUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Nombre: SavedRecipesViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: ViewModel de Guardadas (spec 010, HU-SAV-02, T007). Combina favoritas con el catálogo,
 * aplica chip, búsqueda y orden, y expone el estado para la pantalla.
 *
 * @param toggleFavorite Marca o quita una receta como favorita.
 * @param observeFavoriteIds Ids de las recetas de catálogo marcadas como favorita.
 * @param observeSavedRecipes Combina favoritas con el catálogo y aplica filtro, búsqueda y orden.
 */
@HiltViewModel
class SavedRecipesViewModel @Inject constructor(
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val observeSavedRecipes: ObserveSavedRecipesUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow<SavedFilter>(SavedFilter.FAVORITES)
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow<SavedSort>(SavedSort.RECENT)
    private val isSortMenuOpen = MutableStateFlow(false)

    /**
     * Estado de la pantalla.
     */
    val uiState: StateFlow<SavedRecipesUiState> = combine(
        filter,
        query,
        sort,
        observeSavedRecipes(filter, query, sort),
    ) { selectedFilter, text, selectedSort, data ->
        SavedRecipesUiState(
            filter = selectedFilter,
            query = text,
            sort = selectedSort,
            cards = data.cards,
            isEmptyOverall = data.isEmptyOverall,
            hasPendingSync = data.hasPendingSync,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SavedRecipesUiState(),
    )

    fun onEvent(event: SavedRecipesEvent) {
        when (event) {
            is SavedRecipesEvent.FilterChanged -> filter.value = event.filter
            is SavedRecipesEvent.QueryChanged -> query.value = event.query
            is SavedRecipesEvent.SortChanged -> sort.value = event.sort
            SavedRecipesEvent.AddClicked -> {}
            SavedRecipesEvent.UndoClicked -> {}
            is SavedRecipesEvent.CardClicked -> {}
            is SavedRecipesEvent.FavoriteToggled -> viewModelScope.launch {
                val isNowFavorite = uiState.value.cards.any { it.id == event.recipeId && it.isFavorite }
                toggleFavorite(event.recipeId, com.smartfood.app.feature.recipes.domain.model.FavoriteSource.CATALOG, !isNowFavorite)
            }
        }
    }

    fun openSortMenu() {
        isSortMenuOpen.value = true
    }

    fun closeSortMenu() {
        isSortMenuOpen.value = false
    }

    val isSortMenuOpenState: StateFlow<Boolean> = isSortMenuOpen
}
