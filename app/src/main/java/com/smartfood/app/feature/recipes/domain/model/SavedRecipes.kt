package com.smartfood.app.feature.recipes.domain.model

/**
 * Nombre: SavedRecipes.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Modelos de la pestaña Guardadas (spec 010, plan §2.2, T002): las favoritas, los chips, el orden y la
 * tarjeta ya armada para la UI. El modelo `Favorite` sale de `users/{uid}/favorites/{recipeId}` (RF-SAV-01) y el
 * catálogo o las recetas propias aportan el resto de los datos.
 */

/**
 * De dónde sale una receta guardada como favorita (RF-SAV-01).
 *
 * En Firestore se guarda como `"catalog"` o `"own"` en el campo `source`.
 */
enum class FavoriteSource {

    /** Receta del catálogo de `assets/` (spec 009). */
    CATALOG,

    /** Receta creada por la persona (spec 010, #130). */
    OWN,
}

/**
 * Una receta marcada con ♡.
 *
 * @property recipeId Id del catálogo o de una receta propia.
 * @property source Origen de la receta ([FavoriteSource]).
 * @property savedAtMillis Hora en que se guardó o se volvió a marcar, en milisegundos.
 * @property isPendingSync `true` si la escritura todavía espera subir al servidor (CA-SAV-01-04).
 */
data class Favorite(
    val recipeId: String,
    val source: FavoriteSource,
    val savedAtMillis: Long,
    val isPendingSync: Boolean = false,
)

/** Chips de la lista Guardadas (Figma `633:2723`, CA-SAV-02-02). */
enum class SavedFilter {

    /** Recetas del catálogo marcadas con ♡. */
    FAVORITES,

    /** Recetas creadas por la persona (spec 010, #130). */
    OWN,

    /** Favoritas y propias juntas, de la más reciente a la más antigua (CA-SAV-02-03). */
    RECENT,
}

/** Orden de la lista Guardadas, que se elige en "Ordenar" (CA-SAV-02-05). */
enum class SavedSort {

    /** Por la última fecha de cambio: guardado, creado o editado (el de por defecto). */
    RECENT,

    /** Por nombre, de la A a la Z. */
    NAME,
}

/**
 * Una tarjeta de la lista Guardadas, ya armada para la UI.
 *
 * @property id Id del catálogo o de la receta propia.
 * @property name Nombre para mostrar.
 * @property photo Nombre del recurso de la foto en `drawable-nodpi`; `null` en las recetas propias, que muestran el
 *   ícono de libro (desviación §9).
 * @property servings Porciones de la receta ("2 porciones" en las propias, spec §7).
 * @property minutes Tiempo total en minutos.
 * @property isFavorite `true` si la tarjeta muestra el ♡ relleno.
 * @property isOwn `true` si es una receta creada por la persona.
 * @property lastChangeMillis Fecha de guardado o de la última edición, para "Recientes" y "Más recientes" (spec §7).
 * @property isPendingSync `true` si la tarjeta espera sincronizarse (CA-SAV-02-09).
 */
data class SavedRecipeCard(
    val id: String,
    val name: String,
    val photo: String?,
    val servings: Int,
    val minutes: Int,
    val isFavorite: Boolean,
    val isOwn: Boolean,
    val lastChangeMillis: Long,
    val isPendingSync: Boolean,
)

/**
 * Resultado de combinar las favoritas con el catálogo para la pantalla Guardadas.
 *
 * @property cards Tarjetas después de aplicar chip, búsqueda y orden.
 * @property isEmptyOverall `true` si no hay recetas guardadas válidas; las favoritas huérfanas no cuentan.
 * @property hasPendingSync `true` si alguna favorita visible en Guardadas espera sincronizarse.
 */
data class SavedRecipesData(
    val cards: List<SavedRecipeCard>,
    val isEmptyOverall: Boolean,
    val hasPendingSync: Boolean,
)

/** Categorías disponibles para una receta creada por la persona (RF-SAV-02). */
enum class RecipeCategory {
    AREPAS, ARROCES, CARNES, POLLO, PESCADOS, PASTAS, SOPAS, ENSALADAS, DESAYUNOS, POSTRES, OTRAS,
}

/** Receta propia persistida en `users/{uid}/recipes/{recipeId}`. */
data class OwnRecipe(
    val id: String,
    val name: String,
    val category: RecipeCategory,
    val ingredients: List<RecipeIngredient>,
    val cookMinutes: Int,
    val preparation: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val isPendingSync: Boolean = false,
) {
    companion object { const val SERVINGS = 2 }
}

/** Datos sin validar que provienen del formulario de receta propia. */
data class OwnRecipeDraft(
    val name: String = "",
    val category: RecipeCategory? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val cookMinutes: String = "",
    val preparation: String = "",
)

/** Campo que origina el primer error de validación del editor. */
enum class OwnRecipeField { NAME, CATEGORY, INGREDIENTS, COOK_MINUTES, PREPARATION }

/** Errores tipados de recetas propias. */
sealed interface OwnRecipeError {
    data class Invalid(val field: OwnRecipeField) : OwnRecipeError
    data object NotFound : OwnRecipeError
    data object Network : OwnRecipeError
    data object PermissionDenied : OwnRecipeError
    data object NotAuthenticated : OwnRecipeError
    data class Unknown(val code: String? = null) : OwnRecipeError
}

/** Excepción de dominio usada para transportar [OwnRecipeError]. */
class OwnRecipeException(val error: OwnRecipeError) : Exception()
