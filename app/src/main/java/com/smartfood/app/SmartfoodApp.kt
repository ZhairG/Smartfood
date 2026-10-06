package com.smartfood.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Nombre: SmartfoodApp.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Clase [Application] principal anotada con [HiltAndroidApp] para inicializar el grafo de Hilt.
 */
@HiltAndroidApp
class SmartfoodApp : Application()
