package com.smartfood.app.feature.profile.data.repository

import com.smartfood.app.feature.profile.domain.model.Cuisine
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.MealBudget
import com.smartfood.app.feature.profile.domain.model.SpiceLevel

/**
 * Nombre: PreferencesDocument.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Conversión entre [FoodPreferences] y el mapa `preferences` de `users/{uid}` (spec 008, plan §2.1,
 * CA-PER-03-04). Se usa `Map` para no depender de reflexión con R8. Las funciones son puras para probarlas sin
 * Firebase: los valores desconocidos o mal formados se ignoran en lugar de romper la pantalla.
 */
internal object PreferencesDocument {
    const val PREFERENCES = "preferences"
    const val RESTRICTIONS = "restrictions"
    const val CUISINES = "cuisines"
    const val SPICE = "spice"
    const val BUDGET = "budget"
}

/** Lee el mapa `preferences` del documento; sin mapa o con otro tipo, devuelve preferencias vacías. */
internal fun preferencesFromDocument(data: Map<String, Any?>): FoodPreferences {
    val preferences = data[PreferencesDocument.PREFERENCES] as? Map<*, *> ?: return FoodPreferences()
    return FoodPreferences(
        restrictions = preferences.stringList(PreferencesDocument.RESTRICTIONS)
            .mapNotNull { name -> DietaryRestriction.entries.firstOrNull { it.name == name } }.toSet(),
        cuisines = preferences.stringList(PreferencesDocument.CUISINES)
            .mapNotNull { name -> Cuisine.entries.firstOrNull { it.name == name } }.toSet(),
        spice = (preferences[PreferencesDocument.SPICE] as? String)
            ?.let { name -> SpiceLevel.entries.firstOrNull { it.name == name } },
        budget = (preferences[PreferencesDocument.BUDGET] as? String)
            ?.let { name -> MealBudget.entries.firstOrNull { it.name == name } },
    )
}

/**
 * Mapa que reemplaza el campo `preferences` en Firestore. Las claves `spice` y `budget` solo se escriben si hay
 * elección: Firestore no acepta `null`, y al reemplazar el mapa completo quitarlas también las borra (CA-PER-03-04).
 */
internal fun FoodPreferences.toDocumentFields(): Map<String, Any> = buildMap {
    // Se escribe en el orden de los enums para que el documento no cambie si el usuario elige en otro orden.
    put(
        PreferencesDocument.RESTRICTIONS,
        DietaryRestriction.entries.filter { it in restrictions }.map { it.name },
    )
    put(PreferencesDocument.CUISINES, Cuisine.entries.filter { it in cuisines }.map { it.name })
    spice?.let { put(PreferencesDocument.SPICE, it.name) }
    budget?.let { put(PreferencesDocument.BUDGET, it.name) }
}

private fun Map<*, *>.stringList(key: String): List<String> =
    (this[key] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
