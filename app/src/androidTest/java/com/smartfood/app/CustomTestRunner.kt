package com.smartfood.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Nombre: CustomTestRunner.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Custom AndroidJUnitRunner para reemplazar la aplicación principal por [HiltTestApplication]
 * durante la ejecución de pruebas instrumentadas de Hilt.
 */
class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
