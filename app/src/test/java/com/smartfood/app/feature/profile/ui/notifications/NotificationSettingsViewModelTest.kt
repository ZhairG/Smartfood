package com.smartfood.app.feature.profile.ui.notifications

import com.smartfood.app.feature.profile.domain.model.NotificationSettings
import com.smartfood.app.feature.profile.domain.usecase.SetNotificationSettingUseCase
import com.smartfood.app.testutil.FakeExpiryReminderScheduler
import com.smartfood.app.testutil.FakeNotificationSettingsRepository
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: NotificationSettingsViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [NotificationSettingsViewModel] (spec 008, HU-PER-04, T017): los tres avisos
 * vienen encendidos, cada interruptor se guarda al instante y el aviso diario se programa o cancela según el ajuste.
 */
class NotificationSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNotificationSettingsRepository()
    private val scheduler = FakeExpiryReminderScheduler()

    private fun crearViewModel() = NotificationSettingsViewModel(
        repository = repository,
        setNotificationSetting = SetNotificationSettingUseCase(repository, scheduler)
    )

    @Test
    fun `CA-PER-04-01 los tres avisos vienen encendidos la primera vez`() {
        val viewModel = crearViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(NotificationSettings(), state.settings)
        assertTrue(state.isOn(NotificationSetting.EXPIRING_PRODUCTS))
        assertTrue(state.isOn(NotificationSetting.SUGGESTED_RECIPES))
        assertTrue(state.isOn(NotificationSetting.EMPTY_INVENTORY))
    }

    @Test
    fun `CA-PER-04-02 apagar productos por vencer lo guarda al instante y programa el aviso diario`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(NotificationSettingsEvent.ToggleRequested(NotificationSetting.EXPIRING_PRODUCTS, false))
        runCurrent()

        assertFalse(viewModel.uiState.value.isOn(NotificationSetting.EXPIRING_PRODUCTS))
        assertEquals(listOf(NotificationSettings(expiringProducts = false)), repository.saved)
        // "Inventario vacío" sigue encendido, así que el trabajo diario se mantiene programado (CA-PER-04-03).
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun `CA-PER-04-03 cancelar el aviso diario solo cuando los dos avisos que lo usan quedan apagados`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(NotificationSettingsEvent.ToggleRequested(NotificationSetting.EXPIRING_PRODUCTS, false))
        runCurrent()
        viewModel.onEvent(NotificationSettingsEvent.ToggleRequested(NotificationSetting.EMPTY_INVENTORY, false))
        runCurrent()

        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(1, scheduler.cancelCalls)
    }

    @Test
    fun `CA-PER-04-05 recetas sugeridas solo se guarda`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(NotificationSettingsEvent.ToggleRequested(NotificationSetting.SUGGESTED_RECIPES, false))
        runCurrent()

        assertFalse(viewModel.uiState.value.isOn(NotificationSetting.SUGGESTED_RECIPES))
        assertEquals(listOf(NotificationSettings(suggestedRecipes = false)), repository.saved)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun `CA-PER-04-06 el permiso negado abre el aviso y abrir ajustes emite el efecto`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(NotificationSettingsEvent.PermissionDenied)
        assertEquals(NotificationSettingsDialog.PermissionDenied, viewModel.uiState.value.dialog)

        viewModel.onEvent(NotificationSettingsEvent.OpenSettingsClicked)
        runCurrent()

        assertNull(viewModel.uiState.value.dialog)
        assertEquals(NotificationSettingsEffect.OpenAppSettings, viewModel.effects.first())
    }
}
