package com.smartfood.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Nombre: Entities.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Tablas de la base local (spec 014, RF-LOC-01). Cada fila guarda el `uid` de su dueño: los DAO siempre
 * filtran por el usuario con sesión, así que otra cuenta en el mismo teléfono nunca ve datos ajenos (RNF-LOC-02).
 */

/**
 * Copia local de un registro de `users/{uid}/cookings/{id}` (spec 014, HU-LOC-01).
 *
 * @property id Identificador del registro en Firestore.
 * @property uid Dueño del registro.
 * @property recipeId Receta preparada.
 * @property servings Porciones preparadas.
 * @property cookedAt Fecha de preparación en milisegundos.
 * @property rescuedValueCop Valor rescatado en pesos COP (CA-RIA-04-06).
 */
@Entity(tableName = "cookings", indices = [Index("uid")])
data class CookingEntity(
    @PrimaryKey val id: String,
    val uid: String,
    @ColumnInfo(name = "recipe_id") val recipeId: String,
    val servings: Int,
    @ColumnInfo(name = "cooked_at") val cookedAt: Long,
    @ColumnInfo(name = "rescued_value_cop") val rescuedValueCop: Int,
)

/**
 * Una búsqueda reciente (spec 014, HU-LOC-02). La clave incluye el texto normalizado (minúsculas y sin tildes), así
 * que repetir una búsqueda la actualiza en lugar de duplicarla (CA-LOC-02-04).
 *
 * @property uid Dueño de la búsqueda.
 * @property scope Barra donde se hizo: `RECIPES` o `INVENTORY` (nombre de `SearchScope`).
 * @property normalized Texto normalizado, la clave para no duplicar.
 * @property text Texto tal como se mostró en la barra.
 * @property usedAt Última vez que se usó, en milisegundos.
 */
@Entity(tableName = "recent_searches", primaryKeys = ["uid", "scope", "normalized"])
data class RecentSearchEntity(
    val uid: String,
    val scope: String,
    val normalized: String,
    val text: String,
    @ColumnInfo(name = "used_at") val usedAt: Long,
)
