package com.smartfood.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * <build>
 * <b>Nombre:</b> SmartfoodApp.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 19/09/2026<br>
 * <b>Descripción:</b> Clase [Application] principal anotada con [HiltAndroidApp] para inicializar el grafo de Hilt.<br>
 * </build>
 */
@HiltAndroidApp
class SmartfoodApp : Application()
