package com.smartfood.app.core.search.domain

import com.smartfood.app.testutil.FakeRecentSearchesRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: RecentSearchUseCasesTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas de los casos de uso de las búsquedas recientes (spec 014, T004, HU-LOC-02): qué se guarda
 * (CA-LOC-02-02), sin duplicados y con un máximo de 10 (CA-LOC-02-04), se muestran 5 (CA-LOC-02-01) y «Borrar» solo
 * quita las de una barra (CA-LOC-02-06).
 */
class RecentSearchUseCasesTest {

    /** Reloj que avanza 1 s cada vez que se lee, para que cada búsqueda tenga una fecha distinta. */
    private class TickingClock : Clock() {
        private var seconds = 0L
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant(): Instant = Instant.ofEpochSecond(++seconds)
    }

    private val repository = FakeRecentSearchesRepository()
    private val record = RecordRecentSearchUseCase(repository, TickingClock())
    private val observe = ObserveRecentSearchesUseCase(repository)
    private val clear = ClearRecentSearchesUseCase(repository)

    @Test
    fun `CA-LOC-02-02 guarda el texto recortado con al menos 2 letras`() = runTest {
        record(SearchScope.RECIPES, "  pollo  ")
        record(SearchScope.RECIPES, "a")
        record(SearchScope.RECIPES, "   ")
        record(SearchScope.RECIPES, "12")

        assertEquals(listOf("pollo"), repository.texts(SearchScope.RECIPES))
    }

    @Test
    fun `CA-LOC-02-04 repetir una busqueda con otras mayusculas o tildes no la duplica y la sube`() = runTest {
        record(SearchScope.RECIPES, "limón")
        record(SearchScope.RECIPES, "arroz")
        record(SearchScope.RECIPES, "Limon")

        assertEquals(listOf("Limon", "arroz"), repository.texts(SearchScope.RECIPES))
    }

    @Test
    fun `CA-LOC-02-04 se conservan las 10 ultimas de cada barra`() = runTest {
        (1..12).forEach { record(SearchScope.INVENTORY, "producto $it") }

        val texts = repository.texts(SearchScope.INVENTORY)
        assertEquals(RecordRecentSearchUseCase.MAX_KEPT, texts.size)
        assertEquals("producto 12", texts.first())
        assertEquals("producto 3", texts.last())
    }

    @Test
    fun `CA-LOC-02-01 se muestran las 5 mas recientes, la ultima primero`() = runTest {
        (1..7).forEach { record(SearchScope.RECIPES, "receta $it") }

        assertEquals((7 downTo 3).map { "receta $it" }, observe(SearchScope.RECIPES).first())
    }

    @Test
    fun `CA-LOC-02-06 borrar solo quita las de esa barra`() = runTest {
        record(SearchScope.RECIPES, "pollo")
        record(SearchScope.INVENTORY, "leche")

        clear(SearchScope.RECIPES)

        assertTrue(observe(SearchScope.RECIPES).first().isEmpty())
        assertEquals(listOf("leche"), observe(SearchScope.INVENTORY).first())
    }
}
