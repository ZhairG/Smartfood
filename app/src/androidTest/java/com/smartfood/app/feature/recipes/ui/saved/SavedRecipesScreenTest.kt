package com.smartfood.app.feature.recipes.ui.saved

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedRecipeCard
import com.smartfood.app.feature.recipes.domain.model.SavedSort
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SavedRecipesScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas instrumentadas de Guardadas sin estado (spec 010, T008): chips, búsqueda, orden,
 * vacío y "Sin conexión" (CA-SAV-02-02 a 02-10).
 */
class SavedRecipesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<SavedRecipesEvent>()
    private val estado = mutableStateOf(SavedRecipesUiState(isEmptyOverall = false))

    private fun mostrar(inicial: SavedRecipesUiState = estado.value) {
        estado.value = inicial
        composeTestRule.setContent {
            SmartfoodTheme {
                SavedRecipesScreen(
                    state = estado.value,
                    onEvent = { eventos += it },
                    onSortMenuOpen = { },
                    onSortMenuClose = { },
                )
            }
        }
    }

    @Test
    fun caSav0202_muestraChipsYBuscador() = runTest {
        mostrar(conTarjetas())

        composeTestRule.onNodeWithText("Favoritas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Creadas por mí").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recientes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buscar receta guardado...").assertIsDisplayed()
    }

    @Test
    fun caSav0203_filtroFavoritasMuestraTarjetas() = runTest {
        mostrar(conTarjetas().copy(filter = SavedFilter.FAVORITES))

        composeTestRule.onNodeWithText("Arepas de chócolo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pasta verde").assertIsDisplayed()
    }

    @Test
    fun caSav0203 filtroCreadasPorMiEstaVacio() = runTest {
        mostrar(conTarjetas().copy(filter = SavedFilter.OWN))

        composeTestRule.onNodeWithText("Sin recetas").assertIsDisplayed()
    }

    @Test
    fun caSav0204 buscarFiltraTarjetas() = runTest {
        mostrar(conTarjetas().copy(filter = SavedFilter.FAVORITES))

        composeTestRule.onNodeWithText("Buscar receta guardado...").performTextInput("PASTA")
        composeTestRule.onNodeWithText("Pasta verde").assertIsDisplayed()
        composeTestRule.onNodeWithText("Arepas de chócolo").assertIsNotDisplayed()
    }

    @Test
    fun caSav0204 buscarSinCoincidenciasMuestraVacio() = runTest {
        mostrar(conTarjetas().copy(filter = SavedFilter.FAVORITES))

        composeTestRule.onNodeWithText("Buscar receta guardado...").performTextInput("XYZ")
        composeTestRule.onNodeWithText("Sin recetas").assertIsDisplayed()
    }

    @Test
    fun caSav0205 ordenarPorNombreAZ() = runTest {
        mostrar(conTarjetas().copy(sort = SavedSort.NAME))

        composeTestRule.onNodeWithText("Arepas de chócolo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pasta verde").assertIsDisplayed()
        // Verificar orden alfabético: Arepas antes que Pasta
        val arepasNode = composeTestRule.onNodeWithText("Arepas de chócolo")
        val pastaNode = composeTestRule.onNodeWithText("Pasta verde")
        val arepasPos = arepasNode.first().positionInRoot().topLeft.y
        val pastaPos = pastaNode.first().positionInRoot().topLeft.y
        assertTrue(arepasPos < pastaPos)
    }

    @Test
    fun caSav0207 estadoVacioMuestraIconoYBoton() = runTest {
        mostrar(SavedRecipesUiState(isEmptyOverall = true))

        composeTestRule.onNodeWithText("Aún no creas recetas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guarda una recomendación de IA o crea tu primera receta desde cero.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crear receta").assertIsDisplayed()
    }

    @Test
    fun caSav0209 avisoSinConexionMuestraMensaje() = runTest {
        mostrar(conTarjetas().copy(hasPendingSync = true))

        composeTestRule.onNodeWithText("Sin conexión").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tus recetas guardadas siguen disponibles. Los cambios se sincronizarán después.").assertIsDisplayed()
    }

    @Test
    fun caSav0202_tocarChipoFavoritasEmiteEvento() = runTest {
        mostrar(conTarjetas())

        composeTestRule.onNodeWithText("Favoritas").performClick()

        val eventosRecibidos = eventos.filter { it is SavedRecipesEvent.FilterChanged }
        assertEquals(1, eventosRecibidos.size)
        assertEquals(SavedFilter.FAVORITES, (eventosRecibidos.first() as SavedRecipesEvent.FilterChanged).filter)
    }

    @Test
    fun caSav0202_tocarCrearRecetaEmiteEvento() = runTest {
        mostrar(conTarjetas())

        composeTestRule.onNodeWithText("Crear receta").performClick()

        val eventosRecibidos = eventos.filter { it is SavedRecipesEvent.AddClicked }
        assertEquals(1, eventosRecibidos.size)
    }

    private fun conTarjetas(
        filter: SavedFilter = SavedFilter.FAVORITES,
        sort: SavedSort = SavedSort.RECENT,
        isEmptyOverall: Boolean = false,
        hasPendingSync: Boolean = false,
    ) = SavedRecipesUiState(
        filter = filter,
        sort = sort,
        isEmptyOverall = isEmptyOverall,
        hasPendingSync = hasPendingSync,
        cards = listOf(
            SavedRecipeCard(
                id = "arepas",
                name = "Arepas de chócolo",
                photo = "receta_arepas",
                servings = 2,
                minutes = 30,
                isFavorite = true,
                isOwn = false,
                lastChangeMillis = 1_000L,
                isPendingSync = false,
            ),
            SavedRecipeCard(
                id = "pasta",
                name = "Pasta verde",
                photo = "receta_pasta",
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