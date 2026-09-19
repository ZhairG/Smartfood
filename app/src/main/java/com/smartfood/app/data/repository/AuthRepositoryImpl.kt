package com.smartfood.app.data.repository

import com.smartfood.app.data.source.FirebaseAuthDataSource
import com.smartfood.app.utils.safeRunCatching
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseAuthDataSource
) : AuthRepository {

    override fun isUserAuthenticated(): Boolean = dataSource.currentUser != null

    override suspend fun login(email: String, pass: String): Result<Unit> = safeRunCatching {
        dataSource.signInWithEmailAndPassword(email, pass)
    }

    override suspend fun register(email: String, pass: String): Result<Unit> = safeRunCatching {
        dataSource.createUserWithEmailAndPassword(email, pass)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = safeRunCatching {
        dataSource.sendPasswordResetEmail(email)
    }

    override fun logout() {
        dataSource.signOut()
    }
}
