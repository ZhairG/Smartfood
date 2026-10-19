package com.smartfood.app.feature.recipes.domain.model

/**
 * Nombre: Recipe.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Modelos del catálogo de recetas (spec 009, plan §2, T002): los ingredientes con su precio de referencia
 * y las recetas con sus tiempos, ingredientes y pasos. El catálogo vive dentro de la app (`assets/`).
 */

/**
 * Lo que contiene un ingrediente y algunas dietas excluyen (spec 009, CA-RIA-02-09). En `ingredientes.json` va en
 * `"etiquetas"` con los valores `carne`, `marisco`, `origen_animal`, `gluten`, `lactosa` y `frutos_secos`. El huevo, la
 * leche, el queso y la miel llevan [ANIMAL_PRODUCT]; los lácteos, además, [LACTOSE].
 */
enum class FoodTag { MEAT, SEAFOOD, ANIMAL_PRODUCT, GLUTEN, LACTOSE, NUTS }

/** Unidad base de un ingrediente del catálogo. Todas las cantidades del catálogo y del agente van en esta unidad. */
enum class IngredientUnit { GRAM, MILLILITER, UNIT }

/**
 * Ingrediente del catálogo.
 *
 * @property id Identificador estable (por ejemplo, `pollo`).
 * @property name Nombre para mostrar.
 * @property synonyms Otros nombres con los que puede aparecer en el inventario (RF-RIA-02).
 * @property section Sección del detalle de receta ("Verduras", "Carnes"…).
 * @property unit Unidad base de las cantidades.
 * @property priceCop Precio de referencia en pesos de [priceQuantity] unidades base.
 * @property priceQuantity Cantidad, en la unidad base, que cuesta [priceCop] (por ejemplo, 1000 g).
 * @property shelfLifeDays Vida útil típica en días, para los productos frescos sin fecha; `null` si no aplica.
 * @property tags Lo que contiene y algunas dietas excluyen ([FoodTag]).
 */
data class Ingredient(
    val id: String,
    val name: String,
    val synonyms: List<String> = emptyList(),
    val section: String,
    val unit: IngredientUnit,
    val priceCop: Long,
    val priceQuantity: Double,
    val shelfLifeDays: Int? = null,
    val tags: Set<FoodTag> = emptySet(),
) {
    /** Precio de referencia de una unidad base, en pesos. */
    val pricePerUnitCop: Double get() = priceCop / priceQuantity
}

/**
 * Ingrediente de una receta.
 *
 * @property ingredientId Identificador del [Ingredient] del catálogo.
 * @property quantity Cantidad para las porciones base de la receta, en la unidad base del ingrediente.
 */
data class RecipeIngredient(val ingredientId: String, val quantity: Double)

/**
 * Paso de la cocina guiada.
 *
 * @property title Título corto ("Sofríe la cebolla y el tomate").
 * @property text Instrucción completa.
 * @property minutes Minutos del temporizador del paso, o `null` si no tiene.
 */
data class RecipeStep(val title: String, val text: String, val minutes: Int? = null)

/**
 * Receta del catálogo.
 *
 * @property id Identificador estable.
 * @property name Nombre para mostrar.
 * @property photo Nombre del recurso de la foto (`res/drawable-nodpi`), o `null` si no tiene.
 * @property servings Porciones para las que están escritas las cantidades.
 * @property prepMinutes Minutos de preparación.
 * @property cookMinutes Minutos de cocción.
 * @property restMinutes Minutos de reposo.
 * @property difficulty Dificultad para mostrar ("Fácil", "Media", "Difícil").
 * @property spicy `true` si la receta es picante.
 * @property kcal Calorías por porción.
 * @property ingredients Ingredientes con su cantidad para [servings] porciones.
 * @property beforeStarting Preparativos de "Antes de empezar".
 * @property steps Pasos de la cocina guiada.
 */
data class Recipe(
    val id: String,
    val name: String,
    val photo: String? = null,
    val servings: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val restMinutes: Int = 0,
    val difficulty: String,
    val spicy: Boolean = false,
    val kcal: Int,
    val ingredients: List<RecipeIngredient>,
    val beforeStarting: List<String> = emptyList(),
    val steps: List<RecipeStep>,
) {
    /** Tiempo total en minutos: preparación, cocción y reposo. */
    val totalMinutes: Int get() = prepMinutes + cookMinutes + restMinutes
}
