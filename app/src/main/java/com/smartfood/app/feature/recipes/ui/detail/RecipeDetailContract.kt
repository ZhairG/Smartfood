package com.smartfood.app.feature.recipes.ui.detail

import com.smartfood.app.feature.recipes.domain.model.RecipeIngredientGroup
import com.smartfood.app.feature.recipes.domain.model.Store

/**
 * Nombre: RecipeDetailContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Estado y eventos del detalle de receta (spec 009, HU-RIA-04, T008). El ViewModel calcula las cantidades
 * y costos de las porciones elegidas; la pantalla solo dibuja y avisa.
 */

/**
 * Estado del detalle.
 *
 * @property isLoading `true` mientras se lee el catálogo.
 * @property hasError `true` si la receta no se pudo cargar.
 * @property recipeId Receta abierta.
 * @property name Nombre de la receta (título de la barra).
 * @property photo Foto en `drawable-nodpi`.
 * @property prepMinutes Preparación.
 * @property cookMinutes Cocción.
 * @property restMinutes Reposo.
 * @property difficulty "Fácil", "Media" o "Alta".
 * @property servings Porciones elegidas.
 * @property groups Ingredientes agrupados por sección, ya escalados.
 * @property store Supermercado de los precios ("Mi supermercado", CA-RIA-04-07).
 */
data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val recipeId: String = "",
    val name: String = "",
    val photo: String? = null,
    val prepMinutes: Int = 0,
    val cookMinutes: Int = 0,
    val restMinutes: Int = 0,
    val difficulty: String = "",
    val servings: Int = 1,
    val groups: List<RecipeIngredientGroup> = emptyList(),
    val store: Store = Store.D1,
)

/** Acciones de la persona en el detalle. */
sealed interface RecipeDetailEvent {

    /** Quitó una porción (mínimo 1). */
    data object DecreaseServings : RecipeDetailEvent

    /** Agregó una porción (máximo 12). */
    data object IncreaseServings : RecipeDetailEvent
}
