package com.smartfood.app.feature.recipes.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedRecipesData
import com.smartfood.app.feature.recipes.domain.model.SavedSort
import com.smartfood.app.feature.recipes.domain.usecase.ObserveSavedRecipesUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Nombre: SavedRecipesViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: ViewModel de Guardadas (spec 010, HU-SAV-02, T007). Guarda el chip, la búsqueda y el orden, delega el
 * resto en [ObserveSavedRecipesUseCase] y traduce las acciones en efectos de navegación. Si las recetas no se pueden
 * leer, muestra el error en lugar de cerrar la app (CA-SAV-02-11).
 *
 * @param toggleFavorite Marca o quita una receta como favorita.
 * @param observeSavedRecipes Combina las recetas guardadas y aplica filtro, búsqueda y orden.
 */
@HiltViewModel
class SavedRecipesViewModel @Inject constructor(
    private val toggleFavorite: ToggleFavoriteUseCase,
    observeSavedRecipes: ObserveSavedRecipesUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow(SavedFilter.FAVORITES)
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(SavedSort.RECENT)
    private val isSortMenuOpen = MutableStateFlow(false)

    private val _effects = Channel<SavedRecipesEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso (navegación), recolectados con `ObserveEffects` desde el grafo. */
    val effects: Flow<SavedRecipesEffect> = _effects.receiveAsFlow()

    /** Lo que llegó de las recetas guardadas: los datos o `null` si no se pudieron leer. */
    private val saved: Flow<SavedRecipesData?> = observeSavedRecipes(filter, query, sort)
        .map<SavedRecipesData, SavedRecipesData?> { it }
        .catch { emit(null) }

    /** Estado de la pantalla. */
    val uiState: StateFlow<SavedRecipesUiState> = combine(
        filter,
        query,
        sort,
        isSortMenuOpen,
        saved,
    ) { selectedFilter, text, selectedSort, sortMenuOpen, data ->
        SavedRecipesUiState(
            isLoading = false,
            hasError = data == null,
            filter = selectedFilter,
            query = text,
            sort = selectedSort,
            cards = data?.cards.orEmpty(),
            isEmptyOverall = data?.isEmptyOverall == true,
            hasPendingSync = data?.hasPendingSync == true,
            isSortMenuOpen = sortMenuOpen,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SavedRecipesUiState(),
    )

    /** Recibe las acciones de la pantalla. */
    fun onEvent(event: SavedRecipesEvent) {
        when (event) {
            is SavedRecipesEvent.FilterChanged -> filter.value = event.filter
            is SavedRecipesEvent.QueryChanged -> query.value = event.query
            SavedRecipesEvent.ClearQuery -> query.value = ""
            SavedRecipesEvent.SortClicked -> isSortMenuOpen.value = true
            SavedRecipesEvent.SortDismissed -> isSortMenuOpen.value = false
            is SavedRecipesEvent.SortChanged -> {
                sort.value = event.sort
                isSortMenuOpen.value = false
            }
            is SavedRecipesEvent.FavoriteToggled -> toggle(event.recipeId)
            is SavedRecipesEvent.CardClicked -> openCard(event.recipeId)
            SavedRecipesEvent.AddClicked -> send(SavedRecipesEffect.OpenNewRecipe)
        }
    }

    private fun toggle(recipeId: String) {
        val card = uiState.value.cards.firstOrNull { it.id == recipeId } ?: return
        val source = if (card.isOwn) FavoriteSource.OWN else FavoriteSource.CATALOG
        viewModelScope.launch { toggleFavorite(recipeId, source, !card.isFavorite) }
    }

    private fun openCard(recipeId: String) {
        val card = uiState.value.cards.firstOrNull { it.id == recipeId } ?: return
        send(if (card.isOwn) SavedRecipesEffect.OpenOwnRecipe(recipeId) else SavedRecipesEffect.OpenRecipe(recipeId))
    }

    private fun send(effect: SavedRecipesEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
