package com.smartfood.app.data.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.smartfood.app.data.repository.AuthRepository
import com.smartfood.app.data.repository.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Nombre: DataModule.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Módulo de inyección de dependencias con Hilt para proveer repositorios, cliente HTTP y fuentes de datos.
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

        /**
         * Proveedor de la instancia única de [FirebaseFunctions].
         */
        @Provides
        @Singleton
        fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()

        /**
         * Proveedor de la instancia única de [OkHttpClient] para peticiones REST (Brevo).
         */
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
    }
}
