package com.smartfood.app.feature.recipes.ui.own

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeException
import com.smartfood.app.feature.recipes.domain.usecase.DeleteOwnRecipeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.DeletedRecipeStore
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
 * Nombre: DeleteOwnRecipeViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Confirmación de "Eliminar receta" (spec 010, CA-SAV-05-02 y 05-03, T018). Al eliminar, deja la copia para
 * deshacer en [DeletedRecipeStore], y Guardadas muestra el aviso.
 *
 * @param savedStateHandle Argumento `recipeId` de `Route.DeleteOwnRecipe`.
 * @param delete Elimina la receta y su ♡.
 * @param store Entrega la copia a Guardadas.
 */
@HiltViewModel
class DeleteOwnRecipeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val delete: DeleteOwnRecipeUseCase,
    private val store: DeletedRecipeStore,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle.get<String>(RECIPE_ID_KEY)) {
        "DeleteOwnRecipe requiere recipeId"
    }

    private val _uiState = MutableStateFlow(DeleteOwnRecipeUiState())
    val uiState: StateFlow<DeleteOwnRecipeUiState> = _uiState.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)

    /** Emite cuando la receta se eliminó: hay que volver a Guardadas. */
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    /** "Sí, eliminar". */
    fun onConfirm() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            delete(recipeId).fold(
                onSuccess = { copy ->
                    store.publish(copy)
                    _deleted.send(Unit)
                },
                onFailure = { error ->
                    val typed = (error as? OwnRecipeException)?.error ?: OwnRecipeError.Unknown(error.message)
                    _uiState.update { it.copy(isDeleting = false, error = typed) }
                },
            )
        }
    }

    companion object {
        /** Nombre del argumento en la ruta. */
        const val RECIPE_ID_KEY = "recipeId"
    }
}

/**
 * Estado de la confirmación.
 *
 * @property isDeleting `true` mientras se elimina.
 * @property error Error del último intento.
 */
data class DeleteOwnRecipeUiState(
    val isDeleting: Boolean = false,
    val error: OwnRecipeError? = null,
)
