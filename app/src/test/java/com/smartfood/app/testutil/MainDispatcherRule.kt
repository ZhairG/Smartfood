package com.smartfood.app.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Nombre: MainDispatcherRule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Regla JUnit que reemplaza `Dispatchers.Main` por un dispatcher de prueba, para que
 * `viewModelScope` funcione en pruebas JVM.
 *
 * @property testDispatcher Dispatcher que reemplaza a Main durante la prueba.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
