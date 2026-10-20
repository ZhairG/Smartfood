package com.smartfood.app.feature.recipes.domain.model

/**
 * Nombre: RecipeSuggestion.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Resultado de ordenar una receta según el inventario actual (spec 009, CA-RIA-01-02, T007). Lo produce
 * `RankRecipesForMeUseCase` y lo dibuja la tarjeta de Descubrir: costo de lo que falta y el ingrediente por vencer que
 * la receta rescata.
 */

/** Filtros de la pestaña Recetas (spec 009, CA-RIA-01-01 a 01-03). */
enum class RecipeFilter { FOR_YOU, UNDER_30, ECONOMIC }

/**
 * Receta con su evaluación para el inventario actual.
 *
 * @property recipe Receta del catálogo.
 * @property costCop Costo de lo que falta comprar para las porciones base, en pesos.
 * @property utility Utilidad de cocinarla con el inventario actual (spec §2).
 * @property expiringIngredientName Nombre del ingrediente que usa y está próximo a vencer, o `null` si no rescata nada.
 */
data class RecipeSuggestion(
    val recipe: Recipe,
    val costCop: Long,
    val utility: Double,
    val expiringIngredientName: String? = null,
)
