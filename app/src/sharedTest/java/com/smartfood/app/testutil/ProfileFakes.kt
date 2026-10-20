package com.smartfood.app.testutil

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.UserProfile
import com.smartfood.app.feature.profile.domain.repository.EmailChangeRepository
import com.smartfood.app.feature.profile.domain.repository.ProfilePhotoRepository
import com.smartfood.app.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: ProfileFakes.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementaciones fake de los contratos del Perfil (spec 008), compartidas por las pruebas unitarias y
 * las instrumentadas. Registran las llamadas y devuelven resultados configurables.
 */

/** Fake de [ProfileRepository]: el perfil vive en un `MutableStateFlow`, así cada cambio se emite. */
class FakeProfileRepository(initial: UserProfile? = defaultProfile()) : ProfileRepository {
    val profile = MutableStateFlow(initial)

    /** Si no es `null`, [updateProfile] devuelve este fallo sin cambiar nada. */
    var updateResult: Result<Unit>? = null
    val updates = mutableListOf<UserProfile>()
    val syncedEmails = mutableListOf<String>()

    /** Si no es `null`, [observeProfile] falla con este error (perfil ilegible, CA-05-02). */
    var observeError: AuthError? = null

    /** Preferencias guardadas; [getPreferences] las devuelve salvo que [getPreferencesResult] no sea `null`. */
    var preferences = FoodPreferences()

    /** Si no es `null`, [getPreferences] devuelve este resultado. */
    var getPreferencesResult: Result<FoodPreferences>? = null

    /** Si no es `null`, [savePreferences] devuelve este resultado sin cambiar nada. */
    var savePreferencesResult: Result<Unit>? = null
    val savedPreferences = mutableListOf<FoodPreferences>()

    override fun observeProfile(): Flow<UserProfile> =
        observeError?.let { error -> flow { throw AuthException(error) } } ?: profile.filterNotNull()

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        updateResult?.let { return it }
        updates += profile
        this.profile.value = profile.copy(email = this.profile.value?.email ?: profile.email)
        return Result.success(Unit)
    }

    override suspend fun syncEmail(email: String): Result<Unit> {
        syncedEmails += email
        profile.value = profile.value?.copy(email = email)
        return Result.success(Unit)
    }

    override suspend fun getPreferences(): Result<FoodPreferences> {
        getPreferencesResult?.let { return it }
        return Result.success(preferences)
    }

    override suspend fun savePreferences(preferences: FoodPreferences): Result<Unit> {
        savePreferencesResult?.let { return it }
        this.preferences = preferences
        savedPreferences += preferences
        return Result.success(Unit)
    }

    companion object {
        fun defaultProfile() = UserProfile(
            uid = FakeAuthRepository.DEFAULT_UID,
            fullName = "Ana Pérez",
            email = FakeAuthRepository.DEFAULT_EMAIL,
        )
    }
}

/** Fake de [ProfilePhotoRepository]: cada subida devuelve una ruta única y registra los borrados. */
class FakeProfilePhotoRepository : ProfilePhotoRepository {
    /** Si no es `null`, [upload] devuelve este resultado. */
    var uploadResult: Result<Pair<String, String>>? = null
    val uploads = mutableListOf<String>()
    val deletes = mutableListOf<String>()
    private var count = 0

    override suspend fun upload(localPath: String): Result<Pair<String, String>> {
        uploadResult?.let { return it }
        uploads += localPath
        count++
        val path = "users/${FakeAuthRepository.DEFAULT_UID}/profile/foto-$count.jpg"
        return Result.success(path to "https://fotos.test/$path")
    }

    override suspend fun delete(path: String): Result<Unit> {
        deletes += path
        return Result.success(Unit)
    }
}

/** Fake de [EmailChangeRepository]: registra los correos pedidos y los códigos confirmados. */
class FakeEmailChangeRepository : EmailChangeRepository {
    var requestResult: Result<Duration> = Result.success(60.seconds)
    /** Si no es `null`, [confirm] devuelve este fallo. */
    var confirmError: Result<String>? = null
    val requested = mutableListOf<String>()
    val confirmedCodes = mutableListOf<String>()

    override suspend fun requestCode(newEmail: String): Result<Duration> {
        requested += newEmail
        return requestResult
    }

    override suspend fun confirm(code: String): Result<String> {
        confirmError?.let { return it }
        confirmedCodes += code
        return Result.success(requested.last())
    }
}
