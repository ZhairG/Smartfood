package com.smartfood.app.feature.auth.data.di

import com.smartfood.app.BuildConfig
import com.smartfood.app.feature.auth.data.demo.DemoPasswordRecoveryRepository
import com.smartfood.app.feature.auth.data.local.DataStoreSessionPreferences
import com.smartfood.app.feature.auth.data.repository.FirebaseAuthRepository
import com.smartfood.app.feature.auth.data.repository.FirestoreUserRepository
import com.smartfood.app.feature.auth.data.repository.RemotePasswordRecoveryRepository
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.auth.domain.repository.PasswordRecoveryRepository
import com.smartfood.app.feature.auth.domain.repository.SessionPreferences
import com.smartfood.app.feature.auth.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * <build>
 * <b>Nombre:</b> AuthDataModule.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Enlaza los contratos del dominio de autenticación con sus implementaciones de la capa de<br>
 * datos. La recuperación es la real (spec 003); con `-PrecoveryDemo=true` el build debug usa la simulada<br>
 * (RF-REC-06).<br>
 * </build>
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirestoreUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindSessionPreferences(impl: DataStoreSessionPreferences): SessionPreferences

    companion object {

        /**
         * Recuperación real con Cloud Functions o, en modo demo, la simulada con datos fijos.
         * Los [Provider] evitan construir la implementación que no se usa.
         */
        @Provides
        @Singleton
        fun providePasswordRecoveryRepository(
            remote: Provider<RemotePasswordRecoveryRepository>,
            demo: Provider<DemoPasswordRecoveryRepository>
        ): PasswordRecoveryRepository = if (BuildConfig.USE_DEMO_RECOVERY) demo.get() else remote.get()
    }
}
