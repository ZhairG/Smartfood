package com.smartfood.app.feature.auth.domain.usecase

import com.smartfood.app.core.backend.BackendMode
import com.smartfood.app.core.backend.GetBackendModeUseCase
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.User
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.auth.domain.repository.UserRepository
import com.smartfood.app.feature.legal.domain.model.LegalVersion
import javax.inject.Inject

/**
 * Nombre: RegisterUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Caso de uso del registro (HU-01). Valida los datos y orquesta la creación de la cuenta,
 * el guardado del perfil con la constancia de aceptación de los documentos legales (CA-LEG-02-01; con rollback si
 * falla, CA-01-09) y el cierre de sesión final (CA-01-07).
 * El correo de bienvenida con la verificación lo envía el servidor al crearse el perfil (spec 004, RF-MAIL-03). En el
 * modo gratuito no hay servidor: se pide el correo de verificación estándar de Firebase (spec 012, CA-SPK-04-01).
 *
 * @param authRepository Repositorio de cuenta y sesión.
 * @param userRepository Repositorio del perfil del usuario.
 * @param validateEmailUseCase Validador del formato de correo.
 * @param validatePasswordUseCase Validador de la política de contraseña.
 * @param getBackendMode Modo del backend (spec 012).
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val getBackendMode: GetBackendModeUseCase
) {
    /**
     * @param fullName Nombre completo.
     * @param email Correo electrónico.
     * @param password Contraseña elegida.
     * @param acceptedTerms Indica si se aceptaron los Términos y condiciones.
     * @return Éxito, o un fallo con el [AuthError] de la primera regla incumplida o del repositorio.
     */
    suspend operator fun invoke(
        fullName: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Result<Unit> {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()

        val emptyFields = buildSet {
            if (trimmedName.isBlank()) add(AuthField.FULL_NAME)
            if (trimmedEmail.isBlank()) add(AuthField.EMAIL)
            if (password.isBlank()) add(AuthField.PASSWORD)
        }
        if (emptyFields.isNotEmpty()) return authFailure(AuthError.EmptyFields(emptyFields))
        if (!validateEmailUseCase.execute(trimmedEmail)) return authFailure(AuthError.InvalidEmail)
        if (!validatePasswordUseCase.execute(password).isValid) return authFailure(AuthError.WeakPassword)
        if (!acceptedTerms) return authFailure(AuthError.TermsNotAccepted)

        val uid = authRepository.createAccount(trimmedEmail, password)
            .getOrElse { return Result.failure(it) }

        userRepository.createProfile(
            user = User(uid = uid, fullName = trimmedName, email = trimmedEmail),
            acceptedTermsVersion = LegalVersion.CURRENT
        )
            .onFailure { error ->
                authRepository.deleteCurrentAccount()
                authRepository.logout()
                return Result.failure(error)
            }

        // Sin correo de bienvenida, Firebase envía la verificación; si falla, la cuenta ya quedó creada
        // (CA-SPK-04-02, como CA-MAIL-01-03).
        if (getBackendMode() == BackendMode.FREE) authRepository.sendEmailVerification()

        authRepository.logout()
        return Result.success(Unit)
    }
}
