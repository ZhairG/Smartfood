package com.smartfood.app.feature.recipes.data.catalog

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Nombre: AndroidRecipeAssetSource.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Lee los JSON del catálogo desde la carpeta `assets/` de la app (spec 009, plan §4.1, T004). Los
 * archivos se empaquetan en el APK y funcionan sin conexión.
 */
class AndroidRecipeAssetSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecipeAssetSource {

    override fun read(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }
}
