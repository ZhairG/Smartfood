package com.smartfood.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.usecase.ResolveSessionUseCase
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Nombre: SplashViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Resuelve el destino inicial de la app según la sesión guardada (HU-03) y, cuando hay sesión activa,
 * reprograma el aviso diario de vencimiento (CA-INV-08-01, spec 006, plan §6). El tiempo mínimo del splash
 * (CA-03-04) lo controla [SplashScreen], no este ViewModel.
 *
 * @param resolveSession Caso de uso que decide si se abre directamente Home.
 * @param expiryReminderScheduler Programa el aviso de vencimiento cuando la sesión sigue activa.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val resolveSession: ResolveSessionUseCase,
    private val expiryReminderScheduler: ExpiryReminderScheduler
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)

    /** Destino resuelto, o `null` mientras se consulta la sesión. */
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val hasSession = resolveSession()
            // Sin sesión no hay nada que avisar: el aviso solo se reprograma con sesión activa (CA-INV-08-06).
            if (hasSession) expiryReminderScheduler.schedule()
            _destination.value = if (hasSession) SplashDestination.HOME else SplashDestination.LOGIN
        }
    }
}

/** Destinos posibles al terminar el splash. */
enum class SplashDestination { HOME, LOGIN }
