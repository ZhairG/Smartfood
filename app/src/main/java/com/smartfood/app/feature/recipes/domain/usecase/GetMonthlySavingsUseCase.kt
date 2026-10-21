package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Nombre: GetMonthlySavingsUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Caso de uso que calcula el valor rescatado acumulado del mes actual en pesos COP (spec 009, CA-RIA-04-06, T022).
 *
 * @param cookingHistoryRepository Historial de recetas cocinadas.
 * @param clock Reloj para determinar el mes y año actual.
 */
class GetMonthlySavingsUseCase @Inject constructor(
    private val cookingHistoryRepository: CookingHistoryRepository,
    private val clock: Clock,
) {
    /**
     * @return Flujo reactivo con el total en pesos COP rescatados en el mes actual.
     */
    operator fun invoke(): Flow<Int> {
        val today = LocalDate.now(clock)
        val currentYearMonth = YearMonth.from(today)
        val zone = clock.zone

        return cookingHistoryRepository.observeHistory().map { records ->
            records.filter { record ->
                val recordDate = Instant.ofEpochMilli(record.cookedAt).atZone(zone).toLocalDate()
                YearMonth.from(recordDate) == currentYearMonth
            }.sumOf { it.rescuedValueCop }
        }
    }
}
