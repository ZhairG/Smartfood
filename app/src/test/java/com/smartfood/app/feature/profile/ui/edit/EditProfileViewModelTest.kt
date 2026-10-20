package com.smartfood.app.feature.profile.ui.edit

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.usecase.ValidateEmailUseCase
import com.smartfood.app.feature.profile.domain.usecase.ConfirmEmailChangeUseCase
import com.smartfood.app.feature.profile.domain.usecase.ObserveProfileUseCase
import com.smartfood.app.feature.profile.domain.usecase.RequestEmailChangeUseCase
import com.smartfood.app.feature.profile.domain.usecase.UpdateProfileUseCase
import com.smartfood.app.feature.profile.domain.usecase.ValidateProfileUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeEmailChangeRepository
import com.smartfood.app.testutil.FakeProfilePhotoRepository
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.fixedClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: EditProfileViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [EditProfileViewModel] (spec 008, HU-PER-02, T009): carga, "Guardar" habilitado
 * solo con cambios, aviso de confirmación, guardado con foto, cambio de correo con contraseña y salida sin guardar.
 */
class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 18)
    private val repository = FakeProfileRepository()
    private val photos = FakeProfilePhotoRepository()
    private val auth = FakeAuthRepository()
    private val emailChange = FakeEmailChangeRepository()

    private fun crearViewModel(): EditProfileViewModel {
        val validate = ValidateProfileUseCase(fixedClock(today))
        return EditProfileViewModel(
            observeProfile = ObserveProfileUseCase(repository),
            validateProfile = validate,
            updateProfile = UpdateProfileUseCase(validate, repository, photos),
            requestEmailChange = RequestEmailChangeUseCase(ValidateEmailUseCase(), auth, emailChange),
            confirmEmailChange = ConfirmEmailChangeUseCase(auth, emailChange)
        )
    }

    private val EditProfileViewModel.state get() = uiState.value

    @Test
    fun `CA-PER-02-01 carga los datos actuales con el telefono en el formato del Figma`() {
        repository.profile.value = FakeProfileRepository.defaultProfile().copy(phone = "3001234567")

        val state = crearViewModel().state

        assertFalse(state.isLoading)
        assertEquals("Ana Pérez", state.fullName)
        assertEquals(FakeAuthRepository.DEFAULT_EMAIL, state.email)
        assertEquals("+57 300 123 4567", state.phone)
        assertEquals("AP", state.initials)
    }

    @Test
    fun `CA-PER-02-02 Guardar se habilita solo cuando algo cambia`() {
        val viewModel = crearViewModel()
        assertFalse(viewModel.state.isSaveEnabled)

        viewModel.onEvent(EditProfileEvent.FullNameChanged("Ana María"))
        assertTrue(viewModel.state.isSaveEnabled)

        viewModel.onEvent(EditProfileEvent.FullNameChanged("Ana Pérez"))
        assertFalse("volver al dato guardado no cuenta como cambio", viewModel.state.isSaveEnabled)

        viewModel.onEvent(EditProfileEvent.PhotoPicked("/cache/profile/a.jpg"))
        assertTrue("una foto nueva es un cambio", viewModel.state.isSaveEnabled)
    }

    @Test
    fun `CA-PER-02-03 Guardar pide confirmar y Cancelar cierra sin guardar`() {
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.PhoneChanged("300 123 4567"))

        viewModel.onEvent(EditProfileEvent.SaveClicked)
        assertEquals(EditProfileDialog.ConfirmSave, viewModel.state.dialog)

        viewModel.onEvent(EditProfileEvent.DialogDismissed)
        assertNull(viewModel.state.dialog)
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun `CA-PER-02-04 confirmar guarda, muestra Guardado y lo cierra a los 2 segundos`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.FullNameChanged("Ana María"))
        viewModel.onEvent(EditProfileEvent.PhotoPicked("/cache/profile/a.jpg"))
        viewModel.onEvent(EditProfileEvent.BirthDatePicked(LocalDate.of(2000, 1, 31)))

        viewModel.onEvent(EditProfileEvent.SaveConfirmed)
        runCurrent()

        val guardado = repository.updates.single()
        assertEquals("Ana María", guardado.fullName)
        assertEquals("users/uid-123/profile/foto-1.jpg", guardado.photoPath)
        assertEquals(EditProfileDialog.Saved(), viewModel.state.dialog)
        assertFalse("sigue en la pantalla con los datos nuevos y sin cambios", viewModel.state.hasChanges)
        assertNull(viewModel.state.newPhotoPath)

        advanceTimeBy(EditProfileViewModel.RESULT_DIALOG_MILLIS + 1)
        assertNull(viewModel.state.dialog)
    }

    @Test
    fun `CA-PER-02-05 CA-PER-02-09 un dato invalido o sin red marca el error y no guarda`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.FullNameChanged("A"))
        viewModel.onEvent(EditProfileEvent.SaveConfirmed)
        runCurrent()
        assertEquals(EditProfileField.FULL_NAME, viewModel.state.errorField)
        assertFalse(viewModel.state.isSaving)

        viewModel.onEvent(EditProfileEvent.FullNameChanged("Ana María"))
        assertNull("escribir quita el error", viewModel.state.error)
        photos.uploadResult = authFailure(AuthError.Network)
        viewModel.onEvent(EditProfileEvent.PhotoPicked("/cache/profile/a.jpg"))
        viewModel.onEvent(EditProfileEvent.SaveConfirmed)
        runCurrent()

        assertEquals(AuthError.Network, viewModel.state.error)
        assertEquals("la foto nueva se conserva para reintentar", "/cache/profile/a.jpg", viewModel.state.newPhotoPath)
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun `CA-PER-02-10 cambiar el correo guarda lo demas y pide el codigo enviado al correo nuevo`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.EmailChanged("nuevo@correo.com"))
        viewModel.onEvent(EditProfileEvent.FullNameChanged("Ana María"))
        assertTrue("aparece el campo de contraseña", viewModel.state.isEmailChanged)

        viewModel.onEvent(EditProfileEvent.CurrentPasswordChanged("Clave#123"))
        viewModel.onEvent(EditProfileEvent.SaveConfirmed)
        runCurrent()

        assertEquals(listOf("nuevo@correo.com"), emailChange.requested)
        assertEquals("Clave#123", auth.lastReauthPassword)
        assertEquals("Ana María", repository.updates.single().fullName)
        assertEquals(EditProfileDialog.EmailCode("nuevo@correo.com"), viewModel.state.dialog)
        assertEquals(60, viewModel.state.resendCooldownSeconds)
        assertEquals("la cuenta aún no cambia", FakeAuthRepository.DEFAULT_EMAIL, viewModel.state.original?.email)
    }

    @Test
    fun `CA-PER-02-12 el codigo correcto cambia el correo al instante y muestra Guardado`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.EmailChanged("nuevo@correo.com"))
        viewModel.onEvent(EditProfileEvent.CurrentPasswordChanged("Clave#123"))
        viewModel.onEvent(EditProfileEvent.SaveConfirmed)
        runCurrent()

        viewModel.onEvent(EditProfileEvent.EmailCodeChanged("48a21"))
        assertEquals("solo dígitos y como mucho 4", "4821", viewModel.state.emailCode)
        viewModel.onEvent(EditProfileEvent.ConfirmEmailCode)
        runCurrent()

        assertEquals(listOf("4821"), emailChange.confirmedCodes)
        assertEquals("nuevo@correo.com", auth.lastRefreshedEmail)
        assertEquals(EditProfileDialog.Saved(emailChangedTo = "nuevo@correo.com"), viewModel.state.dialog)
        assertEquals("nuevo@correo.com", viewModel.state.original?.email)
        assertFalse("sin cambios pendientes", viewModel.state.hasChanges)

        advanceTimeBy(EditProfileViewModel.RESULT_DIALOG_MILLIS + 1)
        assertNull(viewModel.state.dialog)
    }

    @Test
    fun `CA-PER-02-11 un codigo incorrecto se marca en el aviso y Cancelar deja el correo como estaba`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.EmailChanged("nuevo@correo.com"))
        viewModel.onEvent(EditProfileEvent.CurrentPasswordChanged("Clave#123"))
        viewModel.onEvent(EditProfileEvent.SaveConfirmed)
        runCurrent()

        emailChange.confirmError = authFailure(AuthError.InvalidOtpCode)
        viewModel.onEvent(EditProfileEvent.EmailCodeChanged("0000"))
        viewModel.onEvent(EditProfileEvent.ConfirmEmailCode)
        runCurrent()
        assertEquals(EditProfileField.EMAIL_CODE, viewModel.state.errorField)
        assertEquals(EditProfileDialog.EmailCode("nuevo@correo.com"), viewModel.state.dialog)

        viewModel.onEvent(EditProfileEvent.CancelEmailCode)
        assertNull(viewModel.state.dialog)
        assertEquals(FakeAuthRepository.DEFAULT_EMAIL, viewModel.state.email)
        assertFalse(viewModel.state.isEmailChanged)
    }

    @Test
    fun `CA-PER-02-10 Reenviar codigo espera la cuenta regresiva y vuelve a confirmar la contrasena`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.EmailChanged("nuevo@correo.com"))
        viewModel.onEvent(EditProfileEvent.CurrentPasswordChanged("Clave#123"))
        viewModel.onEvent(EditProfileEvent.SaveConfirmed)
        runCurrent()

        viewModel.onEvent(EditProfileEvent.ResendEmailCode)
        assertEquals("durante la espera no reenvía", 1, emailChange.requested.size)

        advanceTimeBy(60_001)
        assertEquals(0, viewModel.state.resendCooldownSeconds)
        viewModel.onEvent(EditProfileEvent.ResendEmailCode)
        runCurrent()
        assertEquals(2, emailChange.requested.size)
        assertEquals(60, viewModel.state.resendCooldownSeconds)
    }

    @Test
    fun `CA-PER-02-11 contrasena incorrecta marca el campo y no guarda nada`() = runTest {
        auth.reauthenticateResult = authFailure(AuthError.InvalidCredentials)
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.EmailChanged("nuevo@correo.com"))
        viewModel.onEvent(EditProfileEvent.FullNameChanged("Ana María"))
        viewModel.onEvent(EditProfileEvent.CurrentPasswordChanged("mala"))

        viewModel.onEvent(EditProfileEvent.SaveConfirmed)
        runCurrent()

        assertEquals(EditProfileField.CURRENT_PASSWORD, viewModel.state.errorField)
        assertTrue(emailChange.requested.isEmpty())
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun `CA-PER-02-08 elegir Galeria o Camara cierra la hoja y abre el origen`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(EditProfileEvent.PhotoClicked)
        assertEquals(EditProfileDialog.PhotoSourceSheet, viewModel.state.dialog)
        viewModel.onEvent(EditProfileEvent.CameraChosen)
        assertNull(viewModel.state.dialog)
        assertEquals(EditProfileEffect.OpenCamera, viewModel.effects.first())

        viewModel.onEvent(EditProfileEvent.PhotoClicked)
        viewModel.onEvent(EditProfileEvent.GalleryChosen)
        assertEquals(EditProfileEffect.OpenGallery, viewModel.effects.first())
    }

    @Test
    fun `CA-PER-02-08 una foto nueva reemplaza a la anterior y la borra del telefono`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.PhotoPicked("/cache/profile/a.jpg"))

        viewModel.onEvent(EditProfileEvent.PhotoPicked("/cache/profile/b.jpg"))

        assertEquals("/cache/profile/b.jpg", viewModel.state.newPhotoPath)
        assertEquals(EditProfileEffect.DeleteLocalPhoto("/cache/profile/a.jpg"), viewModel.effects.first())
    }

    @Test
    fun `CA-PER-02-13 salir descarta los cambios sin preguntar y borra la foto sin guardar`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onEvent(EditProfileEvent.FullNameChanged("Ana María"))
        viewModel.onEvent(EditProfileEvent.PhotoPicked("/cache/profile/a.jpg"))

        viewModel.onEvent(EditProfileEvent.BackClicked)

        assertEquals(EditProfileEffect.DeleteLocalPhoto("/cache/profile/a.jpg"), viewModel.effects.first())
        assertEquals(EditProfileEffect.NavigateBack, viewModel.effects.first())
        assertNull(viewModel.state.dialog)
        assertTrue(repository.updates.isEmpty())
    }
}
