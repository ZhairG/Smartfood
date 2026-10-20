package com.smartfood.app.feature.recipes.ui.discover

import com.smartfood.app.feature.recipes.domain.model.RecipeFilter

/**
 * Nombre: RecipesDiscoverContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Estado, eventos y efectos de la pantalla Descubrir (spec 009, HU-RIA-01, T007). Los campos derivados
 * (tarjetas, filtro activo, vacío) los calcula el ViewModel; la pantalla solo dibuja y avisa.
 */

/**
 * Estado de la pantalla Descubrir.
 *
 * @property isLoading `true` mientras se lee el catálogo de `assets/`.
 * @property hasError `true` si el catálogo no se pudo leer.
 * @property query Texto de la búsqueda.
 * @property filter Filtro activo.
 * @property recipes Tarjetas visibles, ya buscadas, filtradas y ordenadas.
 */
data class RecipesDiscoverUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val query: String = "",
    val filter: RecipeFilter = RecipeFilter.FOR_YOU,
    val recipes: List<RecipeCardUi> = emptyList(),
)

/**
 * Datos de una tarjeta de receta.
 *
 * @property id Identificador de la receta.
 * @property name Nombre para mostrar.
 * @property photo Nombre del recurso de la foto en `drawable-nodpi`.
 * @property minutes Tiempo total en minutos.
 * @property kcal Calorías por porción.
 * @property costCop Costo de lo que falta comprar, en pesos.
 * @property expiringIngredient Ingrediente por vencer que la receta usa, o `null`.
 */
data class RecipeCardUi(
    val id: String,
    val name: String,
    val photo: String?,
    val minutes: Int,
    val kcal: Int,
    val costCop: Long,
    val expiringIngredient: String? = null,
)

/** Acciones de la persona en Descubrir. */
sealed interface RecipesDiscoverEvent {

    /** Escribió en la búsqueda. */
    data class QueryChanged(val query: String) : RecipesDiscoverEvent

    /** Tocó la "x" de la búsqueda. */
    data object ClearQuery : RecipesDiscoverEvent

    /** Tocó un filtro. */
    data class FilterSelected(val filter: RecipeFilter) : RecipesDiscoverEvent

    /** Tocó una tarjeta para ver el detalle. */
    data class RecipeClicked(val recipeId: String) : RecipesDiscoverEvent
}

/** Efectos de un solo uso. */
sealed interface RecipesDiscoverEffect {

    /** Abre el detalle de la receta. */
    data class OpenRecipe(val recipeId: String) : RecipesDiscoverEffect
}
