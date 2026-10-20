package com.smartfood.app.feature.recipes.data.di

import com.smartfood.app.feature.recipes.data.catalog.AndroidRecipeAssetSource
import com.smartfood.app.feature.recipes.data.catalog.AssetsRecipeCatalog
import com.smartfood.app.feature.recipes.data.catalog.RecipeAssetSource
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Nombre: RecipesDataModule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Enlaza el contrato del catálogo de recetas con su implementación que lee los assets, y provee el
 * [Json] de kotlinx.serialization (spec 009, plan §4.1, T004). Es el único módulo que crea el catálogo; el agente y las
 * pantallas solo conocen `RecipeCatalog`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RecipesDataModule {

    /** Catálogo de recetas leído de `assets/`, compartido por toda la app (RF-RIA-01). */
    @Binds
    @Singleton
    abstract fun bindRecipeCatalog(impl: AssetsRecipeCatalog): RecipeCatalog

    /** Fuente de los JSON del catálogo: los assets del APK (T004). */
    @Binds
    @Singleton
    abstract fun bindRecipeAssetSource(impl: AndroidRecipeAssetSource): RecipeAssetSource

    companion object {

        /** Lector de JSON tolerante a campos nuevos del catálogo (`ignoreUnknownKeys`). */
        @Provides
        @Singleton
        fun provideJson(): Json = Json { ignoreUnknownKeys = true }
    }
}
