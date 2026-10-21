package com.smartfood.app.feature.recipes.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.recipes.domain.model.RecipeDetail
import com.smartfood.app.feature.recipes.domain.usecase.BuildRecipeIngredientsUseCase
import com.smartfood.app.feature.recipes.domain.usecase.GetRecipeDetailUseCase
import com.smartfood.app.feature.recipes.domain.model.Store
import com.smartfood.app.feature.recipes.domain.usecase.GetPreferredStoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Nombre: RecipeDetailViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel del detalle (spec 009, HU-RIA-04, T008). Lee la receta del catálogo, observa el inventario y
 * delega el escalado y el costo en [BuildRecipeIngredientsUseCase]; las porciones se recalculan al cambiarlas
 * (CA-RIA-04-02). No repite reglas del dominio.
 *
 * @param savedStateHandle Argumento `recipeId` de `Route.RecipeDetail`.
 * @param getDetail Lectura de la receta.
 * @param buildIngredients Escalado, agrupación y costo.
 * @param observeInventory Inventario actual para "En tu inventario".
 * @param getPreferredStore "Mi supermercado" del Perfil para los precios (CA-RIA-04-07).
 */
@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDetail: GetRecipeDetailUseCase,
    private val buildIngredients: BuildRecipeIngredientsUseCase,
    observeInventory: ObserveInventoryUseCase,
    getPreferredStore: GetPreferredStoreUseCase,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle.get<String>(RECIPE_ID_KEY)) {
        "Falta el argumento '$RECIPE_ID_KEY' de Route.RecipeDetail"
    }

    private val detail = MutableStateFlow<DetailState>(DetailState.Loading)
    private val servingsOverride = MutableStateFlow<Int?>(null)
    private val _effects = Channel<RecipeDetailEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso, recolectados por el grafo de recetas. */
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            detail.value = getDetail(recipeId).fold(
                onSuccess = { DetailState.Ready(it) },
                onFailure = { DetailState.Error },
            )
        }
    }

    /**
     * Estado del detalle. Si el inventario no se puede leer, los ingredientes se calculan con el inventario vacío en
     * lugar de cerrar la app.
     */
    val uiState: StateFlow<RecipeDetailUiState> = combine(
        detail,
        observeInventory().catch { emit(emptyList()) },
        servingsOverride,
        // D1 mientras se lee el Perfil; se vuelve a leer cada vez que la pantalla se observa de nuevo.
        flow { emit(Store.D1); emit(getPreferredStore()) },
    ) { state, sections, override, store ->
        when (state) {
            DetailState.Loading -> RecipeDetailUiState(isLoading = true, recipeId = recipeId)
            DetailState.Error -> RecipeDetailUiState(
                isLoading = false,
                hasError = true,
                recipeId = recipeId,
            )

            is DetailState.Ready -> {
                val recipe = state.detail.recipe
                val servings = (override ?: recipe.servings).coerceIn(MinServings, MaxServings)
                val built = buildIngredients(
                    detail = state.detail,
                    inventory = sections.flatMap { section -> section.items },
                    servings = servings,
                    store = store,
                )
                RecipeDetailUiState(
                    isLoading = false,
                    recipeId = recipe.id,
                    name = recipe.name,
                    photo = recipe.photo,
                    prepMinutes = recipe.prepMinutes,
                    cookMinutes = recipe.cookMinutes,
                    restMinutes = recipe.restMinutes,
                    difficulty = recipe.difficulty,
                    servings = servings,
                    groups = built.groups,
                    store = store,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMs),
        initialValue = RecipeDetailUiState(recipeId = recipeId)
    )

    fun onEvent(event: RecipeDetailEvent) {
        when (event) {
            RecipeDetailEvent.StartCooking -> viewModelScope.launch {
                _effects.send(RecipeDetailEffect.OpenCookingIntro(recipeId, uiState.value.servings))
            }
            RecipeDetailEvent.DecreaseServings -> changeServings(delta = -1)
            RecipeDetailEvent.IncreaseServings -> changeServings(delta = 1)
        }
    }

    private fun changeServings(delta: Int) {
        val recipe = (detail.value as? DetailState.Ready)?.detail?.recipe ?: return
        val current = servingsOverride.value ?: recipe.servings
        servingsOverride.update { (current + delta).coerceIn(MinServings, MaxServings) }
    }

    /** Estado de lectura del catálogo. */
    private sealed interface DetailState {
        data object Loading : DetailState
        data class Ready(val detail: RecipeDetail) : DetailState
        data object Error : DetailState
    }

    companion object {
        /** Nombre del argumento de `Route.RecipeDetail` en el `SavedStateHandle`. */
        const val RECIPE_ID_KEY = "recipeId"

        /** Límites del selector de porciones (CA-RIA-04-02). */
        const val MinServings = 1
        const val MaxServings = 12

        /** Igual que las demás pantallas: se deja de observar un rato después de salir. */
        private const val StopTimeoutMs = 5_000L
    }
}
