package com.smartfood.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
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
 * Firebase Authentication, Cloud Firestore, Cloud Functions, Cloud Storage (spec 006, T015) y el DataStore de
 * preferencias de sesión. Las instancias se inyectan siempre no nulas; las pruebas usan fakes de las interfaces del
 * dominio. Con `-PuseFirebaseEmulator=true` (solo debug), los SDK apuntan a la Emulator Suite (spec 003, plan §6).
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreDataModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().apply {
        connectEmulatorOnce("auth") { useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, AUTH_EMULATOR_PORT) }
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        connectEmulatorOnce("firestore") { useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, FIRESTORE_EMULATOR_PORT) }
    }

    /** Cloud Functions en la misma región que Firestore, donde se despliegan las funciones de la spec 003. */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance(FUNCTIONS_REGION).apply {
        connectEmulatorOnce("functions") { useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, FUNCTIONS_EMULATOR_PORT) }
    }

    /** Cloud Storage del bucket por defecto de `google-services.json`, para las fotos del inventario (spec 006). */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance().apply {
        connectEmulatorOnce("storage") { useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, STORAGE_EMULATOR_PORT) }
    }

    @Provides
    @Singleton
    fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.sessionDataStore
}

/** SDK ya conectados a la Emulator Suite en este proceso. */
private val connectedEmulators = mutableSetOf<String>()

/**
 * Conecta un SDK al emulador una sola vez por proceso y solo con `-PuseFirebaseEmulator=true`. Firebase lanza
 * `IllegalStateException` si `useEmulator` se llama dos veces, y Hilt vuelve a armar el grafo en cada prueba
 * (`@HiltAndroidTest`). En producción no hace nada.
 */
private inline fun connectEmulatorOnce(sdk: String, connect: () -> Unit) {
    if (!BuildConfig.USE_FIREBASE_EMULATOR) return
    val firstTime = synchronized(connectedEmulators) { connectedEmulators.add(sdk) }
    if (firstTime) connect()
}

private const val FUNCTIONS_REGION = "southamerica-east1"

// Puertos de la Emulator Suite, iguales a los de `firebase.json`.
private const val AUTH_EMULATOR_PORT = 9099
private const val FIRESTORE_EMULATOR_PORT = 8080
private const val FUNCTIONS_EMULATOR_PORT = 5001
private const val STORAGE_EMULATOR_PORT = 9199

/** DataStore único de preferencias de sesión (archivo `session_prefs`). */
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")
