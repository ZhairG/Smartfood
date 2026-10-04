package com.smartfood.app.feature.auth.data.di

import com.smartfood.app.feature.auth.data.demo.DemoPasswordRecoveryRepository
import com.smartfood.app.feature.auth.data.local.DataStoreSessionPreferences
import com.smartfood.app.feature.auth.data.repository.FirebaseAuthRepository
import com.smartfood.app.feature.auth.data.repository.FirestoreUserRepository
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.auth.domain.repository.PasswordRecoveryRepository
import com.smartfood.app.feature.auth.domain.repository.SessionPreferences
import com.smartfood.app.feature.auth.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Nombre: AuthDataModule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Enlaza los contratos del dominio de autenticación con sus implementaciones de la capa de datos.
 * Para reemplazar la recuperación simulada por una real, solo cambia [bindPasswordRecoveryRepository].
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

    /** Recuperación **simulada** (ADR-01). */
    @Binds
    @Singleton
    abstract fun bindPasswordRecoveryRepository(impl: DemoPasswordRecoveryRepository): PasswordRecoveryRepository
}
