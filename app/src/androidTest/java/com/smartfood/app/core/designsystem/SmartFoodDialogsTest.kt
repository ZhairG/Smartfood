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
import com.smartfood.app.core.designsystem.components.SmartFoodLoadingDialog
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.components.SmartFoodWarningDialog
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

    // CA-INV-04-01, 04-03 y CA-INV-05-04: "Eliminar" confirma la acción destructiva y "Cancelar" la descarta.
    @Test
    fun t025_caInv0401_0403_confirmacionPreguntaConfirmaYCancela() {
        var confirmaciones = 0
        var cancelaciones = 0
        val visible = mostrarConfirmacion(onConfirm = { confirmaciones++ }, onDismiss = { cancelaciones++ })

        composeTestRule.onNodeWithText("¿Desea eliminar el producto?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Eliminar").performClick()
        composeTestRule.onNodeWithText("Cancelar").performClick()

        assertEquals("confirmar y cancelar", 1 to 1, confirmaciones to cancelaciones)
        cerrarDialogo(visible)
    }

    // CA-INV-02-06 y CA-INV-03-02: el resultado muestra título y cuerpo opcional, y se anuncia como liveRegion
    // (RNF-INV-04); CA-INV-02-12 (T056): el diálogo de carga muestra la acción en curso y también se anuncia.
    @Test
    fun t025_t056_caInv0206_0212_resultadoYCargaMuestranSuTextoYSeAnuncian() {
        val cuerpo = mutableStateOf<String?>("Te avisaremos antes de que caduque.")
        val cargando = mutableStateOf(false)
        val visible = mutableStateOf(true)
        composeTestRule.setContent {
            SmartfoodTheme {
                when {
                    !visible.value -> Unit
                    cargando.value -> SmartFoodLoadingDialog(text = "Guardando producto…")
                    else -> SmartFoodResultDialog(icon = { IconoResultado() }, title = "Producto Guardado", body = cuerpo.value)
                }
            }
        }
        val anunciado = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

        composeTestRule.onNodeWithText("Producto Guardado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Te avisaremos antes de que caduque.").assertIsDisplayed()
        composeTestRule.onNode(anunciado).assertIsDisplayed()

        // El cuerpo es opcional y no se dibuja cuando no llega.
        cuerpo.value = null
        composeTestRule.onNodeWithText("Producto Guardado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Te avisaremos antes de que caduque.").assertDoesNotExist()

        cargando.value = true
        composeTestRule.onNodeWithText("Guardando producto…").assertIsDisplayed()
        composeTestRule.onNode(anunciado).assertIsDisplayed()

        cerrarDialogo(visible)
    }

    // CA-05-03 (Figma 604:2668): el aviso muestra título, explicación y los dos botones lado a lado; CA-INV-10-07: la
    // variante roja conserva el texto y los dos botones.
    @Test
    fun ca0503_caInv1007_avisoNormalYRojoMuestranSuTextoYAmbosBotones() {
        var confirmaciones = 0
        var cancelaciones = 0
        val rojo = mutableStateOf(false)
        val visible = mutableStateOf(true)
        composeTestRule.setContent {
            SmartfoodTheme {
                if (visible.value) {
                    SmartFoodWarningDialog(
                        title = "¿Cerrar sesión?",
                        body = if (rojo.value) "Tienes cambios sin subir." else "Tendrás que iniciar sesión nuevamente.",
                        confirmText = "Cerrar sesión",
                        dismissText = "Cancelar",
                        onConfirm = { confirmaciones++ },
                        onDismiss = { cancelaciones++ },
                        destructive = rojo.value
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("¿Cerrar sesión?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tendrás que iniciar sesión nuevamente.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cerrar sesión").performClick()
        composeTestRule.onNodeWithText("Cancelar").performClick()
        assertEquals("aviso normal", 1 to 1, confirmaciones to cancelaciones)

        rojo.value = true
        composeTestRule.onNodeWithText("Tienes cambios sin subir.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cerrar sesión").performClick()
        assertEquals("aviso rojo", 2, confirmaciones)
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
