package com.smartfood.app.core.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.components.SmartFoodConfirmDialog
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SmartFoodDialogsTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de los diálogos reutilizables de confirmación y resultado del inventario.
 *
 * Cada prueba cierra su diálogo al terminar con [cerrarDialogo]: `Dialog` abre una ventana aparte y, si queda abierta,
 * la prueba siguiente no alcanza a crear su propia jerarquía de Compose.
 */
class SmartFoodDialogsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // CA-INV-04-01 y CA-INV-05-04: "¿Desea eliminar el producto?" y "Eliminar" confirman la acción.
    @Test
    fun t025_caInv0401_confirmacionMuestraPreguntaYConfirmaEliminacion() {
        var confirmaciones = 0

        val visible = mostrarConfirmacion(
            onConfirm = { confirmaciones++ },
            onDismiss = {}
        )

        composeTestRule.onNodeWithText("¿Desea eliminar el producto?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Eliminar").performClick()

        assertEquals(1, confirmaciones)
        cerrarDialogo(visible)
    }

    // CA-INV-04-03 y CA-INV-05-04: "Cancelar" descarta la acción destructiva.
    @Test
    fun t025_caInv0401_confirmacionCancelaEliminacion() {
        var cancelaciones = 0

        val visible = mostrarConfirmacion(
            onConfirm = {},
            onDismiss = { cancelaciones++ }
        )

        composeTestRule.onNodeWithText("Cancelar").performClick()

        assertEquals(1, cancelaciones)
        cerrarDialogo(visible)
    }

    // CA-INV-02-06 y CA-INV-03-02: el resultado muestra título y cuerpo, y se anuncia como liveRegion (RNF-INV-04).
    @Test
    fun t025_caInv0206_resultadoMuestraTituloCuerpoYSeAnuncia() {
        val visible = mostrarResultado(
            title = "Producto Guardado",
            body = "Te avisaremos antes de que caduque."
        )

        composeTestRule.onNodeWithText("Producto Guardado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Te avisaremos antes de que caduque.").assertIsDisplayed()
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        ).assertIsDisplayed()

        cerrarDialogo(visible)
    }

    // CA-INV-02-06: el cuerpo es opcional y no se dibuja cuando no llega.
    @Test
    fun t025_caInv0206_resultadoSinCuerpoNoMuestraTextoAdicional() {
        val visible = mostrarResultado(title = "Guardado", body = null)

        composeTestRule.onNodeWithText("Guardado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Te avisaremos antes de que caduque.").assertDoesNotExist()

        cerrarDialogo(visible)
    }

    /** Abre la confirmación y devuelve su visibilidad, para poder cerrarla al final de la prueba. */
    private fun mostrarConfirmacion(onConfirm: () -> Unit, onDismiss: () -> Unit): MutableState<Boolean> {
        val visible = mutableStateOf(true)
        composeTestRule.setContent {
            SmartfoodTheme {
                if (visible.value) {
                    SmartFoodConfirmDialog(
                        icon = { IconoEliminacion() },
                        question = "¿Desea eliminar el producto?",
                        confirmText = "Eliminar",
                        dismissText = "Cancelar",
                        onConfirm = onConfirm,
                        onDismiss = onDismiss
                    )
                }
            }
        }
        return visible
    }

    /** Abre el resultado y devuelve su visibilidad, para poder cerrarlo al final de la prueba. */
    private fun mostrarResultado(title: String, body: String?): MutableState<Boolean> {
        val visible = mutableStateOf(true)
        composeTestRule.setContent {
            SmartfoodTheme {
                if (visible.value) {
                    SmartFoodResultDialog(
                        icon = { IconoResultado() },
                        title = title,
                        body = body
                    )
                }
            }
        }
        return visible
    }

    /** Quita el diálogo de la composición y espera a que se cierre su ventana. */
    private fun cerrarDialogo(visible: MutableState<Boolean>) {
        visible.value = false
        composeTestRule.waitForIdle()
    }

    @Composable
    private fun IconoResultado() {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * 0.4f)
        )
    }

    @Composable
    private fun IconoEliminacion() {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * 0.4f)
        )
    }
}
