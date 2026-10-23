package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.model.Store
import javax.inject.Inject

/**
 * Nombre: GetPreferredStoreUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Supermercado de los precios de las recetas (spec 009, CA-RIA-04-07, T033): el "Mi supermercado" de
 * Preferencias alimentarias (spec 008, CA-PER-03-05), traducido por nombre a [Store]. Si las preferencias no se pueden
 * leer o nunca se eligió, D1.
 *
 * @param getFoodPreferences Preferencias guardadas del Perfil.
 */
class GetPreferredStoreUseCase @Inject constructor(
    private val getFoodPreferences: GetFoodPreferencesUseCase,
) {

    /** Tienda elegida, o [Store.D1]. */
    suspend operator fun invoke(): Store = getFoodPreferences().getOrNull()
        ?.storeOrDefault
        ?.let { chosen -> Store.entries.firstOrNull { it.name == chosen.name } }
        ?: Store.D1
}
