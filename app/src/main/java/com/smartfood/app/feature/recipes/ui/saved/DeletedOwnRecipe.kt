package com.smartfood.app.feature.recipes.ui.saved

/**
 * Nombre: DeletedOwnRecipe.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Datos de una receta borrada para deshacer (spec 010, T017).
 *
 * @property recipeId Id de la receta borrada.
 * @property name Nombre de la receta borrada.
 * @property wasFavorite `true` si también estaba marcada como favorita.
 * @property favoriteSavedAtMillis Hora en que se guardó como favorita, o 0 si no lo era.
 */
data class DeletedOwnRecipe(
    val recipeId: String,
    val name: String,
    val wasFavorite: Boolean = false,
    val favoriteSavedAtMillis: Long = 0,
)
