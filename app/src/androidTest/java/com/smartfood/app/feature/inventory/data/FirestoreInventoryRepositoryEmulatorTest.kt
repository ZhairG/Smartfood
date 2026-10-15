package com.smartfood.app.feature.inventory.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartfood.app.BuildConfig
import com.smartfood.app.feature.inventory.data.repository.FirestoreInventoryRepository
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.toInventoryError
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Nombre: FirestoreInventoryRepositoryEmulatorTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Prueba de punta a punta de [FirestoreInventoryRepository] contra el Emulator Suite con las reglas
 * reales (spec 006, RF-INV-01, RNF-INV-01, T017 y T022). Solo se ejecuta en un build con
 * `-PuseFirebaseEmulator=true`; en las corridas normales se omite. Con un teléfono físico:
 * `adb reverse tcp:9099 tcp:9099`, `adb reverse tcp:8080 tcp:8080` y `-PfirebaseEmulatorHost=127.0.0.1`.
 * Es un solo `@Test`: Hilt arma el grafo en cada prueba y `CoreDataModule` no puede llamar `useEmulator()` dos veces
 * sobre la misma instancia de Firebase en un mismo proceso.
 */
@HiltAndroidTest
class FirestoreInventoryRepositoryEmulatorTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var auth: FirebaseAuth

    private lateinit var repository: FirestoreInventoryRepository

    @Before
    fun prepararEntornoTest() {
        assumeTrue("Solo con -PuseFirebaseEmulator=true", BuildConfig.USE_FIREBASE_EMULATOR)
        hiltRule.inject()
        repository = FirestoreInventoryRepository(firestore, auth)
        runBlocking {
            auth.createUserWithEmailAndPassword("inv-${UUID.randomUUID()}@prueba.com", "Clave123*").await()
        }
    }

    @After
    fun cerrarSesion() {
        if (::auth.isInitialized) auth.signOut()
    }

    private suspend fun createdAt(id: String): Timestamp? =
        firestore.collection("users").document(auth.currentUser!!.uid)
            .collection("inventory").document(id).get().await().getTimestamp("createdAt")

    @Test
    fun rfInv01_creaLeeEditaBorraYSinSesionFallaCumpliendoLasReglas() = runBlocking {
        val item = InventoryItem(
            id = repository.newItemId(),
            name = "Leche Entera",
            category = FoodCategory.DAIRY,
            expiresOn = LocalDate.now().plusDays(5),
            quantity = Quantity(1.5, QuantityUnit.LITERS),
        )

        // Crear: las reglas exigen createdAt y updatedAt con la hora del servidor.
        assertTrue(repository.saveItem(item).isSuccess)
        assertTrue(item in repository.observeItems().first())
        assertEquals(item, repository.getItem(item.id).getOrNull())
        val creadoEn = createdAt(item.id)

        // Editar quitando la cantidad: el set completo borra quantity y unit, y createdAt no cambia.
        val editado = item.copy(name = "Leche Deslactosada", quantity = null)
        assertTrue(repository.saveItem(editado).isSuccess)
        val leido = repository.getItem(item.id).getOrThrow()
        assertEquals("Leche Deslactosada", leido.name)
        assertNull(leido.quantity)
        assertEquals(creadoEn, createdAt(item.id))

        // Borrar.
        assertTrue(repository.deleteItem(item.id).isSuccess)
        assertEquals(InventoryError.NotFound, repository.getItem(item.id).exceptionOrNull()?.toInventoryError())

        // Sin sesión, guardar falla con NotAuthenticated en lugar de lanzar.
        auth.signOut()
        assertEquals(
            InventoryError.NotAuthenticated,
            repository.saveItem(item.copy(id = repository.newItemId())).exceptionOrNull()?.toInventoryError()
        )
    }
}
