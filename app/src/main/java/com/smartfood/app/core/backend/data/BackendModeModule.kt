package com.smartfood.app.core.backend.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.smartfood.app.core.backend.BackendModeProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Nombre: BackendModeModule.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Módulo Hilt del interruptor del backend (spec 012, RF-SPK-01): provee Remote Config y enlaza
 * [BackendModeProvider] con [RemoteConfigBackendModeProvider].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackendModeModule {

    @Binds
    @Singleton
    abstract fun bindBackendModeProvider(impl: RemoteConfigBackendModeProvider): BackendModeProvider

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    }
}
