package com.smartfood.app.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfood.app.ui.theme.SmartfoodTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: AppNavigationTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas instrumentadas de integración para verificar que el árbol de navegación
 * type-safe [AppNavigation] inicie correctamente sin fallos de serialización ni bloqueos de carga.
 */
@HiltAndroidTest
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun inicializarConfiguracionHilt() {
        hiltRule.inject()
    }

    @Test
    fun navegacionApp_iniciaCorrectamenteYSinErrores() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            SmartfoodTheme {
                AppNavigation()
            }
        }

        composeTestRule.mainClock.advanceTimeBy(100)
    }
}
