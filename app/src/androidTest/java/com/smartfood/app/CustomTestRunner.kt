package com.smartfood.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * <build>
 * <b>Nombre:</b> CustomTestRunner.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 28/09/2026<br>
 * <b>Descripción:</b> Custom AndroidJUnitRunner para reemplazar la aplicación principal por [HiltTestApplication]<br>
 * durante la ejecución de pruebas instrumentadas de Hilt.<br>
 * </build>
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
