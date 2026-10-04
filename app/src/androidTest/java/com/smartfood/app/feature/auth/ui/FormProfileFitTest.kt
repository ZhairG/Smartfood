package com.smartfood.app.feature.auth.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.theme.FormMetrics
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.ui.components.AuthScreenScaffold
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: FormProfileFitTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Verifica la elección del perfil de medidas según lo que cabe en la pantalla (spec 002, CA-UI-07-03):
 * un formulario que cabe conserva el perfil de Figma ([FormMetrics.Regular]) y uno que no cabe baja de perfil.
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
        composeTestRule.waitForIdle()
        return checkNotNull(perfil)
    }

    @Test
    fun ca_ui_07_03_siElFormularioCabe_conservaElPerfilDeFigma() {
        // Regresión: antes del primer layout el scroll informa un rango "infinito" y el perfil bajaba sin motivo.
        assertEquals(FormMetrics.Regular, perfilCon(100.dp))
    }

    @Test
    fun ca_ui_07_03_siElFormularioNoCabe_bajaHastaElPerfilCompacto() {
        assertEquals(FormMetrics.Compact, perfilCon(2000.dp))
    }
}
