package com.smartfood.app

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity de pruebas anotada con [@AndroidEntryPoint] para que las pruebas de Jetpack Compose
 * que utilizan `hiltViewModel()` dispongan del contexto de inyección de dependencias de Hilt.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
