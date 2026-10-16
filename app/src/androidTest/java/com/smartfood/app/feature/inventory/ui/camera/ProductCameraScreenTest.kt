package com.smartfood.app.feature.inventory.ui.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: ProductCameraScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de la pantalla de cámara sin estado (spec 006, HU-INV-05, T033). La vista previa de CameraX se
 * reemplaza por un `Box`, así que no hace falta cámara: la captura real se verifica a mano (CA-INV-05-02).
 */
class ProductCameraScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var capturas = 0
    private var atras = 0
    private var ajustes = 0

    private fun mostrar(permission: CameraPermission, isCapturing: Boolean = false, captureFailed: Boolean = false) {
        composeTestRule.setContent {
            SmartfoodTheme {
                ProductCameraScreen(
                    permission = permission,
                    preview = { Box(Modifier.fillMaxSize().testTag(PREVIEW_TAG)) },
                    isCapturing = isCapturing,
                    captureFailed = captureFailed,
                    onCapture = { capturas++ },
                    onBack = { atras++ },
                    onOpenSettings = { ajustes++ }
                )
            }
        }
    }

    @Test
    fun caInv0501_conPermisoSeVeElVisorYElBotonCaptura() {
        mostrar(CameraPermission.GRANTED)

        composeTestRule.onNodeWithText("Foto del producto").assertIsDisplayed()
        composeTestRule.onNodeWithTag(PREVIEW_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Tomar foto").assertIsEnabled().performClick()

        assertEquals(1, capturas)
    }

    @Test
    fun caInv0501_mientrasCapturaElBotonQuedaDeshabilitado() {
        mostrar(CameraPermission.GRANTED, isCapturing = true)

        composeTestRule.onNodeWithContentDescription("Tomar foto").assertIsNotEnabled()
    }

    @Test
    fun siLaCapturaFallaSeAvisaYSePuedeReintentar() {
        mostrar(CameraPermission.GRANTED, captureFailed = true)

        composeTestRule.onNodeWithText("No se pudo tomar la foto. Intenta de nuevo.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Tomar foto").assertIsEnabled()
    }

    @Test
    fun caInv0503_permisoNegadoExplicaYOfreceAjustesYVolver() {
        mostrar(CameraPermission.DENIED)

        composeTestRule.onNodeWithTag(PREVIEW_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Necesitamos tu cámara").assertIsDisplayed()
        composeTestRule.onNodeWithText("Abrir ajustes").performClick()
        composeTestRule.onNodeWithText("Volver").performClick()

        assertEquals(1, ajustes)
        assertEquals(1, atras)
    }

    @Test
    fun rnfInv04_laFlechaAtrasTieneDescripcionYVuelve() {
        mostrar(CameraPermission.GRANTED)

        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(1, atras)
    }

    private companion object {
        const val PREVIEW_TAG = "vista_previa"
    }
}
