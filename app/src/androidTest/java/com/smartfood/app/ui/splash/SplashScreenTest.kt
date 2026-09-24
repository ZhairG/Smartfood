package com.smartfood.app.ui.splash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.smartfood.app.ui.theme.SmartfoodTheme
import org.junit.Rule
import org.junit.Test

class SplashScreenTest {

    // Regla obligatoria para inicializar el entorno de tests de Jetpack Compose
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splashScreen_displaysLogo() {
        // Arrange: Evitamos que la corrutina dispare la navegación deteniendo su reloj interno
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            SmartfoodTheme {
                SplashScreen(onNavigateNext = {})
            }
        }

        // Assert: Esperamos un instante corto para que se dibuje
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("splash_logo").assertIsDisplayed()
    }
}
