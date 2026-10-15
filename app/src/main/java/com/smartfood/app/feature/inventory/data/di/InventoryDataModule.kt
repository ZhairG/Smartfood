package com.smartfood.app.feature.inventory.data.di

import com.smartfood.app.feature.inventory.data.reminder.NoOpExpiryReminderScheduler
import com.smartfood.app.feature.inventory.data.repository.FirebaseStoragePhotoRepository
import com.smartfood.app.feature.inventory.data.repository.FirestoreInventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Nombre: InventoryDataModule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Enlaza los contratos del dominio del inventario con sus implementaciones de la capa de datos. Cada
 * Issue enlaza aquí la implementación que crea (spec 006, `tasks.md`): Firestore y fotos (#69), voz (#32) y
 * preferencias (#33). El aviso de vencimiento usa una implementación vacía hasta T038 (WorkManager).
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

    /** Temporal hasta T038: sin avisos reales, cerrar sesión no tiene nada que cancelar. */
    @Binds
    @Singleton
    abstract fun bindExpiryReminderScheduler(impl: NoOpExpiryReminderScheduler): ExpiryReminderScheduler
}
