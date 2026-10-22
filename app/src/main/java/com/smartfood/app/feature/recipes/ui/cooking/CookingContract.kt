package com.smartfood.app.feature.recipes.ui.cooking

import androidx.compose.runtime.Immutable
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.recipes.domain.model.RecipeStep

/**
 * Nombre: CookingContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato UI para el flujo de cocina guiada ("Antes de empezar" y "Cocina guiada", spec 009, HU-RIA-04;
 * Figma 557:2819 y 557:2781).
 */

@Immutable
data class CookingUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val recipeId: String = "",
    val name: String = "",
    val photo: String? = null,
    val servings: Int = 2,
    val beforeStarting: List<String> = emptyList(),
    val steps: List<RecipeStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val timerSecondsRemaining: Int = 0,
    val isTimerRunning: Boolean = false,
    /** El temporizador llegó a 0 y la alarma suena hasta que la persona la detiene (CA-RIA-04-04). */
    val isAlarmRinging: Boolean = false,
    val dialog: CookingDialog? = null,
) {
    val currentStep: RecipeStep?
        get() = steps.getOrNull(currentStepIndex)

    val isFirstStep: Boolean
        get() = currentStepIndex == 0

    val isLastStep: Boolean
        get() = steps.isNotEmpty() && currentStepIndex == steps.lastIndex

    val progressFraction: Float
        get() = if (steps.isEmpty()) 0f else (currentStepIndex + 1).toFloat() / steps.size

    /** El paso actual tiene tiempo y por eso muestra la tarjeta del temporizador (CA-RIA-04-04). */
    val hasTimer: Boolean
        get() = (currentStep?.minutes ?: 0) > 0
}

/** Diálogo del cierre de la cocina guiada (CA-RIA-04-05). */
sealed interface CookingDialog {

    /** "¿Cocinaste esta receta?". */
    data object ConfirmFinish : CookingDialog

    /** Se descuenta el inventario y se guarda el registro. */
    data object Saving : CookingDialog

    /**
     * Terminó de guardar.
     *
     * @property onPhone `true` si quedó en la cola sin conexión: "Guardado en tu teléfono" (spec 006 §13).
     */
    data class Saved(val onPhone: Boolean) : CookingDialog

    /** No se pudo guardar; nada se perdió y se puede volver a tocar "Terminar". */
    data class Failed(val error: InventoryError) : CookingDialog
}

sealed interface CookingEvent {
    data object StartGuideClicked : CookingEvent
    data object NextStepClicked : CookingEvent
    data object PreviousStepClicked : CookingEvent

    /** "Salir" de la barra: vuelve al detalle sin descontar nada (Figma `557:2786`). */
    data object ExitClicked : CookingEvent
    data object FinishClicked : CookingEvent
    data object ConfirmFinishClicked : CookingEvent
    data object DismissDialog : CookingEvent

    /** Tocar la tarjeta del temporizador lo inicia, lo pausa o detiene la alarma (desviación aprobada, spec §9). */
    data object TimerToggled : CookingEvent
    data object ResetTimerClicked : CookingEvent
}

sealed interface CookingEffect {
    data object OpenGuide : CookingEffect

    /** Vuelve al detalle de la receta, cerrando "Antes de empezar" y la guía. */
    data object ExitToDetail : CookingEffect

    /** El temporizador llegó a 0: empieza la alarma. */
    data object PlayTimerFinishedAlert : CookingEffect

    /** La persona detuvo la alarma, cambió de paso o salió. */
    data object StopTimerAlert : CookingEffect
}
