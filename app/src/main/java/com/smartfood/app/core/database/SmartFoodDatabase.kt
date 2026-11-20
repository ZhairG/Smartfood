package com.smartfood.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Nombre: SmartFoodDatabase.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Base local de SmartFood con Room (spec 014, RF-LOC-01): el historial de cocina y las búsquedas
 * recientes. El esquema se exporta a `app/schemas/` (RNF-LOC-01); un cambio de tablas sube [version] y agrega su
 * migración.
 */
@Database(
    entities = [CookingEntity::class, RecentSearchEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SmartFoodDatabase : RoomDatabase() {

    abstract fun cookingDao(): CookingDao

    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        /** Nombre del archivo de la base en el teléfono. */
        const val NAME = "smartfood.db"
    }
}
