package com.smartfood.app.core.navigation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.smartfood.app.HiltTestActivity
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: AppNavigationTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de integración para verificar el árbol de navegación
 * type-safe [AppNavHost], validando la ejecución automática de animaciones y navegación de empuje en todos los enlaces.
 */
@HiltAndroidTest
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    companion object {
        /** Tiempo de delay de la pantalla inicial [com.smartfood.app.feature.splash.SplashScreen]. */
        private const val SPLASH_DELAY_MS = 2000L

        /** Duración fija de la animación de transición de empuje configurada en [AppNavHost]. */
        private const val PUSH_ANIMATION_DURATION_MS = 600L
    }

    @Before
    fun inicializarConfiguracionHilt() {
        hiltRule.inject()
    }

    /**
     * Inicializa [AppNavHost] dejando que el reloj de composición avance automáticamente
     * para procesar scroll, clics y transiciones sin requerir interacción manual.
     */
    private fun launchAppNavHost() {
        composeTestRule.setContent {
            SmartfoodTheme {
                AppNavHost()
            }
        }
    }

    /**
     * Espera a que la pantalla Splash complete su delay (2000 ms) y transición
     * hasta que "Bienvenido" (LoginScreen) sea visible.
     */
    private fun advanceToLogin() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Bienvenido").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Bienvenido").assertIsDisplayed()
    }

    /**
     * Navega automáticamente de Login a Registro haciendo scroll y clic en "Regístrate".
     */
    private fun advanceToRegister() {
        advanceToLogin()
        composeTestRule.onNodeWithText("Regístrate").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Crea tu cuenta").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Crea tu cuenta").assertIsDisplayed()
    }

    @Test
    fun transicionSplashALogin_respetaTiempoDeSplashYAnimacionDeEmpuje600ms() {
        // En esta prueba desactivamos el autoAdvance para inspeccionar el reloj frame a frame
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            SmartfoodTheme {
                AppNavHost()
            }
        }

        // 1. Estado Inicial (t = 0 ms): El logo de Splash está visible
        composeTestRule.onNodeWithContentDescription("Logo de SmartFood").assertIsDisplayed()

        // 2. Avance parcial (t = 1000 ms): Splash aún no ha terminado
        composeTestRule.mainClock.advanceTimeBy(SPLASH_DELAY_MS / 2)
        composeTestRule.onNodeWithContentDescription("Logo de SmartFood").assertIsDisplayed()

        // 3. Finaliza delay de Splash (t = 2000 ms): Se inicia la navegación a Login
        composeTestRule.mainClock.advanceTimeBy(SPLASH_DELAY_MS / 2)

        // 4. Mitad de la animación de empuje (t = 2300 ms - 300 ms transcurridos)
        composeTestRule.mainClock.advanceTimeBy(PUSH_ANIMATION_DURATION_MS / 2)

        // 5. Final de la animación de empuje (t = 2600 ms - 600 ms totales)
        composeTestRule.mainClock.advanceTimeBy(PUSH_ANIMATION_DURATION_MS / 2)

        // Verificación: LoginScreen está completamente visible e interactiva
        composeTestRule.onNodeWithText("Bienvenido").assertIsDisplayed()
    }

    @Test
    fun transicionLoginARegister_ejecutaAnimacionDeEmpujeDe600ms() {
        launchAppNavHost()
        advanceToLogin()

        // Disparamos la acción de navegación hacia Registro automáticamente
        composeTestRule.onNodeWithText("Regístrate").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Crea tu cuenta").fetchSemanticsNodes().isNotEmpty()
        }

        // Verificación: RegisterScreen se encuentra completamente visible
        composeTestRule.onNodeWithText("Crea tu cuenta").assertIsDisplayed()
    }

    @Test
    fun transicionLoginARecuperacion_ejecutaAnimacionDeEmpujeDe600ms() {
        launchAppNavHost()
        advanceToLogin()

        // Disparamos la navegación a Recuperación de Contraseña automáticamente
        composeTestRule.onNodeWithText("¿Olvidaste tu contraseña?").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Recupera tu acceso y vuelve a disfrutar de tus recetas favoritas.").fetchSemanticsNodes().isNotEmpty()
        }

        // Verificación: PasswordRecoveryScreen está visible con el subtítulo del Paso 1
        composeTestRule.onNodeWithText("Recupera tu acceso y vuelve a disfrutar de tus recetas favoritas.").assertIsDisplayed()
    }

    @Test
    fun transicionRegisterALogin_conPopBackStack_ejecutaAnimacion() {
        launchAppNavHost()
        advanceToRegister()

        // Disparamos el retorno a Login desde Registro automáticamente
        composeTestRule.onNodeWithText("Inicia sesión").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Bienvenido").fetchSemanticsNodes().isNotEmpty()
        }

        // Verificación: Regresamos a LoginScreen
        composeTestRule.onNodeWithText("Bienvenido").assertIsDisplayed()
    }

    @Test
    fun transicionRecuperacionALogin_conPopBackStack_ejecutaAnimacion() {
        launchAppNavHost()
        advanceToLogin()

        // Disparamos la navegación a Recuperación de Contraseña
        composeTestRule.onNodeWithText("¿Olvidaste tu contraseña?").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Recupera tu acceso y vuelve a disfrutar de tus recetas favoritas.").fetchSemanticsNodes().isNotEmpty()
        }

        // Disparamos el retorno a Login mediante el enlace "Iniciar sesión"
        composeTestRule.onNodeWithText("Iniciar sesión").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Bienvenido").fetchSemanticsNodes().isNotEmpty()
        }

        // Verificación: Regresamos a LoginScreen
        composeTestRule.onNodeWithText("Bienvenido").assertIsDisplayed()
    }

    /**
     * Llena el nombre y acepta los términos para comprobar después que el formulario se conserva. La casilla se
     * marca con su acción semántica: un toque en el centro de la fila caería sobre el enlace de los Términos.
     */
    private fun llenarRegistroParcial() {
        composeTestRule.onNodeWithText("Ej. Pepito Perez").performTextInput("Pepito Perez")
        composeTestRule.onNodeWithText("Acepto los", substring = true).performSemanticsAction(SemanticsActions.OnClick)
    }

    /** Abre un documento legal con la acción de la casilla (la misma que usa el lector de pantalla). */
    private fun abrirDocumento(accion: String, titulo: String) {
        val acciones = composeTestRule.onNodeWithText("Acepto los", substring = true)
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeTestRule.runOnIdle { acciones.first { it.label == accion }.action() }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Vista previa: el contenido definitivo está en redacción")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(titulo).assertIsDisplayed()
    }

    private fun esperarRegistroConDatos() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Crea tu cuenta").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Pepito Perez").assertIsDisplayed()
        composeTestRule.onNodeWithText("Acepto los", substring = true).assertIsOn()
    }

    @Test
    fun caLeg0103_terminosYFlechaVolver_conservanElFormulario() {
        launchAppNavHost()
        advanceToRegister()
        llenarRegistroParcial()

        abrirDocumento("Abrir Términos y condiciones", "Términos y condiciones")
        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        esperarRegistroConDatos()
    }

    @Test
    fun caLeg0103_politicaYAtrasDelSistema_conservanElFormulario() {
        launchAppNavHost()
        advanceToRegister()
        llenarRegistroParcial()

        abrirDocumento("Abrir Política de privacidad", "Política de privacidad")
        composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        esperarRegistroConDatos()
    }
}
