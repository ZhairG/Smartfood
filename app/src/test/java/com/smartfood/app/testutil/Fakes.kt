package com.smartfood.app.testutil

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.User
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.auth.domain.repository.PasswordRecoveryRepository
import com.smartfood.app.feature.auth.domain.repository.SessionPreferences
import com.smartfood.app.feature.auth.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: Fakes.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementaciones fake de los contratos del dominio, compartidas por todas las pruebas
 * unitarias. Registran las llamadas recibidas y devuelven resultados configurables.
 */

/** Fake de [AuthRepository]. Por defecto todas las operaciones tienen éxito. */
class FakeAuthRepository : AuthRepository {
    var signedInUserId: String? = null

    var loginResult: Result<Unit> = Result.success(Unit)
    /** Latencia simulada del inicio de sesión, para probar el estado de carga. */
    var loginDelayMs: Long = 0
    var createAccountResult: Result<String> = Result.success(DEFAULT_UID)

    var loginCalls = 0
    var createAccountCalls = 0
    var deleteAccountCalls = 0
    var logoutCalls = 0

    var lastEmail: String? = null
    var lastPassword: String? = null

    override fun currentUserId(): String? = signedInUserId

    override suspend fun login(email: String, password: String): Result<Unit> {
        loginCalls++
        lastEmail = email
        lastPassword = password
        if (loginDelayMs > 0) delay(loginDelayMs)
        return loginResult.onSuccess { signedInUserId = DEFAULT_UID }
    }

    override suspend fun createAccount(email: String, password: String): Result<String> {
        createAccountCalls++
        lastEmail = email
        lastPassword = password
        return createAccountResult.onSuccess { signedInUserId = it }
    }

    override suspend fun deleteCurrentAccount(): Result<Unit> {
        deleteAccountCalls++
        return Result.success(Unit)
    }

    override fun logout() {
        logoutCalls++
        signedInUserId = null
    }

    companion object {
        const val DEFAULT_UID = "uid-123"
    }
}

/** Fake de [UserRepository] que guarda los perfiles en memoria. */
class FakeUserRepository : UserRepository {
    val profiles = mutableMapOf<String, User>()
    /** Versión de los documentos legales aceptada por cada perfil creado. */
    val acceptedTermsVersions = mutableMapOf<String, String>()
    var createProfileResult: Result<Unit>? = null
    var getProfileError: AuthError? = null

    override suspend fun createProfile(user: User, acceptedTermsVersion: String): Result<Unit> {
        createProfileResult?.let { return it }
        profiles[user.uid] = user
        acceptedTermsVersions[user.uid] = acceptedTermsVersion
        return Result.success(Unit)
    }

    override suspend fun getProfile(uid: String): Result<User> {
        getProfileError?.let { return authFailure(it) }
        return profiles[uid]?.let { Result.success(it) } ?: authFailure(AuthError.AccountNotFound)
    }
}

/** Fake de [SessionPreferences] en memoria. */
class FakeSessionPreferences(var rememberMe: Boolean = true) : SessionPreferences {
    override suspend fun isRememberMeEnabled(): Boolean = rememberMe
    override suspend fun setRememberMe(enabled: Boolean) {
        rememberMe = enabled
    }
}

/** Fake de [PasswordRecoveryRepository] con resultados configurables por paso. */
class FakePasswordRecoveryRepository : PasswordRecoveryRepository {
    var requestCodeResult: Result<Duration> = Result.success(60.seconds)
    var verifyCodeResult: Result<Unit> = Result.success(Unit)
    var resetPasswordResult: Result<Unit> = Result.success(Unit)

    var requestCodeCalls = 0
    var verifyCodeCalls = 0
    var resetPasswordCalls = 0

    override suspend fun requestCode(email: String): Result<Duration> {
        requestCodeCalls++
        return requestCodeResult
    }

    override suspend fun verifyCode(email: String, code: String): Result<Unit> {
        verifyCodeCalls++
        return verifyCodeResult
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> {
        resetPasswordCalls++
        return resetPasswordResult
    }
}
