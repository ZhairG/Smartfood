package com.smartfood.app

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * <build>
 * <b>Nombre:</b> HiltTestActivity.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 28/09/2026<br>
 * <b>Descripción:</b> Activity de pruebas anotada con [@AndroidEntryPoint] para que las pruebas de Jetpack Compose<br>
 * que utilizan `hiltViewModel()` dispongan del contexto de inyección de dependencias de Hilt.<br>
 * </build>
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
