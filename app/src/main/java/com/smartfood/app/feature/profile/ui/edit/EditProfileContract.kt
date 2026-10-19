package com.smartfood.app.feature.profile.ui.edit

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.profile.domain.model.UserProfile
import com.smartfood.app.feature.profile.domain.usecase.ValidateProfileUseCase
import java.time.LocalDate

/**
 * Nombre: EditProfileContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de "Editar información" (spec 008, HU-PER-02, Figma `361:649`): estado, eventos y efectos.
 * Los campos en rojo se **derivan** de [EditProfileUiState.error], como en autenticación.
 */

/** Campo del formulario que puede marcarse en rojo. */
enum class EditProfileField { FULL_NAME, EMAIL, PHONE, BIRTH_DATE, CURRENT_PASSWORD }

/** Hoja, calendario o aviso abierto sobre la pantalla. */
sealed interface EditProfileDialog {
    /** Hoja "Agregar foto" con "Galería" y "Cámara" (CA-PER-02-08). */
    data object PhotoSourceSheet : EditProfileDialog

    /** Calendario de la fecha de nacimiento (CA-PER-02-07). */
    data object BirthDatePicker : EditProfileDialog

    /** "¿Desea guardar los cambios?" (Figma `369:1195`, CA-PER-02-03). */
    data object ConfirmSave : EditProfileDialog

    /**
     * "Guardado" (Figma `369:905`, CA-PER-02-04), que se cierra solo.
     *
     * @property emailSentTo Correo nuevo al que se envió el enlace de verificación, o `null` si el correo no cambió
     *   (CA-PER-02-10).
     */
    data class Saved(val emailSentTo: String? = null) : EditProfileDialog
}

/**
 * Estado de "Editar información".
 *
 * @property isLoading `true` hasta leer el perfil.
 * @property original Perfil guardado, contra el que se comparan los cambios.
 * @property fullName Nombre tal como se escribió.
 * @property email Correo tal como se escribió.
 * @property phone Teléfono en texto libre.
 * @property birthDate Fecha de nacimiento elegida.
 * @property newPhotoPath Foto nueva en el teléfono (`cacheDir/profile`), todavía sin subir.
 * @property currentPassword Contraseña actual; se pide solo si el correo cambió (CA-PER-02-10).
 * @property isSaving `true` mientras se guarda: se ve el diálogo de carga.
 * @property error Último error, de validación o del guardado.
 * @property dialog Hoja, calendario o aviso abierto.
 */
data class EditProfileUiState(
    val isLoading: Boolean = true,
    val original: UserProfile? = null,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val birthDate: LocalDate? = null,
    val newPhotoPath: String? = null,
    val currentPassword: String = "",
    val isSaving: Boolean = false,
    val error: AuthError? = null,
    val dialog: EditProfileDialog? = null,
) {
    /** Foto guardada en la nube, si no hay una nueva. */
    val photoUrl: String?
        get() = original?.photoUrl

    /** Iniciales del círculo cuando no hay foto ("Pepito Perez" → "PP"). */
    val initials: String
        get() = fullName.ifBlank { original?.fullName.orEmpty() }
            .split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

    /** `true` si el correo escrito es otro: entonces se pide la contraseña actual (CA-PER-02-10). */
    val isEmailChanged: Boolean
        get() = original != null && !email.trim().equals(original.email, ignoreCase = true)

    /** `true` si algún dato difiere del perfil guardado (CA-PER-02-02). */
    val hasChanges: Boolean
        get() {
            val saved = original ?: return false
            return fullName != saved.fullName ||
                isEmailChanged ||
                ValidateProfileUseCase.normalizePhone(phone) != saved.phone ||
                birthDate != saved.birthDate ||
                newPhotoPath != null
        }

    /** "Guardar" se habilita al cambiar algún dato (CA-PER-02-02). */
    val isSaveEnabled: Boolean
        get() = hasChanges && !isSaving && !isLoading

    /** Campo marcado en rojo, si el error es de un campo. */
    val errorField: EditProfileField?
        get() = when (val e = error) {
            AuthError.InvalidName -> EditProfileField.FULL_NAME
            AuthError.InvalidPhone -> EditProfileField.PHONE
            AuthError.InvalidBirthDate -> EditProfileField.BIRTH_DATE
            AuthError.InvalidEmail, AuthError.EmailAlreadyInUse -> EditProfileField.EMAIL
            AuthError.InvalidCredentials -> EditProfileField.CURRENT_PASSWORD
            is AuthError.EmptyFields -> if (AuthField.PASSWORD in e.fields) EditProfileField.CURRENT_PASSWORD else null
            else -> null
        }
}

/** Lo que el usuario hace en "Editar información". */
sealed interface EditProfileEvent {
    data class FullNameChanged(val value: String) : EditProfileEvent
    data class EmailChanged(val value: String) : EditProfileEvent
    data class PhoneChanged(val value: String) : EditProfileEvent
    data class CurrentPasswordChanged(val value: String) : EditProfileEvent
    data object BirthDateClicked : EditProfileEvent
    data class BirthDatePicked(val date: LocalDate) : EditProfileEvent
    data object PhotoClicked : EditProfileEvent
    data object GalleryChosen : EditProfileEvent
    data object CameraChosen : EditProfileEvent
    data class PhotoPicked(val path: String) : EditProfileEvent
    data object SaveClicked : EditProfileEvent
    data object SaveConfirmed : EditProfileEvent
    data object DialogDismissed : EditProfileEvent
    data object BackClicked : EditProfileEvent
}

/** Efectos de un solo uso. */
sealed interface EditProfileEffect {
    /** Abre la cámara del sistema (CA-PER-02-08). */
    data object OpenCamera : EditProfileEffect

    /** Abre el selector de fotos del teléfono (CA-PER-02-08). */
    data object OpenGallery : EditProfileEffect

    /** Borra del teléfono una foto que ya no se va a guardar (descartada o reemplazada). */
    data class DeleteLocalPhoto(val path: String) : EditProfileEffect

    /** Vuelve al Perfil. */
    data object NavigateBack : EditProfileEffect
}
