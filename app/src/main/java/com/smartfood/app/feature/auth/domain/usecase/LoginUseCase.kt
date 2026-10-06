package com.smartfood.app.feature.auth.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.auth.domain.repository.SessionPreferences
import javax.inject.Inject

/**
 * <build>
 * <b>Nombre:</b> LoginUseCase.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Caso de uso del inicio de sesión (HU-02). Valida los datos, inicia sesión y guarda la<br>
 * preferencia "Recuérdame" (HU-03).<br>
 * </build>
 *
 * @param authRepository Repositorio de cuenta y sesión.
 * @param sessionPreferences Preferencias locales de sesión.
 * @param validateEmailUseCase Validador del formato de correo.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionPreferences: SessionPreferences,
    private val validateEmailUseCase: ValidateEmailUseCase
) {
    /**
     * @param email Correo ingresado.
     * @param password Contraseña ingresada.
     * @param rememberMe Valor de la casilla "Recuérdame".
     * @return Éxito, o un fallo con [AuthError.EmptyFields], [AuthError.InvalidEmail] o el error del repositorio.
     */
    suspend operator fun invoke(email: String, password: String, rememberMe: Boolean): Result<Unit> {
        val trimmedEmail = email.trim()

        val emptyFields = buildSet {
            if (trimmedEmail.isBlank()) add(AuthField.EMAIL)
            if (password.isBlank()) add(AuthField.PASSWORD)
        }
        if (emptyFields.isNotEmpty()) return authFailure(AuthError.EmptyFields(emptyFields))
        if (!validateEmailUseCase.execute(trimmedEmail)) return authFailure(AuthError.InvalidEmail)

        return authRepository.login(trimmedEmail, password)
            .onSuccess { sessionPreferences.setRememberMe(rememberMe) }
    }
}
