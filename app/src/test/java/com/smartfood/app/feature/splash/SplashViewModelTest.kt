package com.smartfood.app.feature.splash

import com.smartfood.app.feature.auth.domain.usecase.ResolveSessionUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeSessionPreferences
import com.smartfood.app.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SplashViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [SplashViewModel] (HU-03).
 */
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val sessionPreferences = FakeSessionPreferences()

    private fun crearViewModel() = SplashViewModel(ResolveSessionUseCase(authRepository, sessionPreferences))

    @Test
    fun `CA-03-01 con sesion recordada el destino es Home`() {
        authRepository.signedInUserId = FakeAuthRepository.DEFAULT_UID

        assertEquals(SplashDestination.HOME, crearViewModel().destination.value)
    }

    @Test
    fun `CA-03-02 con sesion no recordada el destino es Login`() {
        authRepository.signedInUserId = FakeAuthRepository.DEFAULT_UID
        sessionPreferences.rememberMe = false

        assertEquals(SplashDestination.LOGIN, crearViewModel().destination.value)
    }

    @Test
    fun `CA-03-03 sin sesion el destino es Login`() {
        assertEquals(SplashDestination.LOGIN, crearViewModel().destination.value)
    }
}
