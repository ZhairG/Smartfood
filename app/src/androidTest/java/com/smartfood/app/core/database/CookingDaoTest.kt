package com.smartfood.app.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Nombre: CookingDaoTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas de [CookingDao] con una base Room en memoria (spec 014, T002): orden por fecha, filtro por
 * `uid` (RNF-LOC-02), reemplazo de la copia con lo que llega de Firestore y borrado al cerrar sesión.
 */
@RunWith(AndroidJUnit4::class)
class CookingDaoTest {

    private lateinit var database: SmartFoodDatabase
    private lateinit var dao: CookingDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SmartFoodDatabase::class.java,
        ).build()
        dao = database.cookingDao()
    }

    @After
    fun tearDown() = database.close()

    private fun cooking(id: String, uid: String = "ana", cookedAt: Long = 1_000L, value: Int = 5_000) =
        CookingEntity(id, uid, recipeId = "arroz-pollo", servings = 4, cookedAt = cookedAt, rescuedValueCop = value)

    @Test
    fun caLoc0101_devuelveLosRegistrosDelUsuarioDelMasRecienteAlMasAntiguo() = runBlocking<Unit> {
        dao.upsert(cooking("a", cookedAt = 1_000))
        dao.upsert(cooking("b", cookedAt = 3_000))
        dao.upsert(cooking("c", cookedAt = 2_000))

        assertEquals(listOf("b", "c", "a"), dao.observe("ana").first().map { it.id })
    }

    @Test
    fun caLoc0105_otraCuentaNoVeElHistorialAjeno() = runBlocking<Unit> {
        dao.upsert(cooking("a", uid = "ana"))
        dao.upsert(cooking("b", uid = "beto"))

        assertEquals(listOf("b"), dao.observe("beto").first().map { it.id })
    }

    @Test
    fun caLoc0102_reemplazarSoloCambiaLaCopiaDelUsuario() = runBlocking<Unit> {
        dao.upsert(cooking("viejo", uid = "ana"))
        dao.upsert(cooking("ajeno", uid = "beto"))

        dao.replaceAll("ana", listOf(cooking("nuevo1"), cooking("nuevo2", cookedAt = 2_000)))

        assertEquals(listOf("nuevo2", "nuevo1"), dao.observe("ana").first().map { it.id })
        assertEquals(listOf("ajeno"), dao.observe("beto").first().map { it.id })
    }

    @Test
    fun caLoc0103_guardarElMismoRegistroLoActualiza() = runBlocking<Unit> {
        dao.upsert(cooking("a", value = 1_000))
        dao.upsert(cooking("a", value = 2_000))

        assertEquals(listOf(2_000), dao.observe("ana").first().map { it.rescuedValueCop })
    }

    @Test
    fun caLoc0105_cerrarSesionBorraTodo() = runBlocking<Unit> {
        dao.upsert(cooking("a", uid = "ana"))
        dao.upsert(cooking("b", uid = "beto"))

        dao.deleteAll()

        assertTrue(dao.observe("ana").first().isEmpty())
        assertTrue(dao.observe("beto").first().isEmpty())
    }
}
