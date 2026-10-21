package com.smartfood.app.feature.recipes.domain.repository

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
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

    /**
     * Guarda un registro de cocción en Firestore (`users/{uid}/cookings/{id}`). Como el inventario (spec 006 §13), espera
     * al servidor un tiempo corto: sin conexión devuelve [WriteOutcome.PENDING] y la escritura sigue en la cola.
     */
    suspend fun saveRecord(record: CookingRecord): Result<WriteOutcome>
}
