package com.smartfood.app.feature.recipes.data.catalog

/**
 * Nombre: RecipeAssetSource.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Fuente de los JSON del catálogo de recetas (spec 009, plan §4.1, T004). La implementación real lee los
 * assets de Android; las pruebas usan un fake en memoria. Permite probar el catálogo sin Android.
 */
interface RecipeAssetSource {

    /** Devuelve el contenido completo del asset [name] (`recetas.json` o `ingredientes.json`). */
    fun read(name: String): String
}
