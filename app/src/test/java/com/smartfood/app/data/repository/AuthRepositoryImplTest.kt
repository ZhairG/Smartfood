package com.smartfood.app.data.repository

import com.smartfood.app.data.source.FirebaseAuthDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: AuthRepositoryImplTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas unitarias para la implementación del repositorio [AuthRepositoryImpl].
 */
class AuthRepositoryImplTest {

    private lateinit var fakeDataSource: FakeFirebaseAuthDataSource
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun prepararEntornoTest() {
        fakeDataSource = FakeFirebaseAuthDataSource()
        repository = AuthRepositoryImpl(fakeDataSource)
    }

    @Test
    fun `esUsuarioAutenticado retorna falso cuando el usuario actual es nulo`() {
        assertFalse(repository.isUserAuthenticated())
    }

    @Test
    fun `inicio de sesion exitoso retorna Result de exito`() = runTest {
        val result = repository.login("test@correo.com", "password123")
        assertTrue(result.isSuccess)
        assertEquals("test@correo.com", fakeDataSource.lastEmail)
    }

    @Test
    fun `fallo de inicio de sesion retorna Result de fallo de forma segura`() = runTest {
        fakeDataSource.shouldFail = true
        val result = repository.login("test@correo.com", "wrong")
        assertTrue(result.isFailure)
        assertEquals("Error simulado", result.exceptionOrNull()?.message)
    }
}

/**
 * Fake de [FirebaseAuthDataSource] para probar el comportamiento de [AuthRepositoryImpl] sin dependencias nativas.
 */
class FakeFirebaseAuthDataSource : FirebaseAuthDataSource(null) {
    var shouldFail = false
    var lastEmail: String? = null
    var lastPass: String? = null

    override val currentUser get() = null

    override suspend fun signInWithEmailAndPassword(email: String, pass: String) {
        if (shouldFail) throw RuntimeException("Error simulado")
        lastEmail = email
        lastPass = pass
    }

    override suspend fun createUserWithEmailAndPassword(email: String, pass: String) {
        if (shouldFail) throw RuntimeException("Error simulado")
        lastEmail = email
        lastPass = pass
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        if (shouldFail) throw RuntimeException("Error simulado")
        lastEmail = email
    }

    override fun signOut() {
        // No-op para pruebas
    }
}
