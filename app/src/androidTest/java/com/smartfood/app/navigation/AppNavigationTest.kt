package com.smartfood.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.smartfood.app.ui.theme.SmartfoodTheme
import org.junit.Rule
import org.junit.Test
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before

/**
 * Nombre: AppNavigationTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Prueba de Integración Instrumental de la Navegación (Type-Safe).
 * Esta prueba garantiza que el host de Compose no sufra un "Crash" por causa
 * de excepciones de Serialización como ocurrió en versiones previas (SerializationException).
 */
@HiltAndroidTest
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun appNavigation_startsAndDoesNotCrash() {
        // Pausamos el reloj para evitar la transicion automatica de SplashScreen a Login
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            SmartfoodTheme {
                AppNavigation()
            }
        }

        composeTestRule.mainClock.advanceTimeBy(100)
        
        // Si el NavHost crashea por problemas con @Serializable, la ejecucion jamas llegará aqui.
        // Simulamos un comportamiento normal comprobando que, por defecto, AppNavigation() lanza la Splash (u otro layout seguro)
        // Solo importa que cargue sin fallos.
    }
}
