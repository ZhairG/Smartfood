package com.smartfood.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.usecase.ResolveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * <build>
 * <b>Nombre:</b> SplashViewModel.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Resuelve el destino inicial de la app según la sesión guardada (HU-03). El tiempo mínimo del<br>
 * splash (CA-03-04) lo controla [SplashScreen], no este ViewModel.<br>
 * </build>
 *
 * @param resolveSession Caso de uso que decide si se abre directamente Home.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val resolveSession: ResolveSessionUseCase
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)

    /** Destino resuelto, o `null` mientras se consulta la sesión. */
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            _destination.value = if (resolveSession()) SplashDestination.HOME else SplashDestination.LOGIN
        }
    }
}

/** Destinos posibles al terminar el splash. */
enum class SplashDestination { HOME, LOGIN }
