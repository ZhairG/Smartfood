package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.testutil.inventoryItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: SearchInventoryUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [SearchInventoryUseCase]: texto, tildes, comas, plural y categoría
 * (HU-INV-06, HU-INV-07).
 */
class SearchInventoryUseCaseTest {

    private val search = SearchInventoryUseCase()
    private val day = LocalDate.of(2026, 9, 10)

    private val carne = inventoryItem(id = "carne", name = "Carne", category = FoodCategory.PROTEINS, expiresOn = day.plusDays(2))
    private val leche = inventoryItem(id = "leche", name = "Leche Entera", category = FoodCategory.DAIRY, expiresOn = day.plusDays(1))
    private val lacteo = inventoryItem(id = "lacteo", name = "Postre Lácteo", category = FoodCategory.DAIRY, expiresOn = day.plusDays(9))
    private val tomate = inventoryItem(id = "tomate", name = "Tomate", category = FoodCategory.VEGETABLES, expiresOn = day.plusDays(4))
    private val limon = inventoryItem(id = "limon", name = "Limón", category = FoodCategory.FRUITS, expiresOn = day.plusDays(6))
    private val caramelo = inventoryItem(id = "caramelo", name = "Caramelos", category = FoodCategory.OTHER, expiresOn = day.plusDays(30))
    private val items = listOf(carne, leche, lacteo, tomate, limon, caramelo)

    private fun ids(query: String, category: FoodCategory? = null) = search(items, query, category).map { it.id }

    @Test
    fun `CA-INV-06-02 el texto filtra por nombre que contiene el termino sin importar mayusculas`() {
        assertEquals(listOf("carne", "caramelo"), ids("Car"))
        assertEquals(listOf("carne", "caramelo"), ids("cAR"))
    }

    @Test
    fun `CA-INV-06-02 la busqueda ignora las tildes en el texto y en el nombre`() {
        assertEquals(listOf("lacteo"), ids("lacteo"))
        assertEquals(listOf("limon"), ids("LIMÓN"))
    }

    @Test
    fun `CA-INV-06-03 sin texto el filtro de categoria deja solo esa categoria`() {
        assertEquals(listOf("leche", "lacteo"), ids("", FoodCategory.DAIRY))
    }

    @Test
    fun `CA-INV-06-03 sin texto ni categoria se ven todos ordenados por fecha`() {
        assertEquals(listOf("leche", "carne", "tomate", "limon", "lacteo", "caramelo"), ids("  "))
    }

    @Test
    fun `CA-INV-06-04 texto y categoria se cumplen a la vez`() {
        assertEquals(listOf("carne"), ids("Car", FoodCategory.PROTEINS))
        assertEquals(emptyList<String>(), ids("Car", FoodCategory.DAIRY))
    }

    @Test
    fun `CA-INV-06-05 los terminos separados por comas coinciden con cualquiera`() {
        assertEquals(listOf("leche", "tomate"), ids("leche, tomate"))
        assertEquals(listOf("leche", "tomate"), ids(" , leche,,tomate , "))
    }

    @Test
    fun `CA-INV-06-05 un texto sin comas es un solo termino`() {
        assertEquals(emptyList<String>(), ids("Carne Pollo"))
        assertEquals(listOf("leche"), ids("leche  entera"))
    }

    @Test
    fun `CA-INV-07-04 un plural simple tambien coincide con el singular`() {
        assertEquals(listOf("tomate"), ids("tomates"))
        assertEquals(listOf("limon"), ids("limones"))
        assertEquals(listOf("leche", "tomate", "limon"), ids("leches, tomates, limones"))
    }

    @Test
    fun `CA-INV-07-04 un termino corto no se recorta hasta coincidir con todo`() {
        assertEquals(emptyList<String>(), ids("es"))
    }
}
