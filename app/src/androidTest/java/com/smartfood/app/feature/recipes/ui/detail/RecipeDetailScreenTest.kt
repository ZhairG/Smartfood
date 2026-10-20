package com.smartfood.app.feature.recipes.ui.detail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredientGroup
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredientLine
import com.smartfood.app.feature.recipes.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RecipeDetailScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas del detalle sin estado (spec 009, CA-RIA-04-01 y 04-02, T008; Figma
 * `557:2856`): barra con el nombre, tiempos y dificultad, "<Tienda> · precios de octubre 2026" (CA-RIA-04-07), grupos
 * con cantidades, el empaque que falta con su tienda y precio y "Básico de despensa",
 * "En tu inventario", selector de porciones con sus límites, "Empezar a cocinar" deshabilitado hasta la cocina guiada
 * (#115), estados de carga/error y la flecha atrás.
 */
class RecipeDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<RecipeDetailEvent>()
    private val estado = mutableStateOf(detalle())
    private var volvio = false

    private fun mostrar(inicial: RecipeDetailUiState = detalle()) {
        estado.value = inicial
        volvio = false
        composeTestRule.setContent {
            SmartfoodTheme {
                RecipeDetailScreen(
                    state = estado.value,
                    onEvent = { eventos += it },
                    onBack = { volvio = true }
                )
            }
        }
    }

    @Test
    fun caRia0401_muestraFotoTiemposDificultadYPreciosDeReferencia() {
        mostrar()

        composeTestRule.onNodeWithText("Pollo guisado con arroz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preparación").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cocción").assertIsDisplayed()
        composeTestRule.onNodeWithText("25 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reposo").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dificultad").assertIsDisplayed()
        composeTestRule.onNodeWithText("Media").assertIsDisplayed()
        composeTestRule.onNodeWithText("D1 · precios de octubre 2026").assertIsDisplayed()
    }

    @Test
    fun caRia0401_agrupaLosIngredientesConCantidadYCosto() {
        mostrar()

        composeTestRule.onNodeWithText("Ingredientes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Verduras").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cereales").assertIsDisplayed()
        composeTestRule.onNodeWithText("Carnes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomate 200 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomate 1 kg").assertIsDisplayed()
        composeTestRule.onNodeWithText("D1 · COP $4.200").assertIsDisplayed()
        composeTestRule.onNodeWithText("Arroz 300 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pollo 600 g").assertIsDisplayed()
    }

    @Test
    fun caRia0401_marcaLoQueAlcanzaDelInventario() {
        mostrar()

        composeTestRule.onAllNodesWithText("En tu inventario").assertCountEquals(2)
    }

    @Test
    fun caRia0402_losBotonesDePorcionesEmitenSusEventos() {
        mostrar()

        composeTestRule.onNodeWithContentDescription("Agregar una porción").performClick()
        composeTestRule.onNodeWithContentDescription("Quitar una porción").performClick()

        assertEquals(
            listOf<RecipeDetailEvent>(
                RecipeDetailEvent.IncreaseServings,
                RecipeDetailEvent.DecreaseServings
            ),
            eventos
        )
    }

    @Test
    fun caRia0402_elSelectorMuestraLasPorcionesElegidas() {
        mostrar(detalle().copy(servings = 6))

        composeTestRule.onNodeWithText("6 porciones").assertIsDisplayed()
    }

    @Test
    fun caRia0402_enLosLimitesLosBotonesQuedanDeshabilitados() {
        mostrar(detalle().copy(servings = 1))
        composeTestRule.onNodeWithContentDescription("Quitar una porción").assertIsNotEnabled()

        estado.value = detalle().copy(servings = 12)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Agregar una porción").assertIsNotEnabled()
    }

    @Test
    fun caRia0401_empezarACocinarQuedaDeshabilitadoHastaLaCocinaGuiada() {
        mostrar()

        composeTestRule.onNodeWithText("Empezar a cocinar").assertIsNotEnabled()
    }

    @Test
    fun caRia0401_empezarACocinarFlotaALaDerechaSinTaparElUltimoIngrediente() {
        mostrar()
        repeat(4) { composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeUp() } }

        val raiz = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val boton = composeTestRule.onNodeWithText("Empezar a cocinar").getUnclippedBoundsInRoot()
        val ultimo = composeTestRule.onNodeWithText("Pollo 600 g").getUnclippedBoundsInRoot()

        // Figma 557:2884: botón de 181 dp a la derecha, no una barra de todo el ancho.
        assertTrue("el botón va a la derecha", boton.left > raiz.left + (raiz.right - raiz.left) / 3)
        assertTrue("la última fila queda por encima del botón", ultimo.bottom <= boton.top)
    }

    @Test
    fun caRia0407_laTarjetaDiceMiSupermercadoYLaFilaLaTiendaDeSuPrecio() {
        val tomate = detalle().groups.first().lines.single()
            .copy(store = Store.EXITO, packs = 2, packLabel = "500 g", costCop = 3_200)
        mostrar(detalle().copy(store = Store.OLIMPICA, groups = listOf(RecipeIngredientGroup("Verduras", listOf(tomate)))))

        composeTestRule.onNodeWithText("Olímpica · precios de octubre 2026").assertIsDisplayed()
        // Olímpica no lo tiene en línea: la fila dice de qué tienda es el precio y cuántos empaques.
        composeTestRule.onNodeWithText("2 × Tomate 500 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Éxito · COP $3.200").assertIsDisplayed()
    }

    @Test
    fun caRia0401_unBasicoDeDespensaNoMuestraPrecio() {
        val aceite = RecipeIngredientLine(
            ingredientId = "aceite",
            name = "Aceite",
            section = "Otros",
            quantity = 15.0,
            unit = IngredientUnit.MILLILITER,
            inInventory = false,
            costCop = 0,
            pantryBasic = true
        )
        mostrar(detalle().copy(groups = listOf(RecipeIngredientGroup("Otros", listOf(aceite)))))

        composeTestRule.onNodeWithText("Aceite 15 ml").assertIsDisplayed()
        composeTestRule.onNodeWithText("Básico de despensa").assertIsDisplayed()
    }

    @Test
    fun caRia0401_siLaRecetaNoSeLeeMuestraElError() {
        mostrar(RecipeDetailUiState(isLoading = false, hasError = true))

        composeTestRule.onNodeWithText("No pudimos cargar la receta.").assertIsDisplayed()
    }

    @Test
    fun caRia0401_mientrasCargaNoMuestraElBotonDeCocinar() {
        mostrar(RecipeDetailUiState(isLoading = true))

        composeTestRule.onNodeWithText("Empezar a cocinar").assertDoesNotExist()
    }

    @Test
    fun caRia0401_laFlechaAtrasVuelve() {
        mostrar()

        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        assertTrue(volvio)
    }

    private fun detalle() = RecipeDetailUiState(
        isLoading = false,
        recipeId = "pollo-guisado",
        name = "Pollo guisado con arroz",
        photo = "receta_pollo_guisado",
        prepMinutes = 15,
        cookMinutes = 25,
        restMinutes = 0,
        difficulty = "Media",
        servings = 4,
        groups = listOf(
            RecipeIngredientGroup(
                section = "Verduras",
                lines = listOf(
                    RecipeIngredientLine(
                        ingredientId = "tomate",
                        name = "Tomate",
                        section = "Verduras",
                        quantity = 200.0,
                        unit = IngredientUnit.GRAM,
                        inInventory = false,
                        costCop = 4_200,
                        packs = 1,
                        packLabel = "1 kg",
                        store = Store.D1
                    )
                )
            ),
            RecipeIngredientGroup(
                section = "Cereales",
                lines = listOf(
                    RecipeIngredientLine(
                        ingredientId = "arroz",
                        name = "Arroz",
                        section = "Cereales",
                        quantity = 300.0,
                        unit = IngredientUnit.GRAM,
                        inInventory = true,
                        costCop = 0
                    )
                )
            ),
            RecipeIngredientGroup(
                section = "Carnes",
                lines = listOf(
                    RecipeIngredientLine(
                        ingredientId = "pollo",
                        name = "Pollo",
                        section = "Carnes",
                        quantity = 600.0,
                        unit = IngredientUnit.GRAM,
                        inInventory = true,
                        costCop = 0
                    )
                )
            )
        )
    )
}
