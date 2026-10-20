package com.smartfood.app.feature.profile.domain.usecase

import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * Nombre: FoodPreferencesUseCases.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Casos de uso de "Preferencias alimentarias" (spec 008, HU-PER-03, T011). Los enums del modelo ya limitan
 * lo que se puede elegir, así que estos casos solo leen y guardan; no repiten validaciones en el ViewModel.
 */

/**
 * Lee las preferencias del usuario con sesión (CA-PER-03-04).
 *
 * @param repository Contrato del perfil; las preferencias viven en `users/{uid}`.
 */
class GetFoodPreferencesUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): Result<FoodPreferences> = repository.getPreferences()
}

/**
 * Guarda las preferencias elegidas y reemplaza el mapa anterior (CA-PER-03-04).
 *
 * @param repository Contrato del perfil; las preferencias viven en `users/{uid}`.
 */
class SaveFoodPreferencesUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(preferences: FoodPreferences): Result<Unit> = repository.savePreferences(preferences)
}
