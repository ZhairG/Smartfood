package com.smartfood.app.feature.recipes.ui.own

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.recipes.domain.usecase.GetOwnRecipeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.SaveOwnRecipeUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel del editor de recetas propias (spec 010, T013). */
class OwnRecipeEditorViewModel @Inject constructor(
    private val save: SaveOwnRecipeUseCase,
    private val get: GetOwnRecipeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OwnRecipeEditorUiState())
    val uiState: StateFlow<OwnRecipeEditorUiState> = _uiState.asStateFlow()
    private val _effects = Channel<OwnRecipeEditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun initialize(id: String?) {
        if (id == null || _uiState.value.recipeId == id) return
        _uiState.update { it.copy(recipeId = id) }
        viewModelScope.launch { get(id).onSuccess { recipe -> _uiState.update { state -> state.copy(draft = state.draft.copy(name = recipe.name, category = recipe.category, ingredients = recipe.ingredients, cookMinutes = recipe.cookMinutes.toString(), preparation = recipe.preparation)) } } }
    }

    fun onEvent(event: OwnRecipeEditorEvent) {
        when (event) {
            is OwnRecipeEditorEvent.NameChanged -> _uiState.update { it.copy(draft = it.draft.copy(name = event.value), error = null) }
            is OwnRecipeEditorEvent.CategoryPicked -> _uiState.update { it.copy(draft = it.draft.copy(category = event.value), error = null) }
            is OwnRecipeEditorEvent.CookMinutesChanged -> _uiState.update { it.copy(draft = it.draft.copy(cookMinutes = event.value), error = null) }
            is OwnRecipeEditorEvent.PreparationChanged -> _uiState.update { it.copy(draft = it.draft.copy(preparation = event.value), error = null) }
            OwnRecipeEditorEvent.SaveClicked -> save()
        }
    }

    private fun save() = viewModelScope.launch {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, error = null) }
        save(state.recipeId, state.draft).fold(
            onSuccess = { result -> _effects.send(OwnRecipeEditorEffect.Saved(result.recipe.id, result.recipe.name, state.recipeId != null)); _uiState.update { it.copy(isSaving = false) } },
            onFailure = { error -> _uiState.update { it.copy(isSaving = false, error = (error as? com.smartfood.app.feature.recipes.domain.model.OwnRecipeException)?.error) } }
        )
    }
}
