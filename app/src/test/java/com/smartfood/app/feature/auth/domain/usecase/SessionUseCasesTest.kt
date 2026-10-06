package com.smartfood.app.feature.auth.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.User
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeAuthRepository.Companion.DEFAULT_UID
import com.smartfood.app.testutil.FakeSessionPreferences
import com.smartfood.app.testutil.FakeUserRepository
import com.smartfood.app.testutil.authError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> SessionUseCasesTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pruebas unitarias de [ResolveSessionUseCase], [GetCurrentUserUseCase] y [LogoutUseCase]<br>
 * (HU-03 y HU-05).<br>
 * </build>
 */
class SessionUseCasesTest {

    private val authRepository = FakeAuthRepository()
    private val sessionPreferences = FakeSessionPreferences()
    private val userRepository = FakeUserRepository()
    private val resolveSession = ResolveSessionUseCase(authRepository, sessionPreferences)

    @Test
    fun `CA-03-01 con sesion y Recuerdame activo abre Home`() = runTest {
        authRepository.signedInUserId = DEFAULT_UID
        sessionPreferences.rememberMe = true

        assertTrue(resolveSession())
        assertEquals(0, authRepository.logoutCalls)
    }

    @Test
    fun `CA-03-02 con sesion y Recuerdame inactivo cierra la sesion y abre Login`() = runTest {
        authRepository.signedInUserId = DEFAULT_UID
        sessionPreferences.rememberMe = false

        assertFalse(resolveSession())
        assertNull(authRepository.currentUserId())
    }

    @Test
    fun `CA-03-03 sin sesion abre Login`() = runTest {
        assertFalse(resolveSession())
    }

    @Test
    fun `CA-05-01 obtiene el perfil del usuario con sesion`() = runTest {
        val user = User(DEFAULT_UID, "Pepito Perez", "pepito@correo.com")
        userRepository.profiles[DEFAULT_UID] = user
        authRepository.signedInUserId = DEFAULT_UID

        val result = GetCurrentUserUseCase(authRepository, userRepository)()

        assertEquals(user, result.getOrNull())
    }

    @Test
    fun `sin sesion obtener el perfil falla`() = runTest {
        val result = GetCurrentUserUseCase(authRepository, userRepository)()

        assertEquals(AuthError.InvalidCredentials, result.authError)
    }

    @Test
    fun `CA-05-03 cerrar sesion delega en el repositorio`() {
        authRepository.signedInUserId = DEFAULT_UID

        LogoutUseCase(authRepository)()

        assertNull(authRepository.currentUserId())
    }
}
