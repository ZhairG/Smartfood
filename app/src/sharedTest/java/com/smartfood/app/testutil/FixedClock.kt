package com.smartfood.app.testutil

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Nombre: FixedClock.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Reloj fijo para las pruebas que dependen de "hoy" (spec 006), como la clasificación por vencimiento.
 */

/** Reloj detenido en la medianoche de [today], en la zona [zone]. */
fun fixedClock(today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Clock =
    Clock.fixed(today.atStartOfDay(zone).toInstant(), zone)
