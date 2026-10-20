package com.smartfood.app.feature.profile.domain.model

/**
 * Nombre: FoodPreferences.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Preferencias alimentarias del usuario (spec 008, HU-PER-03, plan §2.2): restricciones y cocinas que
 * acepta, nivel de picante y presupuesto por comida. Se guardan en el mapa `preferences` de `users/{uid}` y las usará
 * el agente de recetas (spec 009).
 */

/** Restricciones alimentarias que puede elegir el usuario; se pueden combinar entre sí (CA-PER-03-02). */
enum class DietaryRestriction { VEGETARIAN, VEGAN, GLUTEN_FREE, LACTOSE_FREE, NUT_FREE }

/** Cocinas favoritas; se pueden elegir varias (CA-PER-03-02). */
enum class Cuisine { COLOMBIAN, ITALIAN, ASIAN, MEXICAN, MEDITERRANEAN }

/** Nivel de picante que tolera el usuario; se elige como máximo uno (CA-PER-03-02). */
enum class SpiceLevel { MILD, MEDIUM, HOT }

/** Presupuesto por comida; se elige como máximo uno (CA-PER-03-02). */
enum class MealBudget { ECONOMIC, MODERATE, UNLIMITED }

/**
 * Preferencias alimentarias guardadas.
 *
 * @property restrictions Restricciones aceptadas; vacío si no tiene ninguna.
 * @property cuisines Cocinas favoritas; vacío si no tiene ninguna.
 * @property spice Picante máximo tolerado, o `null` si no eligió ninguno.
 * @property budget Presupuesto por comida, o `null` si no eligió ninguno.
 */
data class FoodPreferences(
    val restrictions: Set<DietaryRestriction> = emptySet(),
    val cuisines: Set<Cuisine> = emptySet(),
    val spice: SpiceLevel? = null,
    val budget: MealBudget? = null,
)
