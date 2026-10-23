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
 * @property costCop Costo de los empaques que hay que comprar de este ingrediente (spec §2).
 * @property pantryBasic `true` si es un básico de despensa que no se compra ("Básico de despensa").
 * @property packs Empaques que hay que comprar.
 * @property packLabel Presentación de mercado ("1 kg", "30 und").
 * @property store Tienda del precio de esos empaques, o `null` si no se compra nada (CA-RIA-04-07).
 */
data class RecipeIngredientLine(
    val ingredientId: String,
    val name: String,
    val section: String,
    val quantity: Double,
    val unit: IngredientUnit,
    val inInventory: Boolean,
    val costCop: Long,
    val pantryBasic: Boolean = false,
    val packs: Int = 0,
    val packLabel: String = "",
    val store: Store? = null,
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
