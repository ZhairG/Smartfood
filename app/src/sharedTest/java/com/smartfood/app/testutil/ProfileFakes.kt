package com.smartfood.app.testutil

import com.smartfood.app.feature.profile.domain.model.UserProfile
import com.smartfood.app.feature.profile.domain.repository.ProfilePhotoRepository
import com.smartfood.app.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

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

    override fun observeProfile(): Flow<UserProfile> = profile.filterNotNull()

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
