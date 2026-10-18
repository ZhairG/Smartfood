package com.smartfood.app.feature.legal.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.smartfood.app.core.designsystem.theme.GreenLight
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.SurfaceDark
import com.smartfood.app.core.designsystem.theme.ThemeMode
import com.smartfood.app.feature.legal.domain.model.LegalDocumentType
import com.smartfood.app.feature.legal.domain.model.LegalVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: LegalDocumentScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de UI de la pantalla stateless [LegalDocumentScreen] (spec 005, HU-LEG-01). Hay una prueba
 * por modo de color; la del modo claro cambia de documento sin volver a montar la pantalla (#105).
 */
class LegalDocumentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tipo = mutableStateOf(LegalDocumentType.TERMS)
    private var regresos = 0

    /** Monta la pantalla una sola vez; el flujo cambia el documento con [tipo]. */
    private fun mostrar(themeMode: ThemeMode) {
        composeTestRule.setContent {
            SmartfoodTheme(themeMode = themeMode) {
                LegalDocumentScreen(type = tipo.value, onBack = { regresos++ })
            }
        }
    }

    private fun esEncabezado(texto: String) =
        hasText(texto) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    /**
     * Color del fondo en la esquina inferior izquierda. En este design system `background` es el verde de la marca:
     * si la pantalla lo usara, el título y los encabezados verdes no se verían.
     */
    private fun colorDeFondo(): Color {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        return Color(bitmap.getPixel(4, bitmap.height - 4))
    }

    @Test
    fun caLeg0103_0105_0106_modoClaroMuestraAmbosDocumentosConSusEncabezadosYVuelve() {
        mostrar(ThemeMode.LIGHT)
        // CA-LEG-01-05: título, aviso de vista previa y versión.
        composeTestRule.onNodeWithText("Términos y condiciones").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vista previa: el contenido definitivo está en redacción").assertIsDisplayed()
        composeTestRule.onNodeWithText("Versión ${LegalVersion.CURRENT}").assertIsDisplayed()
        // CA-LEG-01-06: el fondo es el panel claro.
        val fondo = colorDeFondo()
        assertTrue("El fondo debe ser GreenLight y fue $fondo", fondo.cercanoA(GreenLight))
        // CA-LEG-01-03: la flecha vuelve.
        composeTestRule.onNodeWithContentDescription("Volver").performClick()
        assertEquals("volver", 1, regresos)

        // CA-LEG-01-06: las secciones son encabezados y la última se alcanza con scroll.
        tipo.value = LegalDocumentType.PRIVACY
        composeTestRule.onNodeWithText("Política de privacidad").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vista previa: el contenido definitivo está en redacción").assertIsDisplayed()
        composeTestRule.onNode(esEncabezado("1. Responsable del tratamiento")).assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(esEncabezado("8. Contacto"))
        composeTestRule.onNode(esEncabezado("8. Contacto")).assertIsDisplayed()
    }

    @Test
    fun caLeg0106_modoOscuroMuestraElDocumentoSobreLaSuperficieOscura() {
        mostrar(ThemeMode.DARK)
        composeTestRule.onNodeWithText("Términos y condiciones").assertIsDisplayed()
        composeTestRule.onNode(esEncabezado("1. Aceptación de los términos")).assertIsDisplayed()
        val fondo = colorDeFondo()
        assertTrue("El fondo debe ser SurfaceDark y fue $fondo", fondo.cercanoA(SurfaceDark))
    }

    private fun Color.cercanoA(otro: Color, tolerancia: Float = 0.02f): Boolean =
        kotlin.math.abs(red - otro.red) <= tolerancia &&
            kotlin.math.abs(green - otro.green) <= tolerancia &&
            kotlin.math.abs(blue - otro.blue) <= tolerancia
}
