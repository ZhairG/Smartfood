package com.smartfood.app.feature.recipes.data.catalog

import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Nombre: AssetsRecipeCatalog.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [RecipeCatalog] que lee `recetas.json` e `ingredientes.json` de `assets/` con
 * kotlinx.serialization (spec 009, plan §4.1, T004). Cada archivo se lee una sola vez por proceso y se guarda en
 * memoria con protección de [Mutex]; así las pantallas y el agente comparten el mismo catálogo sin volver a leer.
 */
class AssetsRecipeCatalog @Inject constructor(
    private val source: RecipeAssetSource,
    private val json: Json,
) : RecipeCatalog {

    private val cacheMutex = Mutex()
    private var recipesCache: List<Recipe>? = null
    private var ingredientsCache: Map<String, Ingredient>? = null

    override suspend fun recipes(): List<Recipe> =
        recipesCache ?: cacheMutex.withLock {
            recipesCache ?: loadRecipes().also { recipesCache = it }
        }

    override suspend fun ingredients(): Map<String, Ingredient> =
        ingredientsCache ?: cacheMutex.withLock {
            ingredientsCache ?: loadIngredients().also { ingredientsCache = it }
        }

    private fun loadRecipes(): List<Recipe> =
        json.decodeFromString<List<RecetaDto>>(source.read(RECIPES_ASSET)).map { it.toDomain() }

    private fun loadIngredients(): Map<String, Ingredient> =
        json.decodeFromString<List<IngredienteDto>>(source.read(INGREDIENTS_ASSET))
            .associate { dto -> dto.id to dto.toDomain() }

    private companion object {
        const val RECIPES_ASSET = "recetas.json"
        const val INGREDIENTS_ASSET = "ingredientes.json"
    }
}
