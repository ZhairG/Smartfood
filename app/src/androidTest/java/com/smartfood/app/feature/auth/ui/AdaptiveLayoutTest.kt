package com.smartfood.app.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.components.ActionSuccessScreen
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.ui.login.LoginScreen
import com.smartfood.app.feature.auth.ui.login.LoginUiState
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryScreen
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryUiState
import com.smartfood.app.feature.auth.ui.recovery.RecoveryStep
import com.smartfood.app.feature.auth.ui.register.RegisterScreen
import com.smartfood.app.feature.auth.ui.register.RegisterUiState
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.ui.editor.EditorMode
import com.smartfood.app.feature.inventory.ui.editor.ProductEditorScreen
import com.smartfood.app.feature.inventory.ui.editor.ProductEditorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate
import kotlin.math.ceil

/**
 * Nombre: AdaptiveLayoutTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Verifica que cada pantalla de formulario cabe sin scroll (spec 002, HU-UI-01): con el teclado
 * cerrado, ningún contenedor desplazable tiene rango de scroll y el botón principal es visible.
 * Se ejecuta en 360×720 dp (perfil compacto), 384×857 dp (perfil medio, teléfono V2436) y 412×917 dp (tamaño de los
 * frames de Figma). En cada tamaño se monta un solo contenedor y se cambia la pantalla que dibuja (#105).
 */
@RunWith(Parameterized::class)
class AdaptiveLayoutTest(widthDp: Int, heightDp: Int) {

    companion object {
        // Se usan Int y no DpSize: DpSize es una value class y su constructor no es compatible con Parameterized.
        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}dp")
        fun tamanos(): List<Array<Any>> = listOf(
            arrayOf(360, 720),
            arrayOf(384, 857),
            arrayOf(412, 917)  // tamaño de los frames de Figma (CA-UI-07-03)
        )
    }

    private val screenSize = DpSize(widthDp.dp, heightDp.dp)

    @get:Rule
    val composeTestRule = createComposeRule()

    private val pantalla = mutableStateOf<@Composable () -> Unit>({})

    /** Monta un contenedor una sola vez; cada paso cambia la [pantalla] que dibuja. */
    private fun montar() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(screenSize)) {
                SmartfoodTheme { pantalla.value() }
            }
        }
    }

    private fun mostrar(content: @Composable () -> Unit) {
        pantalla.value = content
        // Primero se compone la pantalla nueva, como hacía setContent; si no, el adelanto del reloj empieza antes.
        composeTestRule.waitForIdle()
        // El scaffold baja de perfil solo si el desborde se mantiene; el reloj de la prueba es virtual y waitForIdle
        // no avanza esos delay, así que se deja pasar el tiempo para que cada pantalla use su perfil definitivo.
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.waitForIdle()
    }

    /** Ningún contenedor desplazable puede tener rango de scroll y [buttonText] debe verse completo. */
    private fun assertCabeSinScroll(pantalla: String, buttonText: String) {
        val scrollables = composeTestRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNodes()
        assertTrue("$pantalla: debe existir el contenedor del formulario", scrollables.isNotEmpty())
        scrollables.forEach { node ->
            val range = node.config[SemanticsProperties.VerticalScrollAxisRange]
            assertEquals("$pantalla: no debe haber scroll en $screenSize", 0f, range.maxValue(), 0.5f)
        }
        composeTestRule.onNode(hasText(buttonText) and hasClickAction()).assertIsDisplayed()
    }

    /**
     * CA-UI-01-01: login, registro (con y sin error) y los tres pasos de la recuperación caben sin scroll. CA-REC-04-04:
     * con la cuenta regresiva más larga ("37:12", límite de 5 por hora) el paso 2 sigue cabiendo, la fila cabe en una
     * línea y la píldora no se recorta. CA-UI-01-02: la pantalla de éxito muestra el botón completo.
     */
    @Test
    fun ca_ui_01_01_ca_ui_01_02_ca_rec_04_04_lasPantallasDeAutenticacionCabenSinScroll() {
        montar()
        val paso2 = PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, email = "demo@smartfood.com")
        listOf<Triple<String, String, @Composable () -> Unit>>(
            Triple("login", "Iniciar Sesión") { LoginScreen(LoginUiState(), {}, {}, {}) },
            Triple("registro", "Crear Cuenta") { RegisterScreen(RegisterUiState(acceptedTerms = true), {}, {}, {}, {}) },
            Triple("registro con error", "Crear Cuenta") {
                RegisterScreen(RegisterUiState(acceptedTerms = true, error = AuthError.EmailAlreadyInUse), {}, {}, {}, {})
            },
            Triple("recuperación paso 1", "Enviar Código") {
                PasswordRecoveryScreen(PasswordRecoveryUiState(step = RecoveryStep.EMAIL_REQUEST), {}, {})
            },
            Triple("recuperación paso 2", "Verificar Código") { PasswordRecoveryScreen(paso2, {}, {}) },
            Triple("recuperación paso 3", "Reestablecer Contraseña") {
                PasswordRecoveryScreen(PasswordRecoveryUiState(step = RecoveryStep.NEW_PASSWORD), {}, {})
            },
            Triple("paso 2 con cuenta regresiva", "Verificar Código") {
                PasswordRecoveryScreen(paso2.copy(resendCooldownSeconds = 2_232, cooldownEmail = "demo@smartfood.com"), {}, {})
            }
        ).forEach { (nombre, boton, contenido) ->
            mostrar(contenido)
            assertCabeSinScroll(nombre, boton)
        }

        val texto = textLayout("Reenviar código en")
        assertEquals("El texto debe quedar en una línea en $screenSize", 1, texto.lineCount)
        val pildora = textLayout("37:12")
        // hasVisualOverflow no sirve aquí: la semántica rearma el texto con el ancho máximo disponible y no con el
        // que recibió. Se compara el ancho que el texto necesita con el que ocupa.
        assertEquals("La píldora debe quedar en una línea en $screenSize", 1, pildora.lineCount)
        assertTrue(
            "La píldora no debe recortarse en $screenSize",
            ceil(pildora.multiParagraph.maxIntrinsicWidth) <= pildora.size.width
        )

        mostrar {
            ActionSuccessScreen(
                title = "¡Cuenta creada con éxito!",
                body = "Ya puedes acceder a todas nuestras funciones. Te enviamos un correo de confirmación.",
                buttonText = "Continuar",
                onButtonClick = {}
            )
        }
        composeTestRule.onNode(hasText("Continuar") and hasClickAction()).assertIsDisplayed()
    }

    /** Resultado del layout del texto visible [text], para comprobar líneas y recortes. */
    private fun textLayout(text: String): TextLayoutResult {
        val node = composeTestRule.onNodeWithText(text, useUnmergedTree = true)
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.first()
    }

    /**
     * T036, T054 y T055 (spec 006, RNF-INV-06): con el precio, el editor puede necesitar desplazarse en pantallas
     * bajas. En alta y en edición, el último campo (el precio) y el botón, que va dentro del formulario como el Figma,
     * se alcanzan desplazando.
     */
    @Test
    fun t036_rnfInv06_elEditorDeAltaYEdicionAlcanzaElPrecioYElBoton() {
        montar()
        listOf(
            "Añadir al Inventario" to ProductEditorUiState(),
            "Eliminar" to ProductEditorUiState(
                mode = EditorMode.EDIT,
                name = "Carne",
                expiresOn = LocalDate.of(2026, 9, 20),
                category = FoodCategory.PROTEINS,
                isSaveEnabled = true
            )
        ).forEach { (boton, estado) ->
            mostrar { ProductEditorScreen(estado, {}) }
            composeTestRule.onNodeWithContentDescription("Precio (opcional)").performScrollTo().assertIsDisplayed()
            composeTestRule.onNode(hasText(boton) and hasClickAction()).performScrollTo().assertIsDisplayed()
        }
    }
}
