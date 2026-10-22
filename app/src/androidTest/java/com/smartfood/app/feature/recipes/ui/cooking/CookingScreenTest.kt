package com.smartfood.app.feature.recipes.ui.cooking

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: CookingScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [CookingIntroScreen] y [CookingScreen] (spec 009, HU-RIA-04, T023; Figma
 * `557:2819` y `557:2781`): "Antes de empezar" sin video, los pasos con su barra, "Anterior" / "Siguiente" /
 * "Terminar", la tarjeta del temporizador solo si el paso tiene tiempo, sin el enlace de técnicas (desviaciones §9),
 * "Salir" y los diálogos del cierre.
 */
class CookingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<CookingEvent>()
    private val estado = mutableStateOf(CookingUiState())

    private val pasos = listOf(
        RecipeStep("Calentar arepas", "Calienta las arepas en una sartén", minutes = 5),
        RecipeStep("Servir", "Sirve el hogao sobre las arepas", minutes = null)
    )

    private fun guia(inicial: CookingUiState) {
        estado.value = inicial
        composeTestRule.setContent {
            SmartfoodTheme {
                CookingScreen(state = estado.value, onEvent = { eventos += it }, onBack = {})
            }
        }
    }

    private fun cocinando(paso: Int = 0, segundos: Int = 300) = CookingUiState(
        isLoading = false,
        name = "Arepas con hogao",
        steps = pasos,
        currentStepIndex = paso,
        timerSecondsRemaining = segundos
    )

    @Test
    fun caRia0403_antesDeEmpezarMuestraLosPreparativosYComenzarGuiaSinVideo() {
        composeTestRule.setContent {
            SmartfoodTheme {
                CookingIntroScreen(
                    state = CookingUiState(
                        isLoading = false,
                        name = "Arepas con hogao",
                        beforeStarting = listOf("Ten listas las arepas", "Pica el tomate")
                    ),
                    onEvent = { eventos += it },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Antes de empezar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Arepas con hogao, paso a paso").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prepara antes de iniciar").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Ten listas las arepas ✓ Pica el tomate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Omitir video").assertDoesNotExist()
        composeTestRule.onNodeWithText("Video", substring = true).assertDoesNotExist()

        composeTestRule.onNodeWithText("Comenzar guía").performClick()
        assertEquals(listOf<CookingEvent>(CookingEvent.StartGuideClicked), eventos)
    }

    @Test
    fun caRia0404_elPrimerPasoMuestraSuTextoYElTemporizadorSinElEnlaceDeTecnicas() {
        guia(cocinando())

        composeTestRule.onNodeWithText("Cocinando · paso 1 de 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("PASO 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calentar arepas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calienta las arepas en una sartén").assertIsDisplayed()
        composeTestRule.onNodeWithText("⏱ Temporizador · 05:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anterior").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Siguiente").assertIsEnabled()
        composeTestRule.onNodeWithText("técnica", substring = true).assertDoesNotExist()
    }

    @Test
    fun caRia0404_tocarLaTarjetaManejaElTemporizadorYReiniciarLoReinicia() {
        guia(cocinando(segundos = 222))

        composeTestRule.onNodeWithText("⏱ Temporizador · 03:42").performClick()
        composeTestRule.onNodeWithText("Reiniciar").performClick()

        assertEquals(listOf(CookingEvent.TimerToggled, CookingEvent.ResetTimerClicked), eventos)
    }

    @Test
    fun caRia0404_alLlegarACeroLaTarjetaDiceTiempoYSeTocaParaDetenerLaAlarma() {
        guia(cocinando(segundos = 0).copy(isAlarmRinging = true))

        composeTestRule.onNodeWithText("⏱ ¡Tiempo!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Toca la tarjeta para detener la alarma.").performClick()

        assertEquals(listOf<CookingEvent>(CookingEvent.TimerToggled), eventos)
    }

    @Test
    fun caRia0404_unPasoSinTiempoNoMuestraElTemporizador() {
        guia(cocinando(paso = 1))

        composeTestRule.onNodeWithText("Temporizador", substring = true).assertDoesNotExist()
    }

    @Test
    fun caRia0404_siguienteAnteriorYSalirAvisanSusEventos() {
        guia(cocinando())
        composeTestRule.onNodeWithText("Siguiente").performClick()
        estado.value = cocinando(paso = 1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Anterior").performClick()
        composeTestRule.onNodeWithText("Salir").performClick()

        assertEquals(
            listOf(CookingEvent.NextStepClicked, CookingEvent.PreviousStepClicked, CookingEvent.ExitClicked),
            eventos
        )
    }

    @Test
    fun caRia0405_enElUltimoPasoTerminarPreguntaSiCocinasteLaReceta() {
        guia(cocinando(paso = 1))
        composeTestRule.onNodeWithText("Siguiente").assertDoesNotExist()

        composeTestRule.onNodeWithText("Terminar").performClick()
        estado.value = cocinando(paso = 1).copy(dialog = CookingDialog.ConfirmFinish)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("¿Cocinaste esta receta?").assertIsDisplayed()
        assertEquals(listOf<CookingEvent>(CookingEvent.FinishClicked), eventos)
    }

    @Test
    fun caRia0405_muestraGuardadoGuardadoEnTuTelefonoYElError() {
        guia(cocinando(paso = 1).copy(dialog = CookingDialog.Saved(onPhone = false)))
        composeTestRule.onNodeWithText("Guardado").assertIsDisplayed()

        estado.value = cocinando(paso = 1).copy(dialog = CookingDialog.Saved(onPhone = true))
        composeTestRule.onNodeWithText("Guardado en tu teléfono").assertIsDisplayed()

        estado.value = cocinando(paso = 1).copy(dialog = CookingDialog.Failed(InventoryError.Network))
        composeTestRule.onNodeWithText("No pudimos guardar").assertIsDisplayed()
    }
}
