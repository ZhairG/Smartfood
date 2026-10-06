package com.smartfood.app.feature.auth.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.theme.FormMetrics
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.ui.components.AuthScreenScaffold
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryScreen
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryUiState
import com.smartfood.app.feature.auth.ui.recovery.RecoveryStep
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> FormProfileFitTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Verifica la elección del perfil de medidas según lo que cabe en la pantalla (spec 002,<br>
 * CA-UI-07-03): un formulario que cabe conserva el perfil de Figma ([FormMetrics.Regular]) y uno que no cabe baja<br>
 * de perfil.<br>
 * </build>
 */
class FormProfileFitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Muestra un formulario de [alto] en una ventana de 412×917 dp y devuelve el último perfil usado. */
    private fun perfilCon(alto: Dp): FormMetrics {
        var perfil: FormMetrics? = null
        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(412.dp, 917.dp))) {
                SmartfoodTheme {
                    AuthScreenScaffold(headerContent = { Text("Título") }) {
                        perfil = LocalFormMetrics.current
                        Spacer(Modifier.height(alto))
                    }
                }
            }
        }
        asentar()
        return checkNotNull(perfil)
    }

    /**
     * Deja pasar el tiempo suficiente para que el scaffold confirme (o descarte) cualquier desborde. El reloj de la
     * prueba es virtual: `waitForIdle` no avanza los `delay` de los efectos.
     */
    private fun asentar() {
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.waitForIdle()
    }

    @Test
    fun ca_ui_07_03_siElFormularioCabe_conservaElPerfilDeFigma() {
        // Regresión: antes del primer layout el scroll informa un rango "infinito" y el perfil bajaba sin motivo.
        assertEquals(FormMetrics.Regular, perfilCon(100.dp))
    }

    @Test
    fun ca_ui_07_03_alCambiarDePasoEnLaRecuperacion_noBajaPorElDesbordeDeLaTransicion() {
        // Regresión: durante el fundido entre pasos, el paso saliente conserva su alto un instante. Ese desborde
        // pasajero no debe bajar el perfil (en el V2436 terminaba en el compacto, con el logo y el nombre en fila).
        // Los tres pasos caben con el perfil de Figma en 384×857, así que debe conservarse.
        var estado by mutableStateOf(PasswordRecoveryUiState(step = RecoveryStep.EMAIL_REQUEST))
        var density = 1f
        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(384.dp, 857.dp))) {
                density = LocalDensity.current.density
                SmartfoodTheme { PasswordRecoveryScreen(estado, {}, {}) }
            }
        }
        asentar()
        estado = estado.copy(step = RecoveryStep.OTP_VERIFICATION, email = "demo@smartfood.com")
        asentar()
        estado = estado.copy(step = RecoveryStep.NEW_PASSWORD)
        asentar()

        // El nombre "SmartFood" mide lo que su alto de línea: 60 dp en Regular, 40 en Medium y 36 en Compacto.
        val altoNombre = composeTestRule.onNodeWithText("SmartFood").fetchSemanticsNode().size.height / density
        assertTrue("El paso 3 debe conservar el perfil de Figma (alto del nombre: $altoNombre dp)", altoNombre > 50f)
    }

    @Test
    fun ca_ui_07_03_siElFormularioNoCabe_bajaHastaElPerfilCompacto() {
        assertEquals(FormMetrics.Compact, perfilCon(2000.dp))
    }
}
