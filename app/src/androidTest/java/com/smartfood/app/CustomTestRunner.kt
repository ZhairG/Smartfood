package com.smartfood.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Nombre: CustomTestRunner.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Custom AndroidJUnitRunner para reemplazar la aplicación principal por [HiltTestApplication]
 * durante la ejecución de pruebas instrumentadas de Hilt. Además inicializa un WorkManager de prueba
 * (spec 006, #45): `HiltTestApplication` no es `Configuration.Provider` y el manifiesto quita el inicializador por
 * defecto, así que sin esto una prueba que encolara un trabajo fallaría o lo encolaría en el teléfono real.
 */
class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }

    override fun callApplicationOnCreate(app: Application) {
        super.callApplicationOnCreate(app)
        // Ejecuta los trabajos en el mismo hilo y nunca toca la cola real de WorkManager. Las pruebas de workers
        // pueden volver a inicializarlo con su propia fábrica (ExpiryReminderWorkerTest).
        WorkManagerTestInitHelper.initializeTestWorkManager(
            app,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )
    }
}
