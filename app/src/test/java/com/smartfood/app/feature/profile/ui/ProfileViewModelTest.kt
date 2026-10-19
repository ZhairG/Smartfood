package com.smartfood.app.feature.profile.ui

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.usecase.LogoutUseCase
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.HasPendingChangesUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.model.UserProfile
import com.smartfood.app.feature.profile.domain.usecase.ObserveProfileUseCase
import com.smartfood.app.feature.profile.domain.usecase.SyncProfileEmailUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeAuthRepository.Companion.DEFAULT_UID
import com.smartfood.app.testutil.FakeExpiryReminderScheduler
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: ProfileViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ProfileViewModel] (HU-05; spec 006 CA-INV-09-03 y Figma `306:369`).
 */
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val profileRepository = FakeProfileRepository()

    @Before
    fun prepararEntornoTest() {
        authRepository.signedInUserId = DEFAULT_UID
        // La cuenta tiene el mismo correo que el perfil: no hay nada que sincronizar.
        authRepository.currentEmailValue = "pepito@correo.com"
    }

    private val pepito = UserProfile(DEFAULT_UID, "Pepito Perez", "pepito@correo.com")
    private val inventoryRepository = FakeInventoryRepository()
    private val photoSync = FakePhotoSync()

    private fun crearViewModel(repository: InventoryRepository = inventoryRepository) = ProfileViewModel(
        observeProfile = ObserveProfileUseCase(profileRepository),
        syncProfileEmail = SyncProfileEmailUseCase(authRepository, profileRepository),
        logout = LogoutUseCase(authRepository, FakeExpiryReminderScheduler(), photoSync),
        observeInventory = ObserveInventoryUseCase(
            repository,
            FakePhotoSync(),
            ClassifyExpiryUseCase(fixedClock(LocalDate.of(2026, 9, 10)))
        ),
        hasPendingChanges = HasPendingChangesUseCase(repository, photoSync)
    )

    @Test
    fun `CA-05-01 saluda con el primer nombre del perfil`() {
        profileRepository.profile.value = pepito

        val state = crearViewModel().uiState.value

        assertEquals("Pepito", state.firstName)
        assertFalse(state.isLoading)
    }

    @Test
    fun `CA-05-02 si el perfil no se puede leer usa saludo generico`() {
        profileRepository.observeError = AuthError.Network

        val state = crearViewModel().uiState.value

        assertNull(state.firstName)
        assertFalse(state.isLoading)
    }

    @Test
    fun `CA-05-03 confirmar el cierre termina la sesion y emite NavigateToLogin`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(ProfileEvent.LogoutClicked)
        viewModel.onEvent(ProfileEvent.LogoutConfirmed)

        assertNull(authRepository.currentUserId())
        assertEquals(ProfileEffect.NavigateToLogin, viewModel.effects.first())
    }

    @Test
    fun `CA-INV-09-03 el encabezado muestra nombre completo correo e iniciales`() {
        profileRepository.profile.value = pepito

        val state = crearViewModel().uiState.value

        assertEquals("Pepito Perez", state.fullName)
        assertEquals("pepito@correo.com", state.email)
        assertEquals("PP", state.initials)
    }

    @Test
    fun `CA-INV-09-03 la tarjeta de ingredientes cuenta los productos del inventario`() {
        inventoryRepository.items.value = listOf(
            inventoryItem(id = "1", name = "Leche"),
            inventoryItem(id = "2", name = "Pan"),
            inventoryItem(id = "3", name = "Arroz")
        )

        assertEquals(3, crearViewModel().uiState.value.inventoryCount)
    }

    @Test
    fun `CA-INV-09-03 si el inventario no se puede leer la tarjeta queda en cero`() {
        val sinSesion = object : InventoryRepository by inventoryRepository {
            override fun observeItems(): Flow<List<InventoryItem>> =
                flow { throw InventoryException(InventoryError.NotAuthenticated) }
        }

        assertEquals(0, crearViewModel(sinSesion).uiState.value.inventoryCount)
    }

    @Test
    fun `las iniciales toman la primera letra de las dos primeras palabras`() {
        assertEquals("AM", ProfileViewModel.initialsOf("ana maría lópez"))
        assertEquals("P", ProfileViewModel.initialsOf("Pepito"))
    }

    // T051: aviso al cerrar sesión con cambios sin subir (CA-INV-10-07).

    @Test
    fun `CA-INV-10-07 con cambios pendientes cerrar sesion primero avisa y no sale`() {
        inventoryRepository.pendingWrites = true
        val viewModel = crearViewModel()

        viewModel.onEvent(ProfileEvent.LogoutClicked)

        assertEquals(ProfileDialog.ConfirmLogout(hasPendingChanges = true), viewModel.uiState.value.dialog)
        assertEquals(DEFAULT_UID, authRepository.currentUserId())
    }

    @Test
    fun `CA-INV-10-07 Cancelar cierra el aviso y deja la sesion como estaba`() {
        photoSync.pending.value = mapOf("leche" to "/files/pending/leche/a.jpg")
        val viewModel = crearViewModel()
        viewModel.onEvent(ProfileEvent.LogoutClicked)

        viewModel.onEvent(ProfileEvent.DialogDismissed)

        assertEquals(null, viewModel.uiState.value.dialog)
        assertEquals(DEFAULT_UID, authRepository.currentUserId())
        assertEquals(0, photoSync.clearAllCalls)
    }

    @Test
    fun `CA-INV-10-07 confirmar cierra la sesion y descarta las fotos pendientes`() = runTest {
        photoSync.pending.value = mapOf("leche" to "/files/pending/leche/a.jpg")
        val viewModel = crearViewModel()
        viewModel.onEvent(ProfileEvent.LogoutClicked)

        viewModel.onEvent(ProfileEvent.LogoutConfirmed)

        assertEquals(ProfileEffect.NavigateToLogin, viewModel.effects.first())
        assertEquals(1, photoSync.clearAllCalls)
        assertEquals(null, authRepository.currentUserId())
    }

    @Test
    fun `CA-05-03 sin cambios pendientes tambien se pide confirmar sin la advertencia`() {
        val viewModel = crearViewModel()

        viewModel.onEvent(ProfileEvent.LogoutClicked)

        assertEquals(ProfileDialog.ConfirmLogout(hasPendingChanges = false), viewModel.uiState.value.dialog)
        assertEquals(DEFAULT_UID, authRepository.currentUserId())
    }

    // Spec 008: Perfil en vivo, foto y acceso a "Editar información".

    @Test
    fun `CA-PER-01-01 muestra la foto y se actualiza al guardar en Editar informacion`() {
        profileRepository.profile.value = pepito
        val viewModel = crearViewModel()
        assertNull(viewModel.uiState.value.photoUrl)

        profileRepository.profile.value = pepito.copy(fullName = "Pepito José", photoUrl = "https://foto")

        assertEquals("https://foto", viewModel.uiState.value.photoUrl)
        assertEquals("Pepito José", viewModel.uiState.value.fullName)
    }

    @Test
    fun `CA-PER-01-03 Editar informacion personal abre su pantalla`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(ProfileEvent.EditProfileClicked)

        assertEquals(ProfileEffect.NavigateToEditProfile, viewModel.effects.first())
    }

    @Test
    fun `CA-PER-02-12 si la cuenta tiene un correo nuevo el perfil lo toma una sola vez`() {
        profileRepository.profile.value = pepito
        authRepository.currentEmailValue = "nuevo@correo.com"

        val state = crearViewModel().uiState.value

        assertEquals(listOf("nuevo@correo.com"), profileRepository.syncedEmails)
        assertEquals("nuevo@correo.com", state.email)
    }
}
