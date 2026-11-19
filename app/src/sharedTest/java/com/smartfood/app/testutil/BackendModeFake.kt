package com.smartfood.app.testutil

import com.smartfood.app.core.backend.BackendMode
import com.smartfood.app.core.backend.BackendModeProvider

/**
 * Nombre: BackendModeFake.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Interruptor del backend en memoria para las pruebas de la spec 012. Por defecto, el modo gratuito,
 * igual que la app cuando no puede leer Remote Config (CA-SPK-01-01).
 * @param mode modo inicial.
 */
class FakeBackendModeProvider(var mode: BackendMode = BackendMode.FREE) : BackendModeProvider {
    var refreshCalls = 0
        private set

    override fun current(): BackendMode = mode

    override fun refresh() {
        refreshCalls++
    }
}
