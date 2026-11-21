package com.smartfood.app.feature.auth.data.di

import com.smartfood.app.BuildConfig
import com.smartfood.app.feature.auth.data.demo.DemoPasswordRecoveryRepository
import com.smartfood.app.feature.auth.data.local.DataStoreSessionPreferences
import com.smartfood.app.feature.auth.data.repository.FirebaseAuthRepository
import com.smartfood.app.feature.auth.data.repository.FirestoreUserRepository
import com.smartfood.app.feature.auth.data.repository.ModeAwarePasswordRecoveryRepository
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
 * Nombre: AuthDataModule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Enlaza los contratos del dominio de autenticación con sus implementaciones de la capa de datos.
 * La recuperación es la real (spec 003, o con enlace en el modo gratuito de la spec 012); con `-PrecoveryDemo=true`
 * el build debug usa la simulada (RF-REC-06), que manda sobre el modo (CA-SPK-02-05).
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
         * Recuperación real según el interruptor del backend (código o enlace, spec 012) o, en modo demo, la simulada
         * con datos fijos. Los [Provider] evitan construir la implementación que no se usa.
         */
        @Provides
        @Singleton
        fun providePasswordRecoveryRepository(
            real: Provider<ModeAwarePasswordRecoveryRepository>,
            demo: Provider<DemoPasswordRecoveryRepository>
        ): PasswordRecoveryRepository = if (BuildConfig.USE_DEMO_RECOVERY) demo.get() else real.get()
    }
}
