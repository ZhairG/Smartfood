package com.smartfood.app.feature.recipes.ui.saved

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedRecipeCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SavedRecipesScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas instrumentadas de Guardadas sin estado (spec 010, T008): lista, "Ordenar", "+", vacíos,
 * "Sin conexión", carga y error (CA-SAV-02-02 a 02-11). La búsqueda y el orden reales se prueban en
 * `ObserveSavedRecipesUseCaseTest`.
 */
class SavedRecipesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<SavedRecipesEvent>()

    private fun mostrar(estado: SavedRecipesUiState) {
        composeTestRule.setContent {
            SmartfoodTheme {
                SavedRecipesScreen(state = estado, onEvent = { eventos += it })
            }
        }
    }

    @Test
    fun caSav0202_muestraEncabezadoBuscadorChipsTarjetaYBotonCrear() {
        mostrar(conTarjetas())

        composeTestRule.onNode(hasText("Guardadas") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ordenar").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Buscar receta guardado...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Favoritas").assertIsSelected()
        composeTestRule.onNodeWithText("Creadas por mí").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recientes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pasta verde").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 porciones · 20 min").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Crear receta").assertIsDisplayed()
    }

    @Test
    fun caSav0102_tocarElCorazonDeLaTarjetaEmiteFavoriteToggled() {
        mostrar(conTarjetas())

        composeTestRule.onNodeWithContentDescription("Quitar de favoritas").performClick()

        assertEquals(listOf<SavedRecipesEvent>(SavedRecipesEvent.FavoriteToggled("pasta")), eventos)
    }

    @Test
    fun caSav0203_tocarUnChipEmiteFilterChanged() {
        mostrar(conTarjetas())

        composeTestRule.onNodeWithText("Creadas por mí").performClick()

        assertEquals(listOf<SavedRecipesEvent>(SavedRecipesEvent.FilterChanged(SavedFilter.OWN)), eventos)
    }

    @Test
    fun caSav0204_sinCoincidenciasMuestraElMensaje() {
        mostrar(SavedRecipesUiState(isLoading = false, query = "xyz"))

        composeTestRule.onNodeWithText("No encontramos recetas con ese nombre").assertIsDisplayed()
    }

    @Test
    fun caSav0205_tocarOrdenarEmiteSortClicked() {
        mostrar(conTarjetas())

        composeTestRule.onNodeWithText("Ordenar").performClick()

        assertEquals(listOf<SavedRecipesEvent>(SavedRecipesEvent.SortClicked), eventos)
    }

    @Test
    fun caSav0205_elMenuMuestraLasDosOpciones() {
        mostrar(conTarjetas().copy(isSortMenuOpen = true))

        composeTestRule.onNodeWithText("Más recientes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nombre A-Z").assertIsDisplayed()
    }

    @Test
    fun caSav0206_tocarUnaTarjetaEmiteCardClicked() {
        mostrar(conTarjetas())

        composeTestRule.onNodeWithText("Pasta verde").performClick()

        assertEquals(listOf<SavedRecipesEvent>(SavedRecipesEvent.CardClicked("pasta")), eventos)
    }

    @Test
    fun caSav0207_sinRecetasOcultaBuscadorChipsYOrdenarYMuestraElVacio() {
        mostrar(SavedRecipesUiState(isLoading = false, isEmptyOverall = true))

        composeTestRule.onNodeWithText("Aún no creas recetas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guarda una recomendación de IA o crea tu primera receta desde cero.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Crear receta").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Buscar receta guardado...").assertDoesNotExist()
        composeTestRule.onNodeWithText("Favoritas").assertDoesNotExist()
        composeTestRule.onNodeWithText("Ordenar").assertDoesNotExist()
    }

    @Test
    fun caSav0208_sinFavoritasMuestraElTextoCortoSinBoton() {
        mostrar(SavedRecipesUiState(isLoading = false))

        composeTestRule.onNodeWithText("Aún no tienes recetas favoritas. Toca ♡ en una receta para guardarla aquí.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Crear receta").assertDoesNotExist()
    }

    @Test
    fun caSav0208_creadasPorMiSinRecetasMuestraElTextoCorto() {
        mostrar(SavedRecipesUiState(isLoading = false, filter = SavedFilter.OWN))

        composeTestRule.onNodeWithText("Aún no has creado recetas.").assertIsDisplayed()
    }

    @Test
    fun caSav0209_conCambiosPendientesMuestraSinConexion() {
        mostrar(conTarjetas().copy(hasPendingSync = true))

        composeTestRule.onNodeWithText("Sin conexión").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Tus recetas guardadas siguen disponibles. Los cambios se sincronizarán después.")
            .assertIsDisplayed()
    }

    @Test
    fun caSav0210_tocarElBotonCrearEmiteAddClicked() {
        mostrar(conTarjetas())

        composeTestRule.onNodeWithContentDescription("Crear receta").performClick()

        assertEquals(listOf<SavedRecipesEvent>(SavedRecipesEvent.AddClicked), eventos)
    }

    @Test
    fun caSav0210_crearRecetaDelVacioEmiteAddClicked() {
        mostrar(SavedRecipesUiState(isLoading = false, isEmptyOverall = true))

        composeTestRule.onNodeWithText("Crear receta").performClick()

        assertEquals(listOf<SavedRecipesEvent>(SavedRecipesEvent.AddClicked), eventos)
    }

    @Test
    fun caSav0211_siFallaLaLecturaMuestraElErrorSinElVacio() {
        mostrar(SavedRecipesUiState(isLoading = false, hasError = true))

        composeTestRule.onNodeWithText("No pudimos cargar tus recetas guardadas.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aún no creas recetas").assertDoesNotExist()
    }

    @Test
    fun alCargarNoMuestraElVacio() {
        mostrar(SavedRecipesUiState())

        composeTestRule.onNodeWithContentDescription("Cargando recetas guardadas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aún no creas recetas").assertDoesNotExist()
        assertTrue(eventos.isEmpty())
    }

    private fun conTarjetas() = SavedRecipesUiState(
        isLoading = false,
        cards = listOf(
            SavedRecipeCard(
                id = "pasta",
                name = "Pasta verde",
                photo = "receta_pasta_verde",
                servings = 2,
                minutes = 20,
                isFavorite = true,
                isOwn = false,
                lastChangeMillis = 2_000L,
                isPendingSync = false,
            ),
        ),
    )
}
