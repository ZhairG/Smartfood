package com.smartfood.app.feature.recipes.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.recipes.domain.model.AiContext
import com.smartfood.app.feature.recipes.domain.model.AiPlanOutcome
import com.smartfood.app.feature.recipes.domain.usecase.GeneratePlanUseCase
import com.smartfood.app.feature.recipes.domain.usecase.GetAiContextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Nombre: AiPlanViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel del flujo de la IA (spec 009, HU-RIA-02, T014; plan §5). Lee el contexto con
 * [GetAiContextUseCase], guarda lo que la persona elige y, al generar, muestra "Analizando opciones" al menos
 * [MIN_LOADING_MILLIS] (desviación aprobada, spec §9) mientras [GeneratePlanUseCase] calcula. "Cancelar" descarta el
 * cálculo y vuelve al contexto sin resultado (CA-RIA-02-02); "Ajustar" vuelve con los mismos datos (CA-RIA-02-08). No
 * calcula nada del agente: solo orquesta.
 *
 * @param getAiContext Inventario emparejado, restricciones y tienda.
 * @param generatePlan Plan de 3 días o sugerencias.
 * @param clock Reloj para saber qué vence en 0 a 3 días.
 */
@HiltViewModel
class AiPlanViewModel @Inject constructor(
    private val getAiContext: GetAiContextUseCase,
    private val generatePlan: GeneratePlanUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AiEffect>(Channel.BUFFERED)
    val effects: Flow<AiEffect> = _effects.receiveAsFlow()

    private var context: AiContext? = null
    private var generation: Job? = null

    init {
        viewModelScope.launch {
            val loaded = getAiContext()
            context = loaded
            val today = LocalDate.now(clock)
            val daysById = loaded.items.associate { it.id to ChronoUnit.DAYS.between(today, it.expiresOn).toInt() }
            val expiring = loaded.items.filter { (daysById.getValue(it.id)) in 0..EXPIRING_DAYS }
            _uiState.update {
                it.copy(
                    isLoadingContext = false,
                    detected = loaded.items,
                    expiringIds = expiring.mapTo(HashSet()) { item -> item.id },
                    firstExpiring = expiring.firstOrNull(),
                    firstExpiringDays = expiring.firstOrNull()?.let { item -> daysById.getValue(item.id) } ?: 0,
                    restrictions = loaded.restrictions,
                    store = loaded.store,
                    ingredients = loaded.ingredients,
                )
            }
        }
    }

    fun onEvent(event: AiEvent) {
        when (event) {
            is AiEvent.BudgetChanged -> _uiState.update { it.copy(budgetCop = roundBudget(event.cop)) }
            AiEvent.ServingsClicked -> _uiState.update { it.copy(sheet = AiSheet.Servings) }
            is AiEvent.ServingsPicked -> _uiState.update { it.copy(servings = event.servings, sheet = null) }
            AiEvent.TimeClicked -> _uiState.update { it.copy(sheet = AiSheet.Time) }
            is AiEvent.TimePicked -> _uiState.update { it.copy(maxMinutes = event.minutes, sheet = null) }
            AiEvent.SpicyToggled -> _uiState.update { it.copy(noSpicy = !it.noSpicy) }
            AiEvent.DismissSheet -> _uiState.update { it.copy(sheet = null) }
            AiEvent.GenerateClicked -> generate()
            AiEvent.CancelClicked -> {
                generation?.cancel()
                generation = null
                _uiState.update { it.copy(step = AiStep.Context) }
            }
            AiEvent.AdjustClicked -> _uiState.update { it.copy(step = AiStep.Context) }
            is AiEvent.RecipeClicked -> send(AiEffect.OpenRecipe(event.recipeId))
            AiEvent.EconomicRecipesClicked -> send(AiEffect.OpenEconomicRecipes)
            AiEvent.HelpClicked -> send(AiEffect.OpenHelp)
        }
    }

    private fun generate() {
        val loaded = context ?: return
        if (_uiState.value.step == AiStep.Loading) return
        _uiState.update { it.copy(step = AiStep.Loading, plan = null, reasons = emptyList(), suggestions = emptyList()) }
        val state = _uiState.value
        generation = viewModelScope.launch {
            val outcome = coroutineScope {
                val result = async {
                    generatePlan(loaded, state.budgetCop, state.servings, state.maxMinutes, state.noSpicy)
                }
                delay(MIN_LOADING_MILLIS)
                result.await()
            }
            _uiState.update {
                when (outcome) {
                    is AiPlanOutcome.Found -> it.copy(step = AiStep.Plan, plan = outcome.plan, reasons = outcome.reasons)
                    is AiPlanOutcome.NoPlan -> it.copy(step = AiStep.NoPlan, suggestions = outcome.suggestions)
                }
            }
        }
    }

    private fun send(effect: AiEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    /** Al múltiplo de $1.000 más cercano, entre $2.000 y $100.000. */
    private fun roundBudget(cop: Long): Long {
        val step = AiUiState.BUDGET_STEP_COP
        return ((cop + step / 2) / step * step).coerceIn(AiUiState.MIN_BUDGET_COP, AiUiState.MAX_BUDGET_COP)
    }

    companion object {
        /** Lo mínimo que se ve "Analizando opciones" (desviación aprobada, spec §9). */
        const val MIN_LOADING_MILLIS = 1_500L

        /** Un producto "por vencer" vence en 3 días o menos (spec §2). */
        const val EXPIRING_DAYS = 3
    }
}
