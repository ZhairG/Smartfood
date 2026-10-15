package com.smartfood.app.feature.auth.ui

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.ActionSuccessScreen
import com.smartfood.app.core.navigation.ObserveEffects
import com.smartfood.app.core.navigation.Route
import com.smartfood.app.core.navigation.navigateToLoginClearingBackStack
import com.smartfood.app.core.navigation.navigateToMainGraph
import com.smartfood.app.feature.auth.ui.login.LoginEffect
import com.smartfood.app.feature.auth.ui.login.LoginScreen
import com.smartfood.app.feature.auth.ui.login.LoginViewModel
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryEffect
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryScreen
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryViewModel
import com.smartfood.app.feature.auth.ui.register.RegisterEffect
import com.smartfood.app.feature.auth.ui.register.RegisterScreen
import com.smartfood.app.feature.auth.ui.register.RegisterViewModel

/**
 * Nombre: AuthNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Grafo de navegación del módulo de autenticación. Cada destino obtiene su ViewModel,
 * recolecta estado y efectos, y delega en una pantalla stateless (patrón Route/Screen, plan.md §2).
 *
 * @param navController Controlador de navegación de la app.
 */
fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation<Route.AuthGraph>(startDestination = Route.Login) {

        composable<Route.Login> {
            val viewModel: LoginViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val autofill = LocalAutofillManager.current

            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    LoginEffect.NavigateToHome -> {
                        // El teléfono ofrece guardar la cuenta con la que se entró (spec 001, CA-07-02).
                        autofill?.commit()
                        navController.navigateToMainGraph()
                    }
                }
            }

            LoginScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onNavigateToRegister = { navController.navigate(Route.Register) },
                onNavigateToForgotPassword = { navController.navigate(Route.PasswordRecovery) }
            )
        }

        composable<Route.Register> {
            val viewModel: RegisterViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val autofill = LocalAutofillManager.current

            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    RegisterEffect.NavigateToSuccess -> {
                        autofill?.commit() // CA-07-02: guardar la cuenta recién creada.
                        navController.navigate(Route.RegisterSuccess) {
                            popUpTo<Route.Register> { inclusive = true }
                        }
                    }
                }
            }

            RegisterScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onNavigateToLogin = { navController.popBackStack() },
                onOpenTerms = { navController.navigate(Route.TermsOfService) },
                onOpenPrivacy = { navController.navigate(Route.PrivacyPolicy) }
            )
        }

        composable<Route.RegisterSuccess> {
            ActionSuccessScreen(
                title = stringResource(id = R.string.register_success_title),
                body = stringResource(id = R.string.register_success_body),
                buttonText = stringResource(id = R.string.register_success_login_button),
                onButtonClick = { navController.navigateToLoginClearingBackStack() }
            )
        }

        composable<Route.PasswordRecovery> {
            val viewModel: PasswordRecoveryViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val autofill = LocalAutofillManager.current

            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    PasswordRecoveryEffect.NavigateToSuccess -> {
                        autofill?.commit() // CA-07-02: actualizar la contraseña guardada.
                        navController.navigate(Route.PasswordRecoverySuccess) {
                            popUpTo<Route.PasswordRecovery> { inclusive = true }
                        }
                    }
                }
            }

            PasswordRecoveryScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable<Route.PasswordRecoverySuccess> {
            ActionSuccessScreen(
                title = stringResource(id = R.string.recovery_success_title),
                body = stringResource(id = R.string.recovery_success_body),
                buttonText = stringResource(id = R.string.recovery_success_button),
                onButtonClick = { navController.navigateToLoginClearingBackStack() }
            )
        }
    }
}
