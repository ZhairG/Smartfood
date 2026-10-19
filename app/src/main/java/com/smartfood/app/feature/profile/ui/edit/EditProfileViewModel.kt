package com.smartfood.app.feature.profile.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.profile.domain.model.UserProfile
import com.smartfood.app.feature.profile.domain.usecase.ObserveProfileUseCase
import com.smartfood.app.feature.profile.domain.usecase.ProfileEdits
import com.smartfood.app.feature.profile.domain.usecase.RequestEmailChangeUseCase
import com.smartfood.app.feature.profile.domain.usecase.UpdateProfileUseCase
import com.smartfood.app.feature.profile.domain.usecase.ValidateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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

/**
 * Nombre: EditProfileViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de "Editar información" (spec 008, HU-PER-02, T007). Carga el perfil una vez, lleva los
 * cambios, pide confirmar y guarda: si el correo cambió, primero valida los datos y pide el enlace de verificación con
 * la contraseña actual; después sube la foto (si hay) y guarda el perfil. Las validaciones viven en los casos de uso.
 *
 * @param observeProfile Perfil del usuario con sesión.
 * @param validateProfile Valida los datos antes de pedir el cambio de correo.
 * @param updateProfile Sube la foto y guarda el perfil.
 * @param requestEmailChange Confirma la contraseña y envía el enlace al correo nuevo.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val observeProfile: ObserveProfileUseCase,
    private val validateProfile: ValidateProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val requestEmailChange: RequestEmailChangeUseCase,
) : ViewModel() {

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
                if (newEmail != null) {
                    // Primero los datos: así no se envía el enlace si después el guardado no puede seguir.
                    validateProfile(state.fullName, state.phone, state.birthDate).getOrThrow()
                    requestEmailChange(newEmail, state.currentPassword).getOrThrow()
                }
                val edits = ProfileEdits(state.fullName, state.phone, state.birthDate, state.newPhotoPath)
                updateProfile(original, edits).getOrThrow()
            }
            result
                .onSuccess { saved ->
                    // El correo de la cuenta no cambia hasta abrir el enlace: el campo vuelve al guardado.
                    _uiState.update { it.loaded(saved).copy(dialog = EditProfileDialog.Saved(newEmail)) }
                    delay(if (newEmail != null) EMAIL_RESULT_MILLIS else RESULT_DIALOG_MILLIS)
                    _uiState.update { if (it.dialog is EditProfileDialog.Saved) it.copy(dialog = null) else it }
                }
                .onFailure { error -> _uiState.update { it.copy(isSaving = false, error = error.toAuthError()) } }
        }
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

        /** Con correo nuevo, el aviso tiene más texto: se deja más tiempo para leerlo (CA-PER-02-10). */
        const val EMAIL_RESULT_MILLIS = 4_000L

        /** "3001234567" → "+57 300 123 4567", el formato del Figma (CA-PER-02-06). */
        fun formatPhone(phone: String): String =
            if (phone.length == 10) "+57 ${phone.substring(0, 3)} ${phone.substring(3, 6)} ${phone.substring(6)}"
            else phone
    }
}
