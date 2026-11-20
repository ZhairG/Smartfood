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
 * Nombre: RecentSearchDaoTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas de [RecentSearchDao] con una base Room en memoria (spec 014, T002): orden y límite, sin
 * duplicados por texto normalizado, recorte a las últimas, «Borrar» por barra y borrado al cerrar sesión.
 */
@RunWith(AndroidJUnit4::class)
class RecentSearchDaoTest {

    private lateinit var database: SmartFoodDatabase
    private lateinit var dao: RecentSearchDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SmartFoodDatabase::class.java,
        ).build()
        dao = database.recentSearchDao()
    }

    @After
    fun tearDown() = database.close()

    private fun search(text: String, usedAt: Long, scope: String = RECIPES, uid: String = "ana") =
        RecentSearchEntity(uid, scope, normalized = text.lowercase(), text = text, usedAt = usedAt)

    @Test
    fun caLoc0204_laMismaBusquedaNoSeDuplicaYSubeAlPrimerLugar() = runBlocking<Unit> {
        dao.upsert(search("pollo", usedAt = 1))
        dao.upsert(search("arroz", usedAt = 2))
        dao.upsert(search("Pollo", usedAt = 3).copy(normalized = "pollo"))

        assertEquals(listOf("Pollo", "arroz"), dao.observe("ana", RECIPES, limit = 5).first().map { it.text })
    }

    @Test
    fun caLoc0204_recortarDejaSoloLasMasRecientes() = runBlocking<Unit> {
        (1..12).forEach { dao.upsert(search("b$it", usedAt = it.toLong())) }

        dao.trim("ana", RECIPES, keep = 10)

        val kept = dao.observe("ana", RECIPES, limit = 20).first().map { it.text }
        assertEquals((12 downTo 3).map { "b$it" }, kept)
    }

    @Test
    fun caLoc0206_borrarSoloQuitaLasDeEsaBarra() = runBlocking<Unit> {
        dao.upsert(search("pollo", usedAt = 1, scope = RECIPES))
        dao.upsert(search("leche", usedAt = 2, scope = INVENTORY))

        dao.clear("ana", RECIPES)

        assertTrue(dao.observe("ana", RECIPES, limit = 5).first().isEmpty())
        assertEquals(listOf("leche"), dao.observe("ana", INVENTORY, limit = 5).first().map { it.text })
    }

    @Test
    fun caLoc0207_lasBarrasSonIndependientes() = runBlocking<Unit> {
        dao.upsert(search("pollo", usedAt = 1, scope = RECIPES))

        assertTrue(dao.observe("ana", INVENTORY, limit = 5).first().isEmpty())
    }

    @Test
    fun caLoc0208_otraCuentaNoLasVeYCerrarSesionLasBorra() = runBlocking<Unit> {
        dao.upsert(search("pollo", usedAt = 1, uid = "ana"))

        assertTrue(dao.observe("beto", RECIPES, limit = 5).first().isEmpty())
        dao.deleteAll()
        assertTrue(dao.observe("ana", RECIPES, limit = 5).first().isEmpty())
    }

    private companion object {
        const val RECIPES = "RECIPES"
        const val INVENTORY = "INVENTORY"
    }
}
