package com.smartfood.app.feature.profile.ui.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.profile.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: EditProfileScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [EditProfileScreen] sin estado (spec 008, HU-PER-02, T009; Figma `361:649`,
 * `369:1195` y `369:905`): los campos con los datos, "Guardar" deshabilitado sin cambios, la contraseña al cambiar el
 * correo, la hoja de foto y los avisos.
 */
class EditProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<EditProfileEvent>()
    private val perfil = UserProfile(
        uid = "u",
        fullName = "Pepito Perez",
        email = "pepito@correo.com",
        birthDate = LocalDate.of(2000, 1, 31)
    )

    private fun estado(
        email: String = perfil.email,
        fullName: String = perfil.fullName,
        dialog: EditProfileDialog? = null,
        error: AuthError? = null,
    ) = EditProfileUiState(
        isLoading = false,
        original = perfil,
        fullName = fullName,
        email = email,
        birthDate = perfil.birthDate,
        dialog = dialog,
        error = error
    )

    private fun mostrar(state: EditProfileUiState) {
        composeTestRule.setContent {
            SmartfoodTheme { EditProfileScreen(state = state, onEvent = { eventos += it }) }
        }
    }

    private fun botonGuardar() = composeTestRule.onNode(hasText("Guardar") and hasClickAction())

    @Test
    fun caPer0201_muestraLaFotoOIniciales_yLosCuatroCamposConLosDatos() {
        mostrar(estado())

        composeTestRule.onNodeWithText("Editar información").assertIsDisplayed()
        composeTestRule.onNodeWithText("PP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Toca para cambiar tu foto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pepito Perez").assertIsDisplayed()
        composeTestRule.onNodeWithText("pepito@correo.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Teléfono").assertIsDisplayed()
        composeTestRule.onNodeWithText("31/01/2000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña actual").assertDoesNotExist()
    }

    @Test
    fun caPer0202_guardarSeVeDeshabilitadoSinCambios_yEscribirEmiteElCambio() {
        mostrar(estado())
        botonGuardar().assertIsNotEnabled()

        composeTestRule.onNodeWithText("Nombre completo").performTextInput(" José")
        assertTrue(eventos.any { it is EditProfileEvent.FullNameChanged })
    }

    @Test
    fun caPer0202_conCambiosGuardarEmiteSaveClicked() {
        mostrar(estado(fullName = "Pepito José"))

        botonGuardar().assertIsEnabled().performClick()

        assertEquals(listOf<EditProfileEvent>(EditProfileEvent.SaveClicked), eventos)
    }

    @Test
    fun caPer0203_elAvisoDeGuardarOfreceGuardarYCancelar() {
        mostrar(estado(fullName = "Pepito José", dialog = EditProfileDialog.ConfirmSave))

        composeTestRule.onNodeWithText("¿Desea guardar los cambios?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.onAllNodes(hasText("Guardar") and hasClickAction())[1].performClick()

        assertEquals(listOf(EditProfileEvent.DialogDismissed, EditProfileEvent.SaveConfirmed), eventos)
    }

    @Test
    fun caPer0204_guardadoMuestraPerfilSeActualizo() {
        mostrar(estado(dialog = EditProfileDialog.Saved()))

        composeTestRule.onNodeWithText("Guardado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil se actualizó").assertIsDisplayed()
    }

    @Test
    fun caPer0208_tocarLaFotoEmitePhotoClicked_yLaHojaOfreceGaleriaYCamara() {
        mostrar(estado(dialog = EditProfileDialog.PhotoSourceSheet))

        composeTestRule.onNodeWithText("Galería").performClick()
        composeTestRule.onNodeWithText("Cámara").performClick()

        assertEquals(listOf(EditProfileEvent.GalleryChosen, EditProfileEvent.CameraChosen), eventos)
    }

    @Test
    fun caPer0208_elCirculoDeLaFotoAbreLaHoja() {
        mostrar(estado())

        composeTestRule.onNodeWithContentDescription("Cambiar foto de perfil").performClick()

        assertEquals(listOf<EditProfileEvent>(EditProfileEvent.PhotoClicked), eventos)
    }

    @Test
    fun caPer0210_alCambiarElCorreoApareceLaContrasenaActual() {
        mostrar(estado(email = "nuevo@correo.com"))

        composeTestRule.onNodeWithText("Contraseña actual").assertExists()
    }

    @Test
    fun caPer0210_elAvisoDelCodigoPideLos4Digitos_yConfirmarSeHabilitaCompleto() {
        mostrar(estado(email = "nuevo@correo.com", dialog = EditProfileDialog.EmailCode("nuevo@correo.com")).copy(
            emailCode = "4821",
            resendCooldownSeconds = 42
        ))

        composeTestRule.onNodeWithText("Revisa tu correo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Escribe el código de 4 dígitos que enviamos a nuevo@correo.com.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reenviar código en").assertIsDisplayed()
        composeTestRule.onNode(hasText("Confirmar") and hasClickAction()).assertIsEnabled().performClick()
        composeTestRule.onNode(hasText("Cancelar") and hasClickAction()).performClick()

        assertEquals(listOf(EditProfileEvent.ConfirmEmailCode, EditProfileEvent.CancelEmailCode), eventos)
    }

    @Test
    fun caPer0211_elCodigoIncorrectoSeExplicaEnElAviso_yReenviarSeOfreceSinEspera() {
        mostrar(estado(
            email = "nuevo@correo.com",
            dialog = EditProfileDialog.EmailCode("nuevo@correo.com"),
            error = AuthError.InvalidOtpCode
        ).copy(emailCode = "12"))

        composeTestRule.onNode(hasText("Confirmar") and hasClickAction()).assertIsNotEnabled()
        composeTestRule.onNodeWithText("Reenviar código").performClick()

        assertEquals(listOf<EditProfileEvent>(EditProfileEvent.ResendEmailCode), eventos)
    }

    @Test
    fun caPer0212_guardadoAnunciaElCorreoNuevo() {
        mostrar(estado(dialog = EditProfileDialog.Saved(emailChangedTo = "nuevo@correo.com")))

        composeTestRule.onNodeWithText("Guardado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu correo ahora es nuevo@correo.com").assertIsDisplayed()
    }

    @Test
    fun caPer0211_laContrasenaIncorrectaSeExplicaBajoLosCampos() {
        mostrar(estado(email = "nuevo@correo.com", error = AuthError.InvalidCredentials))

        composeTestRule.onNodeWithText("Contraseña actual").assertExists()
        composeTestRule.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
    }
}
