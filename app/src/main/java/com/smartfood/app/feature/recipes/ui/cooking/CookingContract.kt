package com.smartfood.app.feature.recipes.ui.cooking

import androidx.compose.runtime.Immutable
import com.smartfood.app.feature.recipes.domain.model.RecipeStep

/**
 * Nombre: CookingContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato UI para el flujo de cocina guiada ("Antes de empezar" y "Cocina guiada", spec 009, HU-RIA-04; Figma 557:2819 y 557:2781).
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
    val isFinishConfirmVisible: Boolean = false,
    val isSaving: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val isSavedOnPhone: Boolean = false,
    val errorMessage: String? = null,
) {
    val currentStep: RecipeStep?
        get() = steps.getOrNull(currentStepIndex)

    val isFirstStep: Boolean
        get() = currentStepIndex == 0

    val isLastStep: Boolean
        get() = steps.isNotEmpty() && currentStepIndex == steps.lastIndex

    val progressFraction: Float
        get() = if (steps.isEmpty()) 0f else (currentStepIndex + 1).toFloat() / steps.size
}

sealed interface CookingEvent {
    data object StartGuideClicked : CookingEvent
    data object NextStepClicked : CookingEvent
    data object PreviousStepClicked : CookingEvent
    data object ExitClicked : CookingEvent
    data object FinishClicked : CookingEvent
    data object ConfirmFinishClicked : CookingEvent
    data object DismissFinishDialog : CookingEvent
    data object StartTimerClicked : CookingEvent
    data object PauseTimerClicked : CookingEvent
    data object ResetTimerClicked : CookingEvent
    data object ResultDialogDismissed : CookingEvent
}

sealed interface CookingEffect {
    data object OpenGuide : CookingEffect
    data object NavigateBack : CookingEffect
    data object PlayTimerFinishedAlert : CookingEffect
}
