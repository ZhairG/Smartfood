package com.smartfood.app.core.designsystem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.core.view.WindowCompat
import com.smartfood.app.HiltTestActivity
import com.smartfood.app.core.navigation.AppNavHost
import com.smartfood.app.feature.auth.ui.login.LoginUiState
import com.smartfood.app.feature.auth.ui.login.LoginScreen
import com.smartfood.app.feature.splash.SplashScreen
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> StatusBarAdaptabilityTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 28/09/2026<br>
 * <b>Descripción:</b> Pruebas instrumentadas para verificar la adaptabilidad dinámica del Status Bar e íconos de la<br>
 * barra de estado según el fondo de pantalla activo.<br>
 * </build>
 */
@HiltAndroidTest
class StatusBarAdaptabilityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun inicializarHilt() {
        hiltRule.inject()
    }

    @Test
    fun test_statusBar_enSplashScreen_configuraIconosOscurosParaFondoClaro() {
        composeTestRule.setContent {
            SmartfoodTheme {
                SplashScreen(destination = null, onNavigate = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Logo de SmartFood").assertIsDisplayed()

        val activity = composeTestRule.activity
        val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        assertTrue(insetsController.isAppearanceLightStatusBars)
    }

    @Test
    fun test_statusBar_enLoginScreen_configuraIconosBlancosParaFondoOscuro() {
        composeTestRule.setContent {
            SmartfoodTheme {
                LoginScreen(
                    state = LoginUiState(),
                    onEvent = {},
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Bienvenido").assertIsDisplayed()

        val activity = composeTestRule.activity
        val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        assertFalse(insetsController.isAppearanceLightStatusBars)
    }

    @Test
    fun test_statusBar_transicionSplashALogin_adaptaEstiloDinamicamente() {
        composeTestRule.setContent {
            SmartfoodTheme {
                AppNavHost()
            }
        }

        // 1. En Splash Screen: el fondo es claro, los íconos deben ser oscuros (isAppearanceLightStatusBars = true)
        composeTestRule.onNodeWithContentDescription("Logo de SmartFood").assertIsDisplayed()
        val activity = composeTestRule.activity
        val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        assertTrue(insetsController.isAppearanceLightStatusBars)

        // 2. Transición a Login Screen (después del delay de Splash)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Bienvenido").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Bienvenido").assertIsDisplayed()

        // 3. Al llegar a Login: el fondo es oscuro, los íconos deben adaptarse a blancos (isAppearanceLightStatusBars = false)
        assertFalse(insetsController.isAppearanceLightStatusBars)
    }
}
