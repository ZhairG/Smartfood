package com.smartfood.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.smartfood.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Nombre: CoreDataModule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Módulo Hilt que provee los SDK compartidos por todas las features:
 * Firebase Authentication, Cloud Firestore, Cloud Functions y el DataStore de preferencias de sesión.
 * Las instancias se inyectan siempre no nulas; las pruebas usan fakes de las interfaces del dominio.
 * Con `-PuseFirebaseEmulator=true` (solo debug), los SDK apuntan a la Emulator Suite (spec 003, plan §6).
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreDataModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().apply {
        if (BuildConfig.USE_FIREBASE_EMULATOR) useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, AUTH_EMULATOR_PORT)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        if (BuildConfig.USE_FIREBASE_EMULATOR) useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, FIRESTORE_EMULATOR_PORT)
    }

    /** Cloud Functions en la misma región que Firestore, donde se despliegan las funciones de la spec 003. */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance(FUNCTIONS_REGION).apply {
        if (BuildConfig.USE_FIREBASE_EMULATOR) useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, FUNCTIONS_EMULATOR_PORT)
    }

    @Provides
    @Singleton
    fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.sessionDataStore
}

private const val FUNCTIONS_REGION = "southamerica-east1"

// Puertos de la Emulator Suite, iguales a los de `firebase.json`.
private const val AUTH_EMULATOR_PORT = 9099
private const val FIRESTORE_EMULATOR_PORT = 8080
private const val FUNCTIONS_EMULATOR_PORT = 5001

/** DataStore único de preferencias de sesión (archivo `session_prefs`). */
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")
