package com.smartfood.app.core.backend.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.smartfood.app.BuildConfig
import com.smartfood.app.core.backend.BackendMode
import com.smartfood.app.core.backend.BackendModeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nombre: RemoteConfigBackendModeProvider.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Lee el interruptor `blaze_activo` de Remote Config, que es gratis en el plan Spark (spec 012,
 * RF-SPK-01). El valor por defecto es `false`: si no se pudo leer, la app usa el modo gratuito (CA-SPK-01-01). En
 * debug, `-PbackendMode=blaze|free` manda sobre Remote Config para probar los dos modos (CA-SPK-01-04).
 * @param remoteConfig instancia de Remote Config de [com.smartfood.app.core.di.CoreDataModule].
 */
@Singleton
class RemoteConfigBackendModeProvider @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) : BackendModeProvider {

    init {
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                // En debug se lee en cada apertura para poder probar los dos modos (CA-SPK-01-03).
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else FETCH_INTERVAL_SECONDS)
                .build()
        )
        remoteConfig.setDefaultsAsync(mapOf(BLAZE_ACTIVE_KEY to false))
    }

    override fun current(): BackendMode = forcedMode(BuildConfig.FORCED_BACKEND_MODE)
        ?: if (remoteConfig.getBoolean(BLAZE_ACTIVE_KEY)) BackendMode.BLAZE else BackendMode.FREE

    override fun refresh() {
        // Sin red o con Remote Config caído, Firebase conserva el último valor activo.
        remoteConfig.fetchAndActivate()
    }

    companion object {
        const val BLAZE_ACTIVE_KEY = "blaze_activo"
        private const val FETCH_INTERVAL_SECONDS = 3_600L

        /** Modo forzado con `-PbackendMode` (solo debug); `null` si no se forzó o el valor no es válido. */
        internal fun forcedMode(value: String): BackendMode? = when (value.trim().lowercase()) {
            "blaze" -> BackendMode.BLAZE
            "free" -> BackendMode.FREE
            else -> null
        }
    }
}
