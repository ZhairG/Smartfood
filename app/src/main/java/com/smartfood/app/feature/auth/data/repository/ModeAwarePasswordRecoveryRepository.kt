package com.smartfood.app.feature.auth.data.repository

import com.smartfood.app.core.backend.BackendMode
import com.smartfood.app.core.backend.BackendModeProvider
import com.smartfood.app.feature.auth.domain.repository.PasswordRecoveryRepository
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Nombre: ModeAwarePasswordRecoveryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Recuperación que se registra en Hilt (spec 012, RF-SPK-02): en cada llamada consulta el interruptor y
 * delega en la versión con código de Cloud Functions (modo Blaze, spec 003) o en la del enlace (modo gratuito).
 * @param mode interruptor del backend.
 * @param blaze recuperación con código (spec 003).
 * @param free recuperación con enlace (spec 012).
 */
class ModeAwarePasswordRecoveryRepository(
    private val mode: BackendModeProvider,
    private val blaze: PasswordRecoveryRepository,
    private val free: PasswordRecoveryRepository
) : PasswordRecoveryRepository {

    @Inject
    constructor(
        mode: BackendModeProvider,
        remote: RemotePasswordRecoveryRepository,
        link: LinkPasswordRecoveryRepository
    ) : this(mode, blaze = remote, free = link)

    private val active: PasswordRecoveryRepository
        get() = if (mode.current() == BackendMode.BLAZE) blaze else free

    override val usesCode: Boolean get() = active.usesCode

    override suspend fun requestCode(email: String): Result<Duration> = active.requestCode(email)

    override suspend fun verifyCode(email: String, code: String): Result<Unit> = active.verifyCode(email, code)

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> =
        active.resetPassword(email, code, newPassword)
}
