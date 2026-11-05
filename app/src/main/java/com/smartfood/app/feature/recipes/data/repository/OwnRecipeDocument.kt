package com.smartfood.app.feature.recipes.data.repository

import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient

/**
 * Nombre: OwnRecipeDocument.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Conversión comprobable entre receta propia y Map de Firestore (spec 010, plan §2.1, T009).
 */
internal object OwnRecipeDocument {
    const val NAME = "name"
    const val NAME_NORMALIZED = "nameNormalized"
    const val CATEGORY = "category"
    const val INGREDIENTS = "ingredients"
    const val COOK_MINUTES = "cookMinutes"
    const val PREPARATION = "preparation"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}

internal fun OwnRecipe.toDocumentFields(): Map<String, Any> = mapOf(
    OwnRecipeDocument.NAME to name,
    OwnRecipeDocument.NAME_NORMALIZED to normalize(name),
    OwnRecipeDocument.CATEGORY to category.name,
    OwnRecipeDocument.INGREDIENTS to ingredients.map {
        mapOf("ingredientId" to it.ingredientId, "quantity" to it.quantity)
    },
    OwnRecipeDocument.COOK_MINUTES to cookMinutes,
    OwnRecipeDocument.PREPARATION to preparation,
)

internal fun ownRecipeFromDocument(id: String, data: Map<String, Any?>, pending: Boolean = false): OwnRecipe? {
    val name = (data[OwnRecipeDocument.NAME] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val category = (data[OwnRecipeDocument.CATEGORY] as? String)?.let { value ->
        enumValues<RecipeCategory>().firstOrNull { it.name == value }
    } ?: return null
    val ingredients = (data[OwnRecipeDocument.INGREDIENTS] as? List<*>)?.map { raw ->
        val map = raw as? Map<*, *> ?: return null
        val ingredientId = map["ingredientId"] as? String ?: return null
        val quantity = (map["quantity"] as? Number)?.toDouble()?.takeIf { it > 0 } ?: return null
        RecipeIngredient(ingredientId, quantity)
    } ?: return null
    val cookMinutes = (data[OwnRecipeDocument.COOK_MINUTES] as? Number)?.toInt() ?: return null
    val preparation = data[OwnRecipeDocument.PREPARATION] as? String ?: return null
    val createdAt = timestampMillis(data[OwnRecipeDocument.CREATED_AT]) ?: return null
    val updatedAt = timestampMillis(data[OwnRecipeDocument.UPDATED_AT]) ?: return null
    return OwnRecipe(id, name, category, ingredients, cookMinutes, preparation, createdAt, updatedAt, pending)
}

private fun timestampMillis(value: Any?): Long? = when (value) {
    is com.google.firebase.Timestamp -> value.toDate().time
    is java.util.Date -> value.time
    else -> null
}

private fun normalize(value: String): String = java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFD)
    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    .lowercase()
