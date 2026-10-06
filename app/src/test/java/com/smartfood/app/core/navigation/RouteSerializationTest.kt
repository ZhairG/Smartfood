package com.smartfood.app.core.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> RouteSerializationTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 28/09/2026<br>
 * <b>Descripción:</b> Pruebas unitarias para verificar la serializabilidad y deserializabilidad de las rutas<br>
 * type-safe [Route] definidas con Kotlin Serialization.<br>
 * </build>
 */
class RouteSerializationTest {

    @Test
    fun rutasAuthGraphYMainGraph_serializanYDeserializanCorrectamente() {
        val authGraph: Route = Route.AuthGraph
        val mainGraph: Route = Route.MainGraph

        val authJson = Json.encodeToString(authGraph)
        val mainJson = Json.encodeToString(mainGraph)

        assertEquals(authGraph, Json.decodeFromString<Route>(authJson))
        assertEquals(mainGraph, Json.decodeFromString<Route>(mainJson))
    }

    @Test
    fun todasLasRutasIndividuales_serializanYDeserializanCorrectamente() {
        val routes = listOf(
            Route.Splash,
            Route.Login,
            Route.Register,
            Route.RegisterSuccess,
            Route.PasswordRecovery,
            Route.PasswordRecoverySuccess,
            Route.Home,
        )

        routes.forEach { route ->
            val json = Json.encodeToString(route)
            val deserialized = Json.decodeFromString<Route>(json)
            assertEquals(route, deserialized)
        }
    }
}
