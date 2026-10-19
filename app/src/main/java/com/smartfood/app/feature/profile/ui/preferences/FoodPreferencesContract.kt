package com.smartfood.app.feature.profile.ui.preferences

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.profile.domain.model.Cuisine
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.MealBudget
import com.smartfood.app.feature.profile.domain.model.SpiceLevel

/**
 * Nombre: FoodPreferencesContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de "Preferencias alimentarias" (spec 008, HU-PER-03, Figma `376:1260`, `593:1976` y
 * `593:1898`): estado, eventos y efectos. El borrador ([FoodPreferencesUiState.draft]) se compara con lo guardado
 * ([FoodPreferencesUiState.saved]) para habilitar "Guardar Preferencias" (CA-PER-03-03).
 */

/** Aviso abierto sobre la pantalla. */
sealed interface FoodPreferencesDialog {
    /** "¿Desea guardar los cambios?" (Figma `593:1976`, CA-PER-03-03). */
    data object ConfirmSave : FoodPreferencesDialog

    /** "Guardado · Preferencias se actualizó" (Figma `593:1898`, CA-PER-03-03), que se cierra solo. */
    data object Saved : FoodPreferencesDialog
}

/**
 * Estado de "Preferencias alimentarias".
 *
 * @property isLoading `true` hasta leer las preferencias guardadas.
 * @property saved Preferencias que están en Firestore, contra las que se comparan los cambios.
 * @property draft Preferencias tal como se ven en la pantalla, todavía sin guardar.
 * @property isSaving `true` mientras se guarda: se ve el diálogo de carga.
 * @property error Último error de lectura o de guardado.
 * @property dialog Aviso abierto, o `null`.
 */
data class FoodPreferencesUiState(
    val isLoading: Boolean = true,
    val saved: FoodPreferences = FoodPreferences(),
    val draft: FoodPreferences = FoodPreferences(),
    val isSaving: Boolean = false,
    val error: AuthError? = null,
    val dialog: FoodPreferencesDialog? = null,
) {
    /** `true` si algún chip difiere de lo guardado (CA-PER-03-03). */
    val hasChanges: Boolean
        get() = draft != saved

    /** "Guardar Preferencias" se habilita al cambiar algo y no mientras carga o guarda (CA-PER-03-03). */
    val isSaveEnabled: Boolean
        get() = hasChanges && !isSaving && !isLoading
}

/** Lo que el usuario hace en "Preferencias alimentarias". */
sealed interface FoodPreferencesEvent {
    /** Toca una restricción: se agrega o se quita (varias permitidas, CA-PER-03-02). */
    data class RestrictionToggled(val restriction: DietaryRestriction) : FoodPreferencesEvent

    /** Toca una cocina: se agrega o se quita (varias permitidas, CA-PER-03-02). */
    data class CuisineToggled(val cuisine: Cuisine) : FoodPreferencesEvent

    /** Toca un nivel de picante: si ya estaba elegido, se quita (solo uno, CA-PER-03-02). */
    data class SpiceSelected(val level: SpiceLevel) : FoodPreferencesEvent

    /** Toca un presupuesto: si ya estaba elegido, se quita (solo uno, CA-PER-03-02). */
    data class BudgetSelected(val budget: MealBudget) : FoodPreferencesEvent

    data object SaveClicked : FoodPreferencesEvent
    data object SaveConfirmed : FoodPreferencesEvent
    data object DialogDismissed : FoodPreferencesEvent
    data object BackClicked : FoodPreferencesEvent
}

/** Efectos de un solo uso. */
sealed interface FoodPreferencesEffect {
    /** Vuelve al Perfil. */
    data object NavigateBack : FoodPreferencesEffect
}
