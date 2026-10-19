package com.smartfood.app.feature.profile.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.auth.domain.usecase.ValidateEmailUseCase
import com.smartfood.app.feature.profile.domain.model.UserProfile
import com.smartfood.app.feature.profile.domain.repository.EmailChangeRepository
import com.smartfood.app.feature.profile.domain.repository.ProfilePhotoRepository
import com.smartfood.app.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Nombre: ProfileUseCases.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Casos de uso de "Editar información" (spec 008, HU-PER-02, T005): validar los datos, guardarlos con la
 * foto, cambiar el correo con un código y sincronizar el correo del perfil con el de la cuenta. Las
 * validaciones viven solo aquí (constitución §3); el ViewModel no las repite.
 */

/**
 * Datos que la persona escribió en "Editar información".
 *
 * @property fullName Nombre tal como se escribió.
 * @property phoneText Teléfono en texto libre ("+57 300 123 4567", "300-123-4567"…), o vacío.
 * @property birthDate Fecha elegida en el calendario, o `null`.
 * @property newPhotoPath Archivo del teléfono con la foto nueva, o `null` si no se cambió.
 */
data class ProfileEdits(
    val fullName: String,
    val phoneText: String,
    val birthDate: LocalDate?,
    val newPhotoPath: String? = null,
)

/** Perfil del usuario con sesión, en vivo. */
class ObserveProfileUseCase @Inject constructor(private val repository: ProfileRepository) {
    operator fun invoke(): Flow<UserProfile> = repository.observeProfile()
}

/**
 * Valida y normaliza los datos del perfil (CA-PER-02-05 a 02-07).
 *
 * @param clock Reloj del teléfono, para "no posterior a hoy".
 */
class ValidateProfileUseCase @Inject constructor(private val clock: Clock) {

    /** Datos válidos y normalizados. */
    data class Valid(val fullName: String, val phone: String?, val birthDate: LocalDate?)

    operator fun invoke(fullName: String, phoneText: String, birthDate: LocalDate?): Result<Valid> {
        val name = fullName.trim().replace(Regex("\\s+"), " ")
        if (name.length !in NAME_LENGTH) return authFailure(AuthError.InvalidName)
        val phone = normalizePhone(phoneText)
        if (phone != null && !PHONE.matches(phone)) return authFailure(AuthError.InvalidPhone)
        if (birthDate != null && birthDate.isAfter(LocalDate.now(clock))) return authFailure(AuthError.InvalidBirthDate)
        return Result.success(Valid(name, phone, birthDate))
    }

    companion object {
        private val NAME_LENGTH = 2..50
        private val PHONE = Regex("^3[0-9]{9}$")

        /**
         * Deja solo los dígitos y quita el indicativo 57 si viene delante ("+57 300 123 4567" → "3001234567"). Un texto
         * vacío es "sin teléfono".
         */
        fun normalizePhone(text: String): String? {
            val digits = text.filter { it.isDigit() }
            if (text.isBlank()) return null
            return if (digits.length == 12 && digits.startsWith("57")) digits.drop(2) else digits
        }
    }
}

/**
 * Guarda los datos del perfil y, si hay foto nueva, la sube primero (CA-PER-02-04, 02-09). Si guardar falla, borra
 * la foto recién subida; si sale bien, borra la anterior. Sin red, la foto no se sube y nada cambia.
 */
class UpdateProfileUseCase @Inject constructor(
    private val validate: ValidateProfileUseCase,
    private val repository: ProfileRepository,
    private val photos: ProfilePhotoRepository,
) {
    suspend operator fun invoke(current: UserProfile, edits: ProfileEdits): Result<UserProfile> {
        val valid = validate(edits.fullName, edits.phoneText, edits.birthDate).getOrElse { return Result.failure(it) }
        val uploaded = edits.newPhotoPath?.let { path -> photos.upload(path).getOrElse { return Result.failure(it) } }
        val updated = current.copy(
            fullName = valid.fullName,
            phone = valid.phone,
            birthDate = valid.birthDate,
            photoPath = uploaded?.first ?: current.photoPath,
            photoUrl = uploaded?.second ?: current.photoUrl,
        )
        repository.updateProfile(updated).onFailure { error ->
            uploaded?.let { photos.delete(it.first) }
            return Result.failure(error)
        }
        if (uploaded != null) current.photoPath?.let { photos.delete(it) }
        return Result.success(updated)
    }
}

/**
 * Pide el código para cambiar el correo (CA-PER-02-10, 02-11): valida el correo nuevo, confirma la contraseña actual
 * (el servidor exige un inicio de sesión reciente) y pide que se envíe el código al correo nuevo. El correo no cambia
 * hasta confirmar el código. Sirve también para "Reenviar código".
 *
 * @return Espera antes de poder pedir otro código.
 */
class RequestEmailChangeUseCase @Inject constructor(
    private val validateEmail: ValidateEmailUseCase,
    private val auth: AuthRepository,
    private val emailChange: EmailChangeRepository,
) {
    suspend operator fun invoke(newEmail: String, currentPassword: String): Result<Duration> {
        val email = newEmail.trim()
        if (currentPassword.isEmpty()) return authFailure(AuthError.EmptyFields(setOf(AuthField.PASSWORD)))
        if (!validateEmail.execute(email)) return authFailure(AuthError.InvalidEmail)
        auth.reauthenticate(currentPassword).onFailure { return Result.failure(it) }
        return emailChange.requestCode(email)
    }
}

/**
 * Confirma el código del correo nuevo (CA-PER-02-11, 02-12): el servidor cambia la cuenta y el perfil, y la sesión se
 * pone al día con el correo nuevo sin cerrarse.
 *
 * @return Correo nuevo de la cuenta.
 */
class ConfirmEmailChangeUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val emailChange: EmailChangeRepository,
) {
    suspend operator fun invoke(code: String, currentPassword: String): Result<String> {
        val digits = code.trim()
        if (digits.length != CODE_LENGTH || !digits.all { it.isDigit() }) return authFailure(AuthError.InvalidOtpCode)
        val email = emailChange.confirm(digits).getOrElse { return Result.failure(it) }
        return auth.refreshSession(email, currentPassword).map { email }
    }

    private companion object {
        const val CODE_LENGTH = 4
    }
}

/** Iguala el correo del perfil al de la cuenta cuando difieren, por ejemplo tras confirmar un correo nuevo (CA-PER-02-12). */
class SyncProfileEmailUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(profile: UserProfile): Result<Unit> {
        val accountEmail = auth.currentEmail() ?: return Result.success(Unit)
        if (accountEmail.equals(profile.email, ignoreCase = true)) return Result.success(Unit)
        return repository.syncEmail(accountEmail)
    }
}
