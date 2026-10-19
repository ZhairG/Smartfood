package com.smartfood.app.feature.recipes.domain.repository

import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.Recipe

/**
 * Nombre: RecipeCatalog.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato del catálogo de recetas e ingredientes (spec 009, RF-RIA-01, T002). La implementación lee los
 * JSON de `assets/` (#113); el agente y las pantallas solo conocen este contrato.
 */
interface RecipeCatalog {

    /** Todas las recetas del catálogo. */
    suspend fun recipes(): List<Recipe>

    /** Todos los ingredientes del catálogo, por su `id`. */
    suspend fun ingredients(): Map<String, Ingredient>
}
