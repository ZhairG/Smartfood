package com.smartfood.app.feature.profile

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.User
import com.smartfood.app.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.smartfood.app.feature.auth.domain.usecase.LogoutUseCase
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeAuthRepository.Companion.DEFAULT_UID
import com.smartfood.app.testutil.FakeExpiryReminderScheduler
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakeUserRepository
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
    private val userRepository = FakeUserRepository()

    @Before
    fun prepararEntornoTest() {
        authRepository.signedInUserId = DEFAULT_UID
    }

    private val inventoryRepository = FakeInventoryRepository()

    private fun crearViewModel(repository: InventoryRepository = inventoryRepository) = ProfileViewModel(
        getCurrentUser = GetCurrentUserUseCase(authRepository, userRepository),
        logout = LogoutUseCase(authRepository, FakeExpiryReminderScheduler()),
        observeInventory = ObserveInventoryUseCase(
            repository,
            ClassifyExpiryUseCase(fixedClock(LocalDate.of(2026, 9, 10)))
        )
    )

    @Test
    fun `CA-05-01 saluda con el primer nombre del perfil`() {
        userRepository.profiles[DEFAULT_UID] = User(DEFAULT_UID, "Pepito Perez", "pepito@correo.com")

        val state = crearViewModel().uiState.value

        assertEquals("Pepito", state.firstName)
        assertFalse(state.isLoading)
    }

    @Test
    fun `CA-05-02 si el perfil no se puede leer usa saludo generico`() {
        userRepository.getProfileError = AuthError.Network

        val state = crearViewModel().uiState.value

        assertNull(state.firstName)
        assertFalse(state.isLoading)
    }

    @Test
    fun `CA-05-03 cerrar sesion termina la sesion y emite NavigateToLogin`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(ProfileEvent.LogoutClicked)

        assertNull(authRepository.currentUserId())
        assertEquals(ProfileEffect.NavigateToLogin, viewModel.effects.first())
    }

    @Test
    fun `CA-INV-09-03 el encabezado muestra nombre completo correo e iniciales`() {
        userRepository.profiles[DEFAULT_UID] = User(DEFAULT_UID, "Pepito Perez", "pepito@correo.com")

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
}
