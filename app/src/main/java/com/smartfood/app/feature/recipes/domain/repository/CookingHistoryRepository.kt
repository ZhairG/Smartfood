package com.smartfood.app.feature.recipes.domain.repository

import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: CookingHistoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para la persistencia del historial de recetas cocinadas (spec 009, T021).
 */
interface CookingHistoryRepository {

    /** Observa la lista de registros de cocción del usuario con sesión. */
    fun observeHistory(): Flow<List<CookingRecord>>

    /** Genera un nuevo identificador único para el registro de cocción. */
    fun newRecordId(): String

    /** Guarda un registro de cocción en Firestore (`users/{uid}/cookings/{id}`). */
    suspend fun saveRecord(record: CookingRecord): Result<Unit>
}
