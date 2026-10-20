package com.smartfood.app.feature.profile.domain

import com.smartfood.app.feature.profile.domain.model.NotificationSettings
import com.smartfood.app.feature.profile.domain.usecase.SetNotificationSettingUseCase
import com.smartfood.app.testutil.FakeExpiryReminderScheduler
import com.smartfood.app.testutil.FakeNotificationSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nombre: SetNotificationSettingUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [SetNotificationSettingUseCase] (spec 008, T015, CA-PER-04-03): guarda el ajuste y
 * programa el aviso diario mientras cualquiera de los dos avisos que lo usan esté encendido; lo cancela solo si ambos
 * quedan apagados.
 */
class SetNotificationSettingUseCaseTest {

    private val repository = FakeNotificationSettingsRepository()
    private val scheduler = FakeExpiryReminderScheduler()
    private val setSetting = SetNotificationSettingUseCase(repository, scheduler)

    @Test
    fun `programar cuando productos por vencer sigue encendido (CA-PER-04-03)`() = runTest {
        setSetting(NotificationSettings(expiringProducts = true, suggestedRecipes = true, emptyInventory = false))

        assertEquals(listOf(NotificationSettings(expiringProducts = true, emptyInventory = false)), repository.saved)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun `programar cuando solo inventario vacio esta encendido (CA-PER-04-03)`() = runTest {
        setSetting(NotificationSettings(expiringProducts = false, suggestedRecipes = false, emptyInventory = true))

        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun `cancelar cuando los dos avisos del trabajo diario quedan apagados (CA-PER-04-03)`() = runTest {
        setSetting(NotificationSettings(expiringProducts = false, suggestedRecipes = true, emptyInventory = false))

        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(1, scheduler.cancelCalls)
    }

    @Test
    fun `recetas sugeridas sola no programa el trabajo diario (CA-PER-04-05)`() = runTest {
        setSetting(NotificationSettings(expiringProducts = false, suggestedRecipes = true, emptyInventory = false))

        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(1, scheduler.cancelCalls)
    }
}
