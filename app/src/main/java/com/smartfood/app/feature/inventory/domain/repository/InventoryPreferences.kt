package com.smartfood.app.feature.inventory.domain.repository

/**
 * Nombre: InventoryPreferences.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de las preferencias locales del inventario, como saber si ya se pidió el permiso de
 * notificaciones (CA-INV-08-05).
 */
interface InventoryPreferences {

    /** Indica si ya se pidió el permiso de notificaciones. Por defecto es `false`. */
    suspend fun wasNotificationPermissionRequested(): Boolean

    /** Registra que ya se pidió el permiso, para no volver a pedirlo automáticamente. */
    suspend fun markNotificationPermissionRequested()
}
