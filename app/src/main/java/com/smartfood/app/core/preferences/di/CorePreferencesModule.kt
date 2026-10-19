package com.smartfood.app.core.preferences.di

import com.smartfood.app.core.preferences.DataStoreThemePreferences
import com.smartfood.app.core.preferences.ThemePreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Nombre: CorePreferencesModule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Enlaza las preferencias del teléfono que viven en `core/preferences` (spec 008, HU-PER-06, T018).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CorePreferencesModule {

    /** Tema elegido por el usuario, guardado en el DataStore `theme_prefs` (RF-PER-07). */
    @Binds
    @Singleton
    abstract fun bindThemePreferences(impl: DataStoreThemePreferences): ThemePreferences
}
