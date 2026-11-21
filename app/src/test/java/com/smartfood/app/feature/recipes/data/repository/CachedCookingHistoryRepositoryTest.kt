package com.smartfood.app.feature.recipes.data.repository

import com.smartfood.app.core.database.CookingEntity
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import com.smartfood.app.testutil.FakeCookingDao
import com.smartfood.app.testutil.FakeCurrentUid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: CachedCookingHistoryRepositoryTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas de [CachedCookingHistoryRepository] (spec 014, T003, HU-LOC-01): el historial sale del teléfono
 * aunque Firestore no responda (CA-LOC-01-01) o falle (CA-LOC-01-04), un snapshot reemplaza la copia (CA-LOC-01-02),
 * guardar escribe en los dos lados (CA-LOC-01-03) y otra cuenta no ve filas ajenas (CA-LOC-01-05).
 */
class CachedCookingHistoryRepositoryTest {

    /** Firestore de prueba: emite lo que la prueba le manda por [snapshots]. */
    private class FakeRemote : CookingHistoryRepository {
        val snapshots = MutableSharedFlow<List<CookingRecord>>()
        var history: Flow<List<CookingRecord>> = snapshots
        var result: Result<WriteOutcome> = Result.success(WriteOutcome.SYNCED)
        val saved = mutableListOf<CookingRecord>()

        override fun observeHistory(): Flow<List<CookingRecord>> = history
        override fun newRecordId(): String = "nuevo"
        override suspend fun saveRecord(record: CookingRecord): Result<WriteOutcome> {
            saved += record
            return result
        }
    }

    private val dao = FakeCookingDao()
    private val uid = FakeCurrentUid("ana")
    private val remote = FakeRemote()
    private val repository = CachedCookingHistoryRepository(remote, dao, uid)

    private fun record(id: String, cookedAt: Long = 1_000, value: Int = 5_000) =
        CookingRecord(id = id, recipeId = "arroz-pollo", servings = 4, cookedAt = cookedAt, rescuedValueCop = value)

    private fun entity(id: String, uid: String = "ana", cookedAt: Long = 1_000, value: Int = 5_000) =
        CookingEntity(id, uid, "arroz-pollo", 4, cookedAt, value)

    @Test
    fun `CA-LOC-01-01 sin respuesta de Firestore emite lo guardado en el telefono`() = runTest {
        dao.upsert(entity("a", value = 7_000))

        val history = repository.observeHistory().first()

        assertEquals(listOf(record("a", value = 7_000)), history)
    }

    @Test
    fun `CA-LOC-01-02 un snapshot de Firestore reemplaza la copia del telefono`() = runTest(UnconfinedTestDispatcher()) {
        dao.upsert(entity("viejo"))
        val seen = mutableListOf<List<CookingRecord>>()
        val job = launch { repository.observeHistory().collect { seen += it } }

        remote.snapshots.emit(listOf(record("n1", cookedAt = 1_000), record("n2", cookedAt = 2_000)))

        assertEquals(listOf("n2", "n1"), seen.last().map { it.id })
        assertEquals(setOf("n1", "n2"), dao.rows.value.map { it.id }.toSet())
        job.cancel()
    }

    @Test
    fun `CA-LOC-01-03 guardar escribe en Firestore y en el telefono con el mismo resultado`() = runTest {
        remote.result = Result.success(WriteOutcome.PENDING)

        val outcome = repository.saveRecord(record("r1", value = 3_000))

        assertEquals(WriteOutcome.PENDING, outcome.getOrNull())
        assertEquals(listOf(record("r1", value = 3_000)), remote.saved)
        assertEquals(listOf(entity("r1", value = 3_000)), dao.rows.value)
    }

    @Test
    fun `CA-LOC-01-03 si Firestore rechaza la escritura no queda en el telefono`() = runTest {
        remote.result = Result.failure(IllegalStateException("sin permiso"))

        val outcome = repository.saveRecord(record("r1"))

        assertTrue(outcome.isFailure)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `CA-LOC-01-04 si Firestore falla sigue con lo guardado y no se cierra`() = runTest(UnconfinedTestDispatcher()) {
        dao.upsert(entity("a"))
        remote.history = flow { throw IllegalStateException("PERMISSION_DENIED") }
        val seen = mutableListOf<List<CookingRecord>>()

        val job = launch { repository.observeHistory().collect { seen += it } }

        assertEquals(listOf("a"), seen.last().map { it.id })
        job.cancel()
    }

    @Test
    fun `CA-LOC-01-05 otra cuenta no ve el historial ajeno y sin sesion no hay historial`() = runTest {
        dao.upsert(entity("a", uid = "ana"))

        uid.uid = "beto"
        assertTrue(repository.observeHistory().first().isEmpty())
        uid.uid = null
        assertTrue(repository.observeHistory().first().isEmpty())
    }

    @Test
    fun `CA-LOC-01-02 el identificador nuevo sale de Firestore`() {
        assertEquals("nuevo", repository.newRecordId())
    }
}
