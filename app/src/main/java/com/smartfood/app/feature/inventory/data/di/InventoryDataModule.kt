package com.smartfood.app.feature.inventory.data.di

import com.smartfood.app.feature.inventory.data.local.DataStoreInventoryPreferences
import com.smartfood.app.feature.inventory.data.reminder.WorkManagerExpiryReminderScheduler
import com.smartfood.app.feature.inventory.data.repository.FirebaseStoragePhotoRepository
import com.smartfood.app.feature.inventory.data.repository.FirestoreInventoryRepository
import com.smartfood.app.feature.inventory.data.speech.AndroidSpeechToText
import com.smartfood.app.feature.inventory.data.sync.WorkManagerPhotoSync
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import com.smartfood.app.feature.inventory.domain.repository.InventoryPreferences
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import com.smartfood.app.feature.inventory.domain.repository.SpeechToText
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Nombre: InventoryDataModule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Enlaza los contratos del dominio del inventario con sus implementaciones de la capa de datos. Cada
 * Issue enlaza aquí la implementación que crea (spec 006, `tasks.md`): Firestore y fotos (#69), voz (#32),
 * preferencias (#33) y avisos (#44). El reloj del teléfono, que solo usa el dominio del inventario, lo provee este
 * mismo módulo (T031, #39).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InventoryDataModule {

    /** Productos del usuario en Firestore (RF-INV-01, T017). */
    @Binds
    @Singleton
    abstract fun bindInventoryRepository(impl: FirestoreInventoryRepository): InventoryRepository

    /** Fotos de productos en Cloud Storage (RNF-INV-02, T018). */
    @Binds
    @Singleton
    abstract fun bindPhotoRepository(impl: FirebaseStoragePhotoRepository): PhotoRepository

    /** Subida y borrado de fotos en segundo plano con WorkManager, también sin conexión (§13, T048). */
    @Binds
    @Singleton
    abstract fun bindPhotoSync(impl: WorkManagerPhotoSync): PhotoSync

    /** Reconocimiento de voz del sistema en español de Colombia (RF-INV-06, T019). */
    @Binds
    @Singleton
    abstract fun bindSpeechToText(impl: AndroidSpeechToText): SpeechToText

    /** Preferencias locales del inventario sobre el DataStore existente (CA-INV-08-05, T020). */
    @Binds
    @Singleton
    abstract fun bindInventoryPreferences(impl: DataStoreInventoryPreferences): InventoryPreferences

    /** Aviso diario de vencimiento sobre WorkManager (CA-INV-08-01, T038). */
    @Binds
    @Singleton
    abstract fun bindExpiryReminderScheduler(
        impl: WorkManagerExpiryReminderScheduler
    ): ExpiryReminderScheduler

    companion object {

        /**
         * Reloj del teléfono para los casos de uso que dependen de "hoy" (spec 006, plan §3): clasificar el
         * vencimiento (`ClassifyExpiryUseCase`) y validar la fecha de un producto nuevo (`ValidateItemUseCase`).
         * Vive aquí porque solo lo usa el dominio del inventario; las pruebas inyectan un reloj fijo.
         */
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
