package com.smartfood.app.feature.auth.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.auth.domain.repository.SessionPreferences
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import javax.inject.Inject

/**
 * Nombre: LoginUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Caso de uso del inicio de sesión (HU-02). Valida los datos, inicia sesión y guarda
 * la preferencia "Recuérdame" (HU-03). Vuelve a programar el aviso diario, que se cancela al cerrar sesión
 * (spec 006, CA-INV-08-07).
 *
 * @param authRepository Repositorio de cuenta y sesión.
 * @param sessionPreferences Preferencias locales de sesión.
 * @param validateEmailUseCase Validador del formato de correo.
 * @param reminderScheduler Aviso diario de productos por vencer.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionPreferences: SessionPreferences,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val reminderScheduler: ExpiryReminderScheduler
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
            .onSuccess {
                sessionPreferences.setRememberMe(rememberMe)
                reminderScheduler.schedule()
            }
    }
}
