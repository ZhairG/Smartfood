package com.smartfood.app.feature.recipes.domain.model

/**
 * Nombre: RecipeDetail.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Datos del detalle de una receta (spec 009, CA-RIA-04-01 y 04-02, T008): la receta, el catálogo de
 * ingredientes y las líneas ya escaladas a las porciones elegidas, agrupadas por sección y con el costo de lo que falta.
 */

/** Receta con el catálogo de ingredientes que necesita el detalle. */
data class RecipeDetail(
    val recipe: Recipe,
    val ingredients: Map<String, Ingredient>,
)

/**
 * Ingrediente del detalle para las porciones elegidas.
 *
 * @property ingredientId Identificador del catálogo.
 * @property name Nombre para mostrar.
 * @property section Sección del Figma ("Verduras", "Cereales", "Carnes"...).
 * @property quantity Cantidad escalada, en la unidad base del ingrediente.
 * @property unit Unidad base.
 * @property inInventory `true` si el inventario cubre toda la cantidad ("En tu inventario").
 * @property costCop Costo de lo que falta comprar de este ingrediente.
 */
data class RecipeIngredientLine(
    val ingredientId: String,
    val name: String,
    val section: String,
    val quantity: Double,
    val unit: IngredientUnit,
    val inInventory: Boolean,
    val costCop: Long,
)

/** Ingredientes de una sección (Verduras, Cereales, Carnes...). */
data class RecipeIngredientGroup(
    val section: String,
    val lines: List<RecipeIngredientLine>,
)

/**
 * Ingredientes del detalle para unas porciones.
 *
 * @property groups Secciones en el orden del Figma.
 * @property missingCostCop Costo total de lo que falta comprar.
 */
data class RecipeIngredients(
    val groups: List<RecipeIngredientGroup>,
    val missingCostCop: Long,
)
