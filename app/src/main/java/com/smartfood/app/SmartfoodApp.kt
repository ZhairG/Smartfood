package com.smartfood.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.smartfood.app.core.backend.BackendModeProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Nombre: SmartfoodApp.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Clase [Application] principal anotada con [HiltAndroidApp] para inicializar el grafo de Hilt.
 * Además, implementa [Configuration.Provider] para que WorkManager use [HiltWorkerFactory] y pueda crear los
 * trabajadores con `@HiltWorker`, como el aviso de vencimiento (spec 006, CA-INV-08-01, T038). Por eso el
 * manifiesto quita el inicializador por defecto de WorkManager. Al iniciar, pide el interruptor del backend de
 * Remote Config (spec 012, CA-SPK-01-03).
 */
@HiltAndroidApp
class SmartfoodApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var backendModeProvider: BackendModeProvider

    override fun onCreate() {
        super.onCreate()
        backendModeProvider.refresh()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
