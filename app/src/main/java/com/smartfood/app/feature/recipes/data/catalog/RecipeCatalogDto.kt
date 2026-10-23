package com.smartfood.app.feature.recipes.data.catalog

import com.smartfood.app.feature.recipes.domain.model.FoodTag
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import com.smartfood.app.feature.recipes.domain.model.Store
import kotlinx.serialization.Serializable

/**
 * Nombre: RecipeCatalogDto.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: DTOs y mapeo de los JSON del catálogo (spec 009, plan §2 y §4.1, T004). Los nombres de las propiedades
 * son los del archivo (`nombre`, `seccion`, `etiquetas`…) y se traducen a los modelos de dominio de T002. Un valor
 * desconocido de `unidad`, `etiquetas` o de una tienda en `precios` lanza [IllegalArgumentException] para que la prueba de
 * contenido lo detecte.
 */

/** Ingrediente tal como viene en `ingredientes.json`. */
@Serializable
internal data class IngredienteDto(
    val id: String,
    val nombre: String,
    val sinonimos: List<String> = emptyList(),
    val seccion: String,
    val unidad: String,
    val vidaUtilDias: Int? = null,
    val etiquetas: List<String> = emptyList(),
    val empaque: EmpaqueDto,
    val precios: Map<String, Long>,
    val basico: Boolean = false,
)

/** Presentación de mercado de un ingrediente: cantidad en la unidad base y cómo se muestra ("1 kg"). */
@Serializable
internal data class EmpaqueDto(
    val cantidad: Double,
    val etiqueta: String,
)

/** Ingrediente de una receta tal como viene en `recetas.json`. */
@Serializable
internal data class RecetaIngredienteDto(
    val id: String,
    val cantidad: Double,
)

/** Paso de una receta tal como viene en `recetas.json`. */
@Serializable
internal data class PasoDto(
    val titulo: String,
    val texto: String,
    val minutos: Int? = null,
)

/** Receta tal como viene en `recetas.json`. */
@Serializable
internal data class RecetaDto(
    val id: String,
    val nombre: String,
    val foto: String? = null,
    val porciones: Int,
    val prepMin: Int,
    val coccionMin: Int,
    val reposoMin: Int = 0,
    val dificultad: String,
    val picante: Boolean = false,
    val kcal: Int,
    val ingredientes: List<RecetaIngredienteDto>,
    val antesDeEmpezar: List<String> = emptyList(),
    val pasos: List<PasoDto>,
)

/** Convierte el DTO de un ingrediente al modelo del dominio (plan §4.1). */
internal fun IngredienteDto.toDomain(): Ingredient = Ingredient(
    id = id,
    name = nombre,
    synonyms = sinonimos,
    section = seccion,
    unit = unidadAUnidad(unidad, id),
    pack = empaque.cantidad,
    packLabel = empaque.etiqueta,
    prices = precios.entries.associate { (tienda, precio) -> tiendaAStore(tienda, id) to precio },
    pantryBasic = basico,
    shelfLifeDays = vidaUtilDias,
    tags = etiquetas.map { etiquetaAFoodTag(it, id) }.toSet(),
)

/** Convierte el DTO de una receta al modelo del dominio (plan §4.1). */
internal fun RecetaDto.toDomain(): Recipe = Recipe(
    id = id,
    name = nombre,
    photo = foto,
    servings = porciones,
    prepMinutes = prepMin,
    cookMinutes = coccionMin,
    restMinutes = reposoMin,
    difficulty = dificultad,
    spicy = picante,
    kcal = kcal,
    ingredients = ingredientes.map { RecipeIngredient(it.id, it.cantidad) },
    beforeStarting = antesDeEmpezar,
    steps = pasos.map { RecipeStep(it.titulo, it.texto, it.minutos) },
)

/** Traduce `g`/`ml`/`und` del JSON a la unidad base del dominio. */
internal fun unidadAUnidad(unidad: String, ingredientId: String): IngredientUnit = when (unidad) {
    "g" -> IngredientUnit.GRAM
    "ml" -> IngredientUnit.MILLILITER
    "und" -> IngredientUnit.UNIT
    else -> throw IllegalArgumentException("Unidad desconocida '$unidad' en el ingrediente '$ingredientId'")
}

/** Traduce las claves de `precios` del JSON a [Store] (plan §2). */
internal fun tiendaAStore(tienda: String, ingredientId: String): Store = when (tienda) {
    "exito" -> Store.EXITO
    "carulla" -> Store.CARULLA
    "olimpica" -> Store.OLIMPICA
    "d1" -> Store.D1
    else -> throw IllegalArgumentException("Tienda desconocida '$tienda' en el ingrediente '$ingredientId'")
}

/** Traduce las `etiquetas` del JSON a [FoodTag] (plan §2 y §4.1). */
internal fun etiquetaAFoodTag(etiqueta: String, ingredientId: String): FoodTag = when (etiqueta) {
    "carne" -> FoodTag.MEAT
    "marisco" -> FoodTag.SEAFOOD
    "origen_animal" -> FoodTag.ANIMAL_PRODUCT
    "gluten" -> FoodTag.GLUTEN
    "lactosa" -> FoodTag.LACTOSE
    "frutos_secos" -> FoodTag.NUTS
    else -> throw IllegalArgumentException("Etiqueta desconocida '$etiqueta' en el ingrediente '$ingredientId'")
}
