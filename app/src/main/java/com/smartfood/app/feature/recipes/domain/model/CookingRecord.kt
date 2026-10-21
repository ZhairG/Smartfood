package com.smartfood.app.feature.recipes.domain.model

/**
 * Nombre: CookingRecord.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Registro histórico de una receta cocinada por el usuario (spec 009, CA-RIA-04-05 y 04-06).
 *
 * @property id Identificador único del registro en Firestore (`users/{uid}/cookings/{id}`).
 * @property recipeId Identificador de la receta preparada.
 * @property servings Porciones preparadas.
 * @property cookedAt Timestamp en milisegundos de la fecha de preparación.
 * @property rescuedValueCop Valor monetario en pesos COP de los alimentos próximos a vencer rescatados al cocinar (CA-RIA-04-06).
 */
data class CookingRecord(
    val id: String,
    val recipeId: String,
    val servings: Int,
    val cookedAt: Long = System.currentTimeMillis(),
    val rescuedValueCop: Int = 0,
)
