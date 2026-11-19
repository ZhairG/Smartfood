package com.smartfood.app.feature.recipes.domain.repository

import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: CookingHistoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para leer el historial de recetas cocinadas (spec 009, T021). El registro se escribe con
 * [CookingRepository.finishCooking], en el mismo lote que el descuento del inventario (plan §6, CA-RIA-04-09).
 */
interface CookingHistoryRepository {

    /** Observa la lista de registros de cocción del usuario con sesión. */
    fun observeHistory(): Flow<List<CookingRecord>>
}
