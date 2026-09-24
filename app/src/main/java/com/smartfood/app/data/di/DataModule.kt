package com.smartfood.app.data.di

import com.google.firebase.auth.FirebaseAuth
import com.smartfood.app.data.repository.AuthRepository
import com.smartfood.app.data.repository.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Nombre: DataModule.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Módulo de inyección de dependencias con Hilt para proveer repositorios y fuentes de datos.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /**
     * Enlaza la implementación [AuthRepositoryImpl] con la interfaz [AuthRepository].
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    companion object {
        /**
         * Proveedor de la instancia única de [FirebaseAuth].
         */
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
