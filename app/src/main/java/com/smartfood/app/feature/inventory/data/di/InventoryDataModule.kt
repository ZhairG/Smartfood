package com.smartfood.app.feature.inventory.data.di

import com.smartfood.app.feature.inventory.data.reminder.NoOpExpiryReminderScheduler
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Nombre: InventoryDataModule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Enlaza los contratos del dominio del inventario con sus implementaciones de la capa de datos.
 * Por ahora solo enlaza el aviso de vencimiento a una implementación vacía (T013); T021 agrega los repositorios y
 * T038 cambia el aviso por el de WorkManager.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InventoryDataModule {

    /** Temporal hasta T038: sin avisos reales, cerrar sesión no tiene nada que cancelar. */
    @Binds
    @Singleton
    abstract fun bindExpiryReminderScheduler(impl: NoOpExpiryReminderScheduler): ExpiryReminderScheduler
}
