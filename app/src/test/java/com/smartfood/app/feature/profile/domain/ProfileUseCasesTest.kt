package com.smartfood.app.feature.profile.domain

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.usecase.ValidateEmailUseCase
import com.smartfood.app.feature.profile.domain.usecase.ConfirmEmailChangeUseCase
import com.smartfood.app.feature.profile.domain.usecase.ProfileEdits
import com.smartfood.app.feature.profile.domain.usecase.RequestEmailChangeUseCase
import com.smartfood.app.feature.profile.domain.usecase.SyncProfileEmailUseCase
import com.smartfood.app.feature.profile.domain.usecase.UpdateProfileUseCase
import com.smartfood.app.feature.profile.domain.usecase.ValidateProfileUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeEmailChangeRepository
import com.smartfood.app.testutil.FakeProfilePhotoRepository
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.authError
import com.smartfood.app.testutil.fixedClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: ProfileUseCasesTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de los casos de uso de "Editar información" (spec 008, HU-PER-02, T005): validación
 * con tablas de casos, guardado con foto, cambio de correo y sincronización del correo.
 */
class ProfileUseCasesTest {

    private val today = LocalDate.of(2026, 10, 18)
    private val validate = ValidateProfileUseCase(fixedClock(today))
    private val repository = FakeProfileRepository()
    private val photos = FakeProfilePhotoRepository()
    private val auth = FakeAuthRepository()
    private val emailChange = FakeEmailChangeRepository()
    private val update = UpdateProfileUseCase(validate, repository, photos)
    private val current = FakeProfileRepository.defaultProfile()

    @Test
    fun `CA-PER-02-05 CA-PER-02-06 CA-PER-02-07 cada dato invalido falla con su error`() {
        listOf(
            Triple("A", "", null) to AuthError.InvalidName,
            Triple("   ", "", null) to AuthError.InvalidName,
            Triple("x".repeat(51), "", null) to AuthError.InvalidName,
            Triple("Ana", "200 123 4567", null) to AuthError.InvalidPhone,
            Triple("Ana", "300 123 456", null) to AuthError.InvalidPhone,
            Triple("Ana", "+1 300 123 4567", null) to AuthError.InvalidPhone,
            Triple("Ana", "", today.plusDays(1)) to AuthError.InvalidBirthDate,
        ).forEach { (datos, error) ->
            assertEquals("$datos", error, validate(datos.first, datos.second, datos.third).authError)
        }
    }

    @Test
    fun `CA-PER-02-05 CA-PER-02-06 CA-PER-02-07 los datos validos se normalizan`() {
        listOf(
            Triple("  Ana   María ", "", null) to ValidateProfileUseCase.Valid("Ana María", null, null),
            Triple("Ana", "+57 300 123 4567", null) to ValidateProfileUseCase.Valid("Ana", "3001234567", null),
            Triple("Ana", "300-123-4567", null) to ValidateProfileUseCase.Valid("Ana", "3001234567", null),
            Triple("Ana", "(57) 3001234567", today) to ValidateProfileUseCase.Valid("Ana", "3001234567", today),
            Triple("x".repeat(50), "   ", null) to ValidateProfileUseCase.Valid("x".repeat(50), null, null),
        ).forEach { (datos, esperado) ->
            assertEquals("$datos", esperado, validate(datos.first, datos.second, datos.third).getOrThrow())
        }
    }

    @Test
    fun `CA-PER-02-04 CA-PER-02-09 guardar con foto nueva la sube, guarda y borra la anterior`() = runTest {
        val conFoto = current.copy(photoPath = "users/u/profile/vieja.jpg", photoUrl = "https://vieja")

        val guardado = update(conFoto, ProfileEdits("Ana María", "3001234567", today.minusYears(20), "/cache/profile/n.jpg"))
            .getOrThrow()

        assertEquals(listOf("/cache/profile/n.jpg"), photos.uploads)
        assertEquals("users/uid-123/profile/foto-1.jpg", guardado.photoPath)
        assertEquals(guardado, repository.updates.single())
        assertEquals(listOf("users/u/profile/vieja.jpg"), photos.deletes)
    }

    @Test
    fun `CA-PER-02-09 sin red la foto no se sube y nada cambia, y si guardar falla se borra la foto subida`() = runTest {
        photos.uploadResult = authFailure(AuthError.Network)
        assertEquals(AuthError.Network, update(current, ProfileEdits("Ana", "", null, "/f.jpg")).authError)
        assertTrue(repository.updates.isEmpty())

        photos.uploadResult = null
        repository.updateResult = authFailure(AuthError.Network)
        assertEquals(AuthError.Network, update(current, ProfileEdits("Ana", "", null, "/f.jpg")).authError)
        assertEquals("borra la foto que alcanzó a subir", listOf("users/uid-123/profile/foto-1.jpg"), photos.deletes)
    }

    @Test
    fun `CA-PER-02-04 sin foto nueva conserva la foto actual y no borra nada`() = runTest {
        val conFoto = current.copy(photoPath = "p", photoUrl = "u")
        val guardado = update(conFoto, ProfileEdits("Ana", "", null)).getOrThrow()

        assertEquals("p", guardado.photoPath)
        assertTrue(photos.uploads.isEmpty() && photos.deletes.isEmpty())
    }

    @Test
    fun `CA-PER-02-10 CA-PER-02-11 cambiar el correo pide la contrasena y envia el codigo`() = runTest {
        val change = RequestEmailChangeUseCase(ValidateEmailUseCase(), auth, emailChange)

        assertEquals(AuthError.EmptyFields(setOf(AuthField.PASSWORD)), change("nuevo@correo.com", "").authError)
        assertEquals(AuthError.InvalidEmail, change("no-es-correo", "Clave#123").authError)

        auth.reauthenticateResult = authFailure(AuthError.InvalidCredentials)
        assertEquals(AuthError.InvalidCredentials, change("nuevo@correo.com", "mala").authError)
        assertTrue("no pide el código si la contraseña es incorrecta", emailChange.requested.isEmpty())

        auth.reauthenticateResult = Result.success(Unit)
        emailChange.requestResult = authFailure(AuthError.EmailAlreadyInUse)
        assertEquals(AuthError.EmailAlreadyInUse, change("usado@correo.com", "Clave#123").authError)

        emailChange.requestResult = Result.success(42.seconds)
        assertEquals(42.seconds, change("  nuevo@correo.com ", "Clave#123").getOrThrow())
        assertEquals("nuevo@correo.com", emailChange.requested.last())
        assertEquals("Clave#123", auth.lastReauthPassword)
    }

    @Test
    fun `CA-PER-02-11 CA-PER-02-12 confirmar el codigo cambia el correo y pone al dia la sesion`() = runTest {
        val confirm = ConfirmEmailChangeUseCase(auth, emailChange)
        emailChange.requestCode("nuevo@correo.com")

        assertEquals(AuthError.InvalidOtpCode, confirm("12a4", "Clave#123").authError)
        assertEquals(AuthError.InvalidOtpCode, confirm("123", "Clave#123").authError)
        assertTrue("no llama al servidor con un código mal formado", emailChange.confirmedCodes.isEmpty())

        emailChange.confirmError = authFailure(AuthError.OtpCodeExpired)
        assertEquals(AuthError.OtpCodeExpired, confirm("4821", "Clave#123").authError)
        assertNull(auth.lastRefreshedEmail)

        emailChange.confirmError = null
        assertEquals("nuevo@correo.com", confirm(" 4821 ", "Clave#123").getOrThrow())
        assertEquals("nuevo@correo.com", auth.lastRefreshedEmail)
        assertEquals("nuevo@correo.com", auth.currentEmail())
    }

    @Test
    fun `CA-PER-02-12 el correo del perfil se iguala al de la cuenta solo si difiere`() = runTest {
        val sync = SyncProfileEmailUseCase(auth, repository)

        sync(current)
        assertTrue("igual: no escribe", repository.syncedEmails.isEmpty())

        auth.currentEmailValue = "nuevo@correo.com"
        sync(current)
        assertEquals(listOf("nuevo@correo.com"), repository.syncedEmails)
    }
}
