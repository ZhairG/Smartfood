package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import com.smartfood.app.testutil.fixedClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Nombre: GetMonthlySavingsUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [GetMonthlySavingsUseCase] (spec 009, CA-RIA-04-06, T022).
 */
class GetMonthlySavingsUseCaseTest {

    private val today = LocalDate.of(2026, 10, 20)
    private val clock = fixedClock(today)
    private val zone = ZoneId.systemDefault()

    private class FakeHistoryRepo : CookingHistoryRepository {
        var list = emptyList<CookingRecord>()
        override fun observeHistory(): Flow<List<CookingRecord>> = flowOf(list)
        override fun newRecordId(): String = "id"
        override suspend fun saveRecord(record: CookingRecord): Result<WriteOutcome> =
            Result.success(WriteOutcome.SYNCED)
    }

    private lateinit var repo: FakeHistoryRepo
    private lateinit var useCase: GetMonthlySavingsUseCase

    @Before
    fun setUp() {
        repo = FakeHistoryRepo()
        useCase = GetMonthlySavingsUseCase(repo, clock)
    }

    @Test
    fun `suma solo las cocinas del mes actual`() = runTest {
        val currentMonthMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val lastMonthMillis = today.minusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()

        repo.list = listOf(
            CookingRecord("1", "recipe1", 2, currentMonthMillis, 1200),
            CookingRecord("2", "recipe2", 2, currentMonthMillis, 1800),
            CookingRecord("3", "recipe3", 2, lastMonthMillis, 5000)
        )

        val total = useCase().first()
        assertEquals(3000, total)
    }
}
