package com.smartfood.app.feature.legal.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.smartfood.app.core.navigation.Route
import com.smartfood.app.feature.legal.domain.model.LegalDocumentType

/**
 * Nombre: LegalNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Destinos de los documentos legales (spec 005). Se registran en la raíz de `AppNavHost` para que
 * cualquier grafo pueda abrirlos; hoy los abre el registro (RF-LEG-01). "Volver" regresa a la pantalla anterior
 * con su estado intacto (CA-LEG-01-03).
 *
 * @param navController Controlador de navegación de la app.
 */
fun NavGraphBuilder.legalDestinations(navController: NavHostController) {
    composable<Route.TermsOfService> {
        LegalDocumentScreen(type = LegalDocumentType.TERMS, onBack = { navController.popBackStack() })
    }

    composable<Route.PrivacyPolicy> {
        LegalDocumentScreen(type = LegalDocumentType.PRIVACY, onBack = { navController.popBackStack() })
    }
}
