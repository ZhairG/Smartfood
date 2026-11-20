package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.data.catalog.RecipeAssetSource
import java.io.File

/**
 * Nombre: RealCatalog.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Catálogo real de `src/main/assets` para las pruebas del agente en la JVM (spec 009, T013, T037 y T044).
 * Los inventarios sintéticos y los perfiles de evaluación están en [EvaluationCatalog] (`sharedTest`), que también usa
 * la medición en el teléfono.
 */
internal object RealCatalog : EvaluationCatalog(AssetsDirectory)

/** Lee los assets desde la carpeta del módulo, como en la JVM de las pruebas unitarias. */
private object AssetsDirectory : RecipeAssetSource {

    private val dir: File by lazy {
        listOf(File("src/main/assets"), File("app/src/main/assets")).firstOrNull { it.isDirectory }
            ?: error("No se encontró app/src/main/assets desde ${File("").absolutePath}")
    }

    override fun read(name: String): String = dir.resolve(name).readText()
}
