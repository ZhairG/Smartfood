package com.smartfood.app.feature.home

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.User
import com.smartfood.app.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.smartfood.app.feature.auth.domain.usecase.LogoutUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeAuthRepository.Companion.DEFAULT_UID
import com.smartfood.app.testutil.FakeExpiryReminderScheduler
import com.smartfood.app.testutil.FakeUserRepository
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: HomeViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [HomeViewModel] (HU-05).
 */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()

    @Before
    fun prepararEntornoTest() {
        authRepository.signedInUserId = DEFAULT_UID
    }

    private fun crearViewModel() = HomeViewModel(
        getCurrentUser = GetCurrentUserUseCase(authRepository, userRepository),
        logout = LogoutUseCase(authRepository, FakeExpiryReminderScheduler())
    )

    @Test
    fun `CA-05-01 saluda con el primer nombre del perfil`() {
        userRepository.profiles[DEFAULT_UID] = User(DEFAULT_UID, "Pepito Perez", "pepito@correo.com")

        val state = crearViewModel().uiState.value

        assertEquals("Pepito", state.firstName)
        assertFalse(state.isLoading)
    }

    @Test
    fun `CA-05-02 si el perfil no se puede leer usa saludo generico`() {
        userRepository.getProfileError = AuthError.Network

        val state = crearViewModel().uiState.value

        assertNull(state.firstName)
        assertFalse(state.isLoading)
    }

    @Test
    fun `CA-05-03 cerrar sesion termina la sesion y emite NavigateToLogin`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(HomeEvent.LogoutClicked)

        assertNull(authRepository.currentUserId())
        assertEquals(HomeEffect.NavigateToLogin, viewModel.effects.first())
    }
}
