package com.smartfood.app.feature.recipes.ui.own

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeDraft
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeException
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.feature.recipes.domain.usecase.GetOwnRecipeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.SaveOwnRecipeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Nombre: OwnRecipeEditorViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: ViewModel del editor de recetas propias (spec 010, HU-SAV-03 y 04, T013). Las reglas viven en
 * `ValidateOwnRecipeUseCase`; aquí solo se guarda el borrador y se traduce el resultado en estado o navegación.
 *
 * @param save Valida y guarda la receta.
 * @param get Lee la receta que se edita.
 * @param catalog Ingredientes para la hoja de "Añadir ingrediente".
 */
@HiltViewModel
class OwnRecipeEditorViewModel @Inject constructor(
    private val save: SaveOwnRecipeUseCase,
    private val get: GetOwnRecipeUseCase,
    private val catalog: RecipeCatalog,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OwnRecipeEditorUiState())
    val uiState: StateFlow<OwnRecipeEditorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<OwnRecipeEditorEffect>(Channel.BUFFERED)
    val effects: Flow<OwnRecipeEditorEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val ingredients = catalog.ingredients().values.sortedBy { it.name }
            _uiState.update { it.copy(catalog = ingredients) }
        }
    }

    /** Carga la receta [id]; con `null` el formulario queda vacío. Se llama una vez por ruta. */
    fun initialize(id: String?) {
        if (id == null || _uiState.value.recipeId == id) return
        _uiState.update { it.copy(recipeId = id, isLoading = true) }
        viewModelScope.launch {
            get(id).fold(
                onSuccess = { recipe ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            draft = state.draft.copy(
                                name = recipe.name,
                                category = recipe.category,
                                ingredients = recipe.ingredients,
                                cookMinutes = recipe.cookMinutes.toString(),
                                preparation = recipe.preparation,
                            ),
                        )
                    }
                },
                onFailure = { error -> _uiState.update { it.copy(isLoading = false, error = error.toOwnRecipeError()) } },
            )
        }
    }

    /** Recibe las acciones de la pantalla. */
    fun onEvent(event: OwnRecipeEditorEvent) {
        when (event) {
            is OwnRecipeEditorEvent.NameChanged -> editDraft { copy(name = event.value) }
            OwnRecipeEditorEvent.CategoryClicked -> _uiState.update { it.copy(isCategorySheetOpen = true) }
            OwnRecipeEditorEvent.CategoryDismissed -> _uiState.update { it.copy(isCategorySheetOpen = false) }
            is OwnRecipeEditorEvent.CategoryPicked -> {
                _uiState.update { it.copy(isCategorySheetOpen = false) }
                editDraft { copy(category = event.value) }
            }
            is OwnRecipeEditorEvent.CookMinutesChanged ->
                editDraft { copy(cookMinutes = event.value.filter(Char::isDigit).take(MAX_MINUTE_DIGITS)) }
            is OwnRecipeEditorEvent.PreparationChanged -> editDraft { copy(preparation = event.value) }
            OwnRecipeEditorEvent.AddIngredientClicked -> _uiState.update { it.copy(ingredientSheet = IngredientSheetState()) }
            is OwnRecipeEditorEvent.IngredientClicked -> openIngredient(event.ingredientId)
            is OwnRecipeEditorEvent.IngredientQueryChanged -> editSheet { copy(query = event.value) }
            is OwnRecipeEditorEvent.IngredientPicked -> editSheet { copy(selectedId = event.ingredientId) }
            is OwnRecipeEditorEvent.IngredientQuantityChanged ->
                editSheet { copy(quantity = event.value, isQuantityInvalid = false) }
            OwnRecipeEditorEvent.IngredientConfirmed -> confirmIngredient()
            OwnRecipeEditorEvent.IngredientRemoved -> removeIngredient()
            OwnRecipeEditorEvent.IngredientSheetDismissed -> _uiState.update { it.copy(ingredientSheet = null) }
            OwnRecipeEditorEvent.SaveClicked -> saveRecipe()
            OwnRecipeEditorEvent.DiscardClicked -> send(OwnRecipeEditorEffect.Close)
            OwnRecipeEditorEvent.DeleteClicked -> {
                val state = _uiState.value
                state.recipeId?.let { send(OwnRecipeEditorEffect.OpenDelete(it, state.draft.name.trim())) }
            }
        }
    }

    /** Cambia el borrador y quita el aviso: al corregir, el rojo desaparece (CA-SAV-04-03). */
    private fun editDraft(change: OwnRecipeDraft.() -> OwnRecipeDraft) {
        _uiState.update { it.copy(draft = it.draft.change(), error = null) }
    }

    private fun editSheet(change: IngredientSheetState.() -> IngredientSheetState) {
        _uiState.update { state -> state.copy(ingredientSheet = state.ingredientSheet?.change()) }
    }

    private fun openIngredient(id: String) {
        val line = _uiState.value.draft.ingredients.firstOrNull { it.ingredientId == id } ?: return
        _uiState.update {
            it.copy(
                ingredientSheet = IngredientSheetState(
                    editingId = id,
                    selectedId = id,
                    quantity = line.quantity.toQuantityText(),
                ),
            )
        }
    }

    private fun confirmIngredient() {
        val sheet = _uiState.value.ingredientSheet ?: return
        val id = sheet.selectedId ?: return
        val quantity = sheet.quantity.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
        if (quantity == null) {
            editSheet { copy(isQuantityInvalid = true) }
            return
        }
        // Si el ingrediente ya estaba, se actualiza: una receta no lo repite (CA-SAV-03-05).
        val others = _uiState.value.draft.ingredients.filterNot { it.ingredientId == id || it.ingredientId == sheet.editingId }
        _uiState.update { it.copy(ingredientSheet = null) }
        editDraft { copy(ingredients = others + RecipeIngredient(id, quantity)) }
    }

    private fun removeIngredient() {
        val id = _uiState.value.ingredientSheet?.editingId ?: return
        _uiState.update { it.copy(ingredientSheet = null) }
        editDraft { copy(ingredients = ingredients.filterNot { line -> line.ingredientId == id }) }
    }

    private fun saveRecipe() {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            save(state.recipeId, state.draft).fold(
                onSuccess = { result ->
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.send(OwnRecipeEditorEffect.Saved(result.recipe.id, result.recipe.name, state.recipeId != null))
                },
                onFailure = { error -> _uiState.update { it.copy(isSaving = false, error = error.toOwnRecipeError()) } },
            )
        }
    }

    private fun send(effect: OwnRecipeEditorEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun Throwable.toOwnRecipeError(): OwnRecipeError =
        (this as? OwnRecipeException)?.error ?: OwnRecipeError.Unknown(message)

    /** Cantidad sin ceros de más: 2,0 se escribe "2". */
    private fun Double.toQuantityText(): String =
        if (this == toLong().toDouble()) toLong().toString() else toString()

    private companion object {
        const val MAX_MINUTE_DIGITS = 4
    }
}
