package com.smartfood.app.feature.profile.data.repository

import com.smartfood.app.core.backend.BackendMode
import com.smartfood.app.core.backend.BackendModeProvider
import com.smartfood.app.feature.profile.domain.repository.EmailChangeRepository
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Nombre: ModeAwareEmailChangeRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Cambio de correo que se registra en Hilt (spec 012, RF-SPK-02): delega en la versión con código de
 * Cloud Functions (modo Blaze, spec 008) o en la del enlace de Firebase (modo gratuito).
 * @param mode interruptor del backend.
 * @param blaze cambio con código (spec 008).
 * @param free cambio con enlace (spec 012).
 */
class ModeAwareEmailChangeRepository(
    private val mode: BackendModeProvider,
    private val blaze: EmailChangeRepository,
    private val free: EmailChangeRepository
) : EmailChangeRepository {

    @Inject
    constructor(
        mode: BackendModeProvider,
        remote: RemoteEmailChangeRepository,
        link: LinkEmailChangeRepository
    ) : this(mode, blaze = remote, free = link)

    private val active: EmailChangeRepository
        get() = if (mode.current() == BackendMode.BLAZE) blaze else free

    override val usesCode: Boolean get() = active.usesCode

    override suspend fun requestCode(newEmail: String): Result<Duration> = active.requestCode(newEmail)

    override suspend fun confirm(code: String): Result<String> = active.confirm(code)
}
