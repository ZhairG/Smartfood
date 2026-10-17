package com.smartfood.app.feature.inventory.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartfood.app.BuildConfig
import com.smartfood.app.feature.inventory.data.repository.FirestoreInventoryRepository
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
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
import org.junit.Assert.assertFalse
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

        // Una lectura primero calienta la conexión y las reglas del emulador: la primera escritura en frío puede
        // tardar más que la espera de 3 s y quedar PENDING aunque haya red.
        repository.getItem("calentar")

        // Crear: las reglas exigen createdAt y updatedAt con la hora del servidor. Con red, el servidor lo confirma.
        assertEquals(WriteOutcome.SYNCED, repository.createItem(item).getOrThrow())
        assertTrue(item in repository.observeItems().first())
        assertEquals(item, repository.getItem(item.id).getOrNull())
        val creadoEn = createdAt(item.id)

        // Editar quitando la cantidad: `update` borra quantity y unit, y createdAt no cambia (T047).
        val editado = item.copy(name = "Leche Deslactosada", quantity = null)
        assertEquals(WriteOutcome.SYNCED, repository.updateItem(editado, photoChanged = false).getOrThrow())
        val leido = repository.getItem(item.id).getOrThrow()
        assertEquals("Leche Deslactosada", leido.name)
        assertNull(leido.quantity)
        assertEquals(creadoEn, createdAt(item.id))

        // La foto la pone el worker con setPhoto; en un producto que ya no existe falla con NotFound (T048).
        val foto = ItemPhoto("users/${auth.currentUser!!.uid}/inventory/${item.id}/f.jpg", "https://foto")
        assertTrue(repository.setPhoto(item.id, foto).isSuccess)
        assertEquals(foto, repository.getItem(item.id).getOrThrow().photo)
        assertFalse(repository.hasPendingWrites())

        // Sin conexión, la escritura queda en el teléfono: PENDING, el producto aparece pendiente y se sube al
        // volver la conexión (CA-INV-10-02, 10-05).
        firestore.disableNetwork().await()
        val sinRed = item.copy(id = repository.newItemId(), name = "Arroz")
        assertEquals(WriteOutcome.PENDING, repository.createItem(sinRed).getOrThrow())
        assertTrue(repository.observeItems().first().single { it.id == sinRed.id }.isPendingSync)
        assertTrue(repository.hasPendingWrites())
        firestore.enableNetwork().await()
        firestore.waitForPendingWrites().await()
        assertFalse(repository.hasPendingWrites())
        assertFalse(repository.observeItems().first().single { it.id == sinRed.id }.isPendingSync)

        // Borrar.
        assertEquals(WriteOutcome.SYNCED, repository.deleteItem(item.id).getOrThrow())
        assertEquals(InventoryError.NotFound, repository.getItem(item.id).exceptionOrNull()?.toInventoryError())
        assertEquals(InventoryError.NotFound, repository.setPhoto(item.id, foto).exceptionOrNull()?.toInventoryError())

        // Sin sesión, guardar falla con NotAuthenticated en lugar de lanzar.
        auth.signOut()
        assertEquals(
            InventoryError.NotAuthenticated,
            repository.createItem(item.copy(id = repository.newItemId())).exceptionOrNull()?.toInventoryError()
        )
    }
}
