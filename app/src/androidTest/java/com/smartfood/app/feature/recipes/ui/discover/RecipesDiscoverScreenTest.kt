package com.smartfood.app.feature.recipes.ui.discover

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RecipesDiscoverScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de Descubrir sin estado (spec 009, CA-RIA-01-01 a 01-04, T007; Figma
 * `557:3028`): encabezado, búsqueda, tarjeta de la IA, filtros, tarjetas con foto/tiempo/kcal/costo, aviso del
 * ingrediente por vencer, estados vacío/error y eventos. No dibuja la barra inferior.
 */
class RecipesDiscoverScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<RecipesDiscoverEvent>()
    private val estado = mutableStateOf(RecipesDiscoverUiState(isLoading = false))

    private fun mostrar(inicial: RecipesDiscoverUiState = RecipesDiscoverUiState(isLoading = false)) {
        estado.value = inicial
        composeTestRule.setContent {
            SmartfoodTheme {
                RecipesDiscoverScreen(
                    state = estado.value,
                    onEvent = { eventos += it }
                )
            }
        }
    }

    @Test
    fun caRia0101_muestraElEncabezadoLaBusquedaLaTarjetaDeLaIaYLosFiltros() {
        mostrar()

        composeTestRule.onNode(hasText("Recetas") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Buscar receta o ingrediente...").assertIsDisplayed()
        composeTestRule.onNodeWithText("¿Qué cocinamos hoy?").assertIsDisplayed()
        composeTestRule.onNodeWithText("La IA prioriza lo que está por vencer y cuida tu presupuesto.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Para ti").assertIsSelected()
        composeTestRule.onNodeWithText("≤ 30 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("Económicas").assertIsDisplayed()
    }

    @Test
    fun caRia0101_sugerirmeRecetasQuedaDeshabilitadoHastaQueExistaElFlujoDeIa() {
        mostrar()

        composeTestRule.onNodeWithText("Sugerirme recetas").assertIsNotEnabled()
    }

    @Test
    fun caRia0101_laTarjetaMuestraNombreTiempoKcalYCosto() {
        mostrar(conRecetas())

        composeTestRule.onNodeWithText("Pollo guisado con arroz").assertIsDisplayed()
        composeTestRule.onNodeWithText("35 min · 520 kcal · COP $8.400").assertIsDisplayed()
    }

    @Test
    fun caRia0102_laRecetaQueUsaUnIngredientePorVencerLoAvisa() {
        mostrar(conRecetas())

        composeTestRule.onNodeWithText("Usa pollo próximo a vencer").assertIsDisplayed()
    }

    @Test
    fun caRia0101_tocarUnaTarjetaEmiteRecipeClicked() {
        mostrar(conRecetas())

        composeTestRule.onNodeWithText("Pollo guisado con arroz").performClick()

        assertEquals(
            listOf<RecipesDiscoverEvent>(RecipesDiscoverEvent.RecipeClicked("pollo-guisado")),
            eventos
        )
    }

    @Test
    fun caRia0103_tocarUnFiltroEmiteFilterSelected() {
        mostrar(conRecetas())

        composeTestRule.onNodeWithText("≤ 30 min").performClick()
        composeTestRule.onNodeWithText("Económicas").performClick()

        assertEquals(
            listOf<RecipesDiscoverEvent>(
                RecipesDiscoverEvent.FilterSelected(RecipeFilter.UNDER_30),
                RecipesDiscoverEvent.FilterSelected(RecipeFilter.ECONOMIC)
            ),
            eventos
        )
    }

    @Test
    fun caRia0104_escribirEnLaBusquedaEmiteQueryChanged() {
        mostrar()

        composeTestRule.onNodeWithContentDescription("Buscar receta o ingrediente...")
            .performTextInput("pollo")

        assertEquals(listOf<RecipesDiscoverEvent>(RecipesDiscoverEvent.QueryChanged("pollo")), eventos)
    }

    @Test
    fun caRia0104_laEquisDeLaBusquedaLimpiaElTexto() {
        mostrar(RecipesDiscoverUiState(isLoading = false, query = "pollo"))

        composeTestRule.onNodeWithContentDescription("Limpiar búsqueda").performClick()

        assertEquals(listOf<RecipesDiscoverEvent>(RecipesDiscoverEvent.ClearQuery), eventos)
    }

    @Test
    fun caRia0104_sinCoincidenciasMuestraElAviso() {
        mostrar(RecipesDiscoverUiState(isLoading = false, query = "lasaña"))

        composeTestRule.onNodeWithText("No hay recetas que coincidan").assertIsDisplayed()
    }

    @Test
    fun caRia0101_siElCatalogoNoSeLeeMuestraElError() {
        mostrar(RecipesDiscoverUiState(isLoading = false, hasError = true))

        composeTestRule.onNodeWithText("No pudimos cargar las recetas.").assertIsDisplayed()
    }

    @Test
    fun caRia0101_mientrasCargaMuestraElIndicador() {
        mostrar(RecipesDiscoverUiState(isLoading = true))

        composeTestRule.onNodeWithContentDescription("Cargando recetas").assertIsDisplayed()
    }

    @Test
    fun caRia0103_elFiltroActivoSeAnunciaComoSeleccionado() {
        mostrar(conRecetas().copy(filter = RecipeFilter.ECONOMIC))

        composeTestRule.onNode(hasText("Económicas") and isSelected()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Para ti").assertIsDisplayed()
        assertTrue(eventos.isEmpty())
    }

    private fun conRecetas() = RecipesDiscoverUiState(
        isLoading = false,
        recipes = listOf(
            RecipeCardUi(
                id = "pollo-guisado",
                name = "Pollo guisado con arroz",
                photo = "receta_pollo_guisado",
                minutes = 35,
                kcal = 520,
                costCop = 8_400,
                expiringIngredient = "Pollo"
            ),
            RecipeCardUi(
                id = "pasta-verde",
                name = "Pasta verde rápida",
                photo = "receta_pasta_verde",
                minutes = 25,
                kcal = 480,
                costCop = 6_200
            )
        )
    )
}
