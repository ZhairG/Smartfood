package com.smartfood.app.feature.inventory.domain.repository

/**
 * Nombre: ExpiryReminderScheduler.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para programar y cancelar el aviso diario de productos por vencer (RF-INV-07).
 */
interface ExpiryReminderScheduler {

    /** Programa el aviso diario de las 9:00 (CA-INV-08-01). Llamarlo de nuevo no duplica el aviso. */
    fun schedule()

    /** Cancela el aviso y descarta la notificación visible, por ejemplo al cerrar sesión (CA-INV-08-06). */
    fun cancel()
}
