package com.smartfood.app.feature.recipes.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.data.mapper.mapOwnRecipeError
import com.smartfood.app.feature.recipes.data.mapper.OwnRecipeErrorMapper
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeException
import com.smartfood.app.feature.recipes.domain.repository.OwnRecipesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import javax.inject.Inject

/**
 * Nombre: FirestoreOwnRecipesRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Persistencia de recetas propias en `users/{uid}/recipes` (spec 010, RF-SAV-02, T009).
 */
class FirestoreOwnRecipesRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : OwnRecipesRepository {
    private fun recipes(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw OwnRecipeException(OwnRecipeError.NotAuthenticated)
        return firestore.collection(USERS).document(uid).collection(RECIPES)
    }

    override fun observeRecipes(): Flow<List<OwnRecipe>> = flow {
        emitAll(recipes().snapshots(MetadataChanges.INCLUDE))
    }.map { snapshot ->
        snapshot.documents.mapNotNull { document ->
            // ESTIMATE: sin conexión, la hora del servidor todavía no existe y la receta no debe desaparecer (CA-SAV-03-08).
            ownRecipeFromDocument(
                document.id,
                document.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: emptyMap(),
                document.metadata.hasPendingWrites(),
            )
        }
    }.catch { throw OwnRecipeErrorMapper.toException(it) }

    override suspend fun getRecipe(id: String): Result<OwnRecipe> = safeRunCatching {
        val snapshot = recipes().document(id).get().await()
        ownRecipeFromDocument(snapshot.id, snapshot.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: emptyMap())
            ?: throw OwnRecipeException(OwnRecipeError.NotFound)
    }.mapOwnRecipeError()

    override fun newRecipeId(): String = firestore.collection(RECIPES).document().id

    override suspend fun saveRecipe(recipe: OwnRecipe, isNew: Boolean): Result<WriteOutcome> = write {
        val fields = recipe.toDocumentFields() + mapOf(
            // Las reglas exigen una fecha: al editar o deshacer se conserva la de creación como `Timestamp`.
            OwnRecipeDocument.CREATED_AT to if (isNew) FieldValue.serverTimestamp() else Timestamp(Date(recipe.createdAtMillis)),
            OwnRecipeDocument.UPDATED_AT to FieldValue.serverTimestamp(),
        )
        recipes().document(recipe.id).set(fields)
    }

    override suspend fun deleteRecipe(id: String): Result<WriteOutcome> = write { recipes().document(id).delete() }

    private suspend fun write(block: () -> Task<Void>): Result<WriteOutcome> = safeRunCatching {
        val confirmed = withTimeoutOrNull(WRITE_WAIT_MILLIS) { block().await(); Unit } != null
        if (confirmed) WriteOutcome.SYNCED else WriteOutcome.PENDING
    }.mapOwnRecipeError()

    private companion object {
        const val USERS = "users"
        const val RECIPES = "recipes"
        const val WRITE_WAIT_MILLIS = 3_000L
    }
}
