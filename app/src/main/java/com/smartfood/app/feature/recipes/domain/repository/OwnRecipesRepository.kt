package com.smartfood.app.feature.recipes.domain.repository

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: OwnRecipesRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Contrato de persistencia para recetas creadas por la persona (spec 010, RF-SAV-02, T009).
 */
interface OwnRecipesRepository {
    /** Recetas propias del usuario, incluyendo metadatos de escrituras pendientes. */
    fun observeRecipes(): Flow<List<OwnRecipe>>

    /** Obtiene una receta propia por identificador. */
    suspend fun getRecipe(id: String): Result<OwnRecipe>

    /** Genera un identificador nuevo sin escribir en Firestore. */
    fun newRecipeId(): String

    /** Crea o actualiza una receta y devuelve si quedó sincronizada. */
    suspend fun saveRecipe(recipe: OwnRecipe, isNew: Boolean): Result<WriteOutcome>

    /** Elimina una receta propia. */
    suspend fun deleteRecipe(id: String): Result<WriteOutcome>
}
