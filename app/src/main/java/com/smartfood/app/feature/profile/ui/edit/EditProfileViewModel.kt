package com.smartfood.app.feature.profile.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.profile.domain.model.UserProfile
import com.smartfood.app.feature.profile.domain.usecase.ConfirmEmailChangeUseCase
import com.smartfood.app.feature.profile.domain.usecase.ObserveProfileUseCase
import com.smartfood.app.feature.profile.domain.usecase.ProfileEdits
import com.smartfood.app.feature.profile.domain.usecase.RequestEmailChangeUseCase
import com.smartfood.app.feature.profile.domain.usecase.UpdateProfileUseCase
import com.smartfood.app.feature.profile.domain.usecase.ValidateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: EditProfileViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de "Editar información" (spec 008, HU-PER-02, T007). Carga el perfil una vez, lleva los
 * cambios, pide confirmar y guarda: valida los datos, guarda el perfil (con la foto, si hay) y, si el correo cambió,
 * confirma la contraseña y pide el código del correo nuevo, que se escribe en "Revisa tu correo" (spec 008, T009b). Al
 * confirmarlo, la cuenta y el perfil cambian sin cerrar la sesión. Las validaciones viven en los casos de uso.
 *
 * @param observeProfile Perfil del usuario con sesión.
 * @param validateProfile Valida los datos antes de pedir el código, para no enviarlo si el guardado no puede seguir.
 * @param updateProfile Sube la foto y guarda el perfil.
 * @param requestEmailChange Confirma la contraseña y envía el código al correo nuevo (también al reenviarlo).
 * @param confirmEmailChange Comprueba el código y pone al día la sesión con el correo nuevo.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val observeProfile: ObserveProfileUseCase,
    private val validateProfile: ValidateProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val requestEmailChange: RequestEmailChangeUseCase,
    private val confirmEmailChange: ConfirmEmailChangeUseCase,
) : ViewModel() {

    /** Cuenta regresiva para "Reenviar código". */
    private var cooldownJob: Job? = null

    private val _uiState = MutableStateFlow(EditProfileUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<EditProfileEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso, como abrir la cámara o volver. */
    val effects: Flow<EditProfileEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Solo la primera lectura llena el formulario: las siguientes no deben pisar lo que se está escribiendo.
            safeRunCatching { observeProfile().first() }
                .onSuccess { profile -> _uiState.update { it.loaded(profile) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.toAuthError()) } }
        }
    }

    /** Procesa un evento de la UI. */
    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.FullNameChanged -> edit { copy(fullName = event.value) }
            is EditProfileEvent.EmailChanged -> edit { copy(email = event.value) }
            is EditProfileEvent.PhoneChanged -> edit { copy(phone = event.value) }
            is EditProfileEvent.CurrentPasswordChanged -> edit { copy(currentPassword = event.value) }
            EditProfileEvent.BirthDateClicked -> _uiState.update { it.copy(dialog = EditProfileDialog.BirthDatePicker) }
            is EditProfileEvent.BirthDatePicked -> edit { copy(birthDate = event.date, dialog = null) }
            EditProfileEvent.PhotoClicked -> _uiState.update { it.copy(dialog = EditProfileDialog.PhotoSourceSheet) }
            EditProfileEvent.GalleryChosen -> closeDialogAndSend(EditProfileEffect.OpenGallery)
            EditProfileEvent.CameraChosen -> closeDialogAndSend(EditProfileEffect.OpenCamera)
            is EditProfileEvent.PhotoPicked -> onPhotoPicked(event.path)
            EditProfileEvent.SaveClicked -> if (_uiState.value.isSaveEnabled) {
                _uiState.update { it.copy(dialog = EditProfileDialog.ConfirmSave) }
            }
            EditProfileEvent.SaveConfirmed -> save()
            is EditProfileEvent.EmailCodeChanged ->
                edit { copy(emailCode = event.value.filter { it.isDigit() }.take(EMAIL_CODE_LENGTH)) }
            EditProfileEvent.ConfirmEmailCode -> confirmCode()
            EditProfileEvent.ResendEmailCode -> resendCode()
            // Cancelar deja el correo como estaba; los demás datos ya se guardaron.
            EditProfileEvent.CancelEmailCode -> {
                cooldownJob?.cancel()
                _uiState.update {
                    it.copy(
                        dialog = null,
                        email = it.original?.email.orEmpty(),
                        currentPassword = "",
                        emailCode = "",
                        resendCooldownSeconds = 0,
                        error = null
                    )
                }
            }
            EditProfileEvent.DialogDismissed -> _uiState.update { it.copy(dialog = null) }
            // Los cambios sin guardar se descartan sin preguntar (CA-PER-02-13).
            EditProfileEvent.BackClicked -> viewModelScope.launch {
                _uiState.value.newPhotoPath?.let { _effects.send(EditProfileEffect.DeleteLocalPhoto(it)) }
                _effects.send(EditProfileEffect.NavigateBack)
            }
        }
    }

    /** Cambia un dato y quita el error anterior. */
    private fun edit(change: EditProfileUiState.() -> EditProfileUiState) {
        _uiState.update { it.change().copy(error = null) }
    }

    private fun closeDialogAndSend(effect: EditProfileEffect) {
        _uiState.update { it.copy(dialog = null) }
        viewModelScope.launch { _effects.send(effect) }
    }

    /** La foto nueva reemplaza a la que se había elegido antes, que se borra del teléfono. */
    private fun onPhotoPicked(path: String) {
        val previous = _uiState.value.newPhotoPath
        edit { copy(newPhotoPath = path) }
        if (previous != null && previous != path) {
            viewModelScope.launch { _effects.send(EditProfileEffect.DeleteLocalPhoto(previous)) }
        }
    }

    private fun save() {
        val state = _uiState.value
        val original = state.original ?: return
        _uiState.update { it.copy(dialog = null, isSaving = true, error = null) }
        viewModelScope.launch {
            val newEmail = state.email.trim().takeIf { state.isEmailChanged }
            val result = safeRunCatching {
                // Primero los datos: así no se envía el código si después el guardado no puede seguir.
                validateProfile(state.fullName, state.phone, state.birthDate).getOrThrow()
                val wait = newEmail?.let { requestEmailChange(it, state.currentPassword).getOrThrow() }
                val edits = ProfileEdits(state.fullName, state.phone, state.birthDate, state.newPhotoPath)
                val saved = if (state.hasProfileChanges) updateProfile(original, edits).getOrThrow() else original
                saved to wait
            }
            result
                .onSuccess { (saved, wait) ->
                    if (newEmail == null || wait == null) {
                        _uiState.update { it.loaded(saved).copy(dialog = EditProfileDialog.Saved()) }
                        closeSavedLater()
                    } else {
                        // Los demás datos ya quedaron; el correo cambia al confirmar el código (CA-PER-02-10).
                        _uiState.update {
                            it.loaded(saved).copy(
                                email = newEmail,
                                currentPassword = state.currentPassword,
                                dialog = EditProfileDialog.EmailCode(newEmail)
                            )
                        }
                        startCooldown(wait.inWholeSeconds.toInt())
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(isSaving = false, error = error.toAuthError()) } }
        }
    }

    /** Comprueba el código: la cuenta y el perfil toman el correo nuevo sin cerrar la sesión (CA-PER-02-12). */
    private fun confirmCode() {
        val state = _uiState.value
        if (state.dialog !is EditProfileDialog.EmailCode || state.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            confirmEmailChange(state.emailCode, state.currentPassword)
                .onSuccess { email ->
                    cooldownJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            original = it.original?.copy(email = email),
                            email = email,
                            currentPassword = "",
                            emailCode = "",
                            resendCooldownSeconds = 0,
                            dialog = EditProfileDialog.Saved(emailChangedTo = email)
                        )
                    }
                    closeSavedLater()
                }
                .onFailure { error -> _uiState.update { it.copy(isSaving = false, error = error.toAuthError()) } }
        }
    }

    /** "Reenviar código": vuelve a confirmar la contraseña (el servidor exige un inicio de sesión reciente). */
    private fun resendCode() {
        val state = _uiState.value
        val dialog = state.dialog as? EditProfileDialog.EmailCode ?: return
        if (state.resendCooldownSeconds > 0 || state.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null, emailCode = "") }
        viewModelScope.launch {
            requestEmailChange(dialog.email, state.currentPassword)
                .onSuccess { wait ->
                    _uiState.update { it.copy(isSaving = false) }
                    startCooldown(wait.inWholeSeconds.toInt())
                }
                .onFailure { error -> _uiState.update { it.copy(isSaving = false, error = error.toAuthError()) } }
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        _uiState.update { it.copy(resendCooldownSeconds = seconds) }
        cooldownJob = viewModelScope.launch {
            while (_uiState.value.resendCooldownSeconds > 0) {
                delay(1.seconds)
                _uiState.update { it.copy(resendCooldownSeconds = (it.resendCooldownSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }

    /** "Guardado" se cierra solo (CA-PER-02-04). */
    private suspend fun closeSavedLater() {
        delay(RESULT_DIALOG_MILLIS)
        _uiState.update { if (it.dialog is EditProfileDialog.Saved) it.copy(dialog = null) else it }
    }

    /** Llena el formulario con el perfil guardado. */
    private fun EditProfileUiState.loaded(profile: UserProfile) = copy(
        isLoading = false,
        isSaving = false,
        original = profile,
        fullName = profile.fullName,
        email = profile.email,
        phone = profile.phone?.let(::formatPhone).orEmpty(),
        birthDate = profile.birthDate,
        newPhotoPath = null,
        currentPassword = "",
        error = null,
    )

    internal companion object {
        /** "Guardado" se cierra solo a los 2 s (CA-PER-02-04), como en el inventario. */
        const val RESULT_DIALOG_MILLIS = 2_000L

        /** Largo del código del correo nuevo; coincide con el del servidor. */
        const val EMAIL_CODE_LENGTH = 4

        /** "3001234567" → "+57 300 123 4567", el formato del Figma (CA-PER-02-06). */
        fun formatPhone(phone: String): String =
            if (phone.length == 10) "+57 ${phone.substring(0, 3)} ${phone.substring(3, 6)} ${phone.substring(6)}"
            else phone
    }
}
