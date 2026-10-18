package com.smartfood.app.feature.inventory.data.reminder

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.GetExpiringItemsUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.inventoryItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Nombre: ExpiryReminderWorkerTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [ExpiryReminderWorker] con `WorkManagerTestInitHelper` (HU-INV-08, T037 y
 * T038). El trabajador se construye con los fakes compartidos de `sharedTest` para no tocar Firebase (#105). Comprueba que publica el
 * aviso con el producto más próximo (CA-INV-08-01, 08-02), que no publica si no hay productos por vencer
 * (CA-INV-08-03), que la notificación abre la app (CA-INV-08-04) y que sin sesión no publica (CA-INV-08-06).
 */
@RunWith(AndroidJUnit4::class)
class ExpiryReminderWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val today: LocalDate = LocalDate.of(2026, 10, 16)

    private val clock: Clock =
        Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

    private var auth: FakeAuthRepository = sesion("uid-con-sesion")
    private var inventory: FakeInventoryRepository = inventario(emptyList())

    private lateinit var workManager: WorkManager

    @Before
    fun preparar() {
        concederPermisoDeNotificaciones()
        NotificationManagerCompat.from(context).cancelAll()
        val configuracion = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? = when (workerClassName) {
                    ExpiryReminderWorker::class.java.name -> ExpiryReminderWorker(
                        appContext,
                        workerParameters,
                        auth,
                        inventory,
                        GetExpiringItemsUseCase(ClassifyExpiryUseCase(clock)),
                        ExpiryNotifier(appContext)
                    )
                    else -> null
                }
            })
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuracion)
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun limpiar() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    @Test
    fun ca_inv_08_01_y_08_02_publicaElAvisoYNombraElProductoMasProximo() {
        inventory = inventario(
            listOf(
                item("Leche Entera", 0),
                item("Pan", 1),
                item("Arroz", 2)
            )
        )

        ejecutarWorker()

        val notificaciones = notificacionesActivas()
        assertEquals(1, notificaciones.size)
        val notificacion = notificaciones.first().notification
        assertEquals("Productos por vencer", notificacion.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(
            "«Leche Entera» y 2 productos más vencen pronto",
            notificacion.extras.getString(Notification.EXTRA_TEXT)
        )
    }

    @Test
    fun ca_inv_08_01_unSoloProductoUsaElTextoEnSingular() {
        inventory = inventario(listOf(item("Leche Entera", 2)))

        ejecutarWorker()

        val notificacion = notificacionesActivas().single().notification
        assertEquals("«Leche Entera» vence pronto", notificacion.extras.getString(Notification.EXTRA_TEXT))
    }

    @Test
    fun ca_inv_08_03_sinProductosPorVencerNoPublicaNada() {
        inventory = inventario(listOf(item("Arroz", 10)))

        ejecutarWorker()

        assertEquals(0, notificacionesActivas().size)
    }

    @Test
    fun ca_inv_08_04_laNotificacionLaPublicaLaAppParaAbrirlaAlTocarla() {
        inventory = inventario(listOf(item("Leche Entera", 1)))

        ejecutarWorker()

        val notificacion = notificacionesActivas().single().notification
        assertNotNull(notificacion.contentIntent)
        assertEquals(context.packageName, notificacion.contentIntent.creatorPackage)
    }

    @Test
    fun ca_inv_08_06_sinSesionActivaNoPublicaNada() {
        auth = sesion(null)
        inventory = inventario(listOf(item("Leche Entera", 1)))

        ejecutarWorker()

        assertEquals(0, notificacionesActivas().size)
    }

    /** Encola el trabajador como trabajo único y espera a que termine, como haría el scheduler real (T038). */
    private fun ejecutarWorker() {
        val peticion = OneTimeWorkRequestBuilder<ExpiryReminderWorker>().build()
        workManager.enqueue(peticion).result.get()
        val limite = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < limite) {
            val info = workManager.getWorkInfoById(peticion.id).get()
            if (info?.state?.isFinished == true) return
            Thread.sleep(50)
        }
        fail("el aviso de vencimiento no terminó en $TIMEOUT_MS ms")
    }

    private fun notificacionesActivas() = NotificationManagerCompat.from(context).activeNotifications

    private fun item(nombre: String, dias: Long) =
        inventoryItem(id = nombre, name = nombre, expiresOn = today.plusDays(dias))

    /** Con [uid] `null` no hay sesión (CA-INV-08-06). */
    private fun sesion(uid: String?) = FakeAuthRepository().apply { signedInUserId = uid }

    /** El worker solo lee la primera emisión del inventario. */
    private fun inventario(items: List<InventoryItem>) = FakeInventoryRepository().apply { this.items.value = items }

    /** En API 33+ la app necesita el permiso concedido para poder publicar; en el dispositivo lo concede la prueba. */
    private fun concederPermisoDeNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val concedido = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedido) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        assertTrue(
            "el permiso de notificaciones debe estar concedido para probar el aviso",
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
