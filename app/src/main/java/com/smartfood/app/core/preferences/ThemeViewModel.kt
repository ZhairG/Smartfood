package com.smartfood.app.core.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.core.designsystem.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Nombre: ThemeViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel del tema de la app (spec 008, T018). `MainActivity` lo recoge y se lo pasa a `SmartfoodTheme`,
 * que es el único que decide el esquema de color (spec 002, HU-UI-05). Mientras se lee el DataStore se usa el tema por
 * defecto, claro (CA-PER-06-03 y 06-04).
 *
 * @param preferences Tema guardado en este teléfono.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    preferences: ThemePreferences
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.Default)

    /** Modo de color vigente, que cambia al instante cuando el usuario elige otro (CA-PER-06-02). */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.themeMode.collect { mode -> _themeMode.value = mode }
        }
    }
}
