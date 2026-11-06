package com.smartfood.app.testutil

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.Favorite
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.repository.OwnRecipesRepository
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeException
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.repository.FavoritesRepository
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Nombre: SavedRecipesFakes.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Fakes de los contratos de la pestaña Guardadas (spec 010, plan §8, T002), compartidos por las pruebas
 * unitarias y las instrumentadas (source set `sharedTest`, #105). `FakeFavoritesRepository` guarda las favoritas en un
 * `MutableStateFlow`, así cada cambio se emite como en Firestore, y registra las llamadas recibidas.
 */

/** Construye una favorita de prueba con valores por defecto. */
fun savedFavorite(
    recipeId: String = "pasta_verde_rapida",
    source: FavoriteSource = FavoriteSource.CATALOG,
    savedAtMillis: Long = 1_000L,
    isPendingSync: Boolean = false,
) = Favorite(recipeId, source, savedAtMillis, isPendingSync)

/**
 * Fake de [FavoritesRepository]. Por defecto todas las escrituras tienen éxito y el servidor las confirma
 * ([WriteOutcome.SYNCED]).
 */
class FakeFavoritesRepository : FavoritesRepository {

    /** Favoritas que emite [observeFavorites]; se puede cambiar directamente desde la prueba. */
    val favorites = MutableStateFlow<List<Favorite>>(emptyList())

    /** Cómo terminan las escrituras: `PENDING` simula que no hay conexión (CA-SAV-01-04). */
    var outcome: WriteOutcome = WriteOutcome.SYNCED

    /** `true` para que las favoritas nuevas queden marcadas como pendientes de sincronizar (CA-SAV-02-09). */
    var pendingWrites: Boolean = false

    /** Si no es `null`, [setFavorite] devuelve este fallo sin cambiar la lista. */
    var setResult: Result<WriteOutcome>? = null

    /** Si no es `null`, [observeFavorites] falla con este error, como cuando Firestore rechaza la lectura. */
    var observeFailure: Throwable? = null

    /** Si no es `null`, las escrituras esperan a que se complete, para ver el estado mientras guardan. */
    var gate: CompletableDeferred<Unit>? = null

    /** Hora que se asigna a la favorita creada por [setFavorite]. */
    var savedAtMillis: Long = 1_000L

    /** Llamadas recibidas en [setFavorite], en orden. */
    val setCalls = mutableListOf<FavoriteCall>()

    override fun observeFavorites(): Flow<List<Favorite>> = flow {
        observeFailure?.let { throw it }
        emitAll(favorites)
    }

    override suspend fun setFavorite(
        recipeId: String,
        source: FavoriteSource,
        favorite: Boolean,
    ): Result<WriteOutcome> {
        setCalls += FavoriteCall(recipeId, source, favorite)
        gate?.await()
        setResult?.let { return it }
        favorites.value = if (favorite) {
            favorites.value.filterNot { it.recipeId == recipeId } +
                Favorite(recipeId, source, savedAtMillis, pendingWrites)
        } else {
            favorites.value.filterNot { it.recipeId == recipeId }
        }
        return Result.success(outcome)
    }

    /**
     * Una llamada a [setFavorite].
     *
     * @property recipeId Receta que se marcó o se quitó.
     * @property source Origen con el que se guardó.
     * @property favorite `true` si se guardó y `false` si se quitó.
     */
    data class FavoriteCall(
        val recipeId: String,
        val source: FavoriteSource,
        val favorite: Boolean,
    )
}

/** Fake de recetas propias para pruebas de dominio y ViewModel (spec 010, T009). */
class FakeOwnRecipesRepository : OwnRecipesRepository {
    val recipes = MutableStateFlow<List<OwnRecipe>>(emptyList())
    var outcome: WriteOutcome = WriteOutcome.SYNCED
    var pendingWrites: Boolean = false
    var getResult: Result<OwnRecipe>? = null
    var saveResult: Result<WriteOutcome>? = null
    var deleteResult: Result<WriteOutcome>? = null
    var nextId: String = "own-recipe-1"
    val saveCalls = mutableListOf<SaveRecipeCall>()

    override fun observeRecipes(): Flow<List<OwnRecipe>> = recipes

    override suspend fun getRecipe(id: String): Result<OwnRecipe> =
        getResult ?: recipes.value.firstOrNull { it.id == id }?.let { Result.success(it) }
        ?: Result.failure(OwnRecipeException(OwnRecipeError.NotFound))

    override fun newRecipeId(): String = nextId

    override suspend fun saveRecipe(recipe: OwnRecipe, isNew: Boolean): Result<WriteOutcome> {
        saveCalls += SaveRecipeCall(recipe, isNew)
        saveResult?.let { return it }
        recipes.value = recipes.value.filterNot { it.id == recipe.id } + recipe.copy(isPendingSync = pendingWrites)
        return Result.success(outcome)
    }

    override suspend fun deleteRecipe(id: String): Result<WriteOutcome> {
        deleteResult?.let { return it }
        recipes.value = recipes.value.filterNot { it.id == id }
        return Result.success(outcome)
    }

    data class SaveRecipeCall(val recipe: OwnRecipe, val isNew: Boolean)
}

/** Catálogo vacío para las pruebas que no dependen de recetas ni ingredientes. */
object EmptyRecipeCatalog : RecipeCatalog {
    override suspend fun recipes(): List<Recipe> = emptyList()
    override suspend fun ingredients(): Map<String, Ingredient> = emptyMap()
}

/** Receta propia mínima para pruebas. */
fun ownRecipe(
    id: String = "own-1",
    name: String = "Arroz casero",
    category: RecipeCategory = RecipeCategory.ARROCES,
    ingredients: List<RecipeIngredient> = listOf(RecipeIngredient("arroz", 200.0)),
    cookMinutes: Int = 30,
    preparation: String = "Cocinar y servir.",
    createdAtMillis: Long = 1_000L,
    updatedAtMillis: Long = 1_000L,
    isPendingSync: Boolean = false,
) = OwnRecipe(id, name, category, ingredients, cookMinutes, preparation, createdAtMillis, updatedAtMillis, isPendingSync)
