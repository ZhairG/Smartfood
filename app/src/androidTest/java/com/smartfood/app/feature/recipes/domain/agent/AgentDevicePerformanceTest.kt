package com.smartfood.app.feature.recipes.domain.agent

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfood.app.feature.recipes.data.catalog.RecipeAssetSource
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.Store
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Nombre: AgentDevicePerformanceTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Medición del agente en el teléfono con el catálogo real de 70 recetas (spec 009, T052, CA-RIA-02-10,
 * CA-RIA-02-13, RNF-RIA-02), que pidió el informe de IA. Corre los 30 inventarios normales de `AgentEvaluationTest` y
 * los 6 de estrés ("Inventario enorme": todos los ingredientes y 8 por vencer), primero sin límite y luego con el límite
 * de 2 s de la app, y registra en logcat (`adb logcat -s SmartFoodIA`):
 * - el tiempo de cada búsqueda (promedio y peor caso), los nodos y si terminó;
 * - la memoria: el máximo del heap de Java que se usó por encima del de antes de buscar, muestreado cada 2 ms (es una
 *   aproximación: incluye la basura que el recolector todavía no liberó);
 * - con el límite, la brecha con el plan sin límite (solo en los inventarios normales).
 * El estrés sin límite se corta a los 60 s, porque en el APK debug puede tardar minutos.
 *
 * Es una prueba del APK debug, sin R8: el tiempo que vive el usuario se mide además con el APK release y el registro
 * de `AiPlanViewModel`. Solo afirma lo que no depende del equipo: con el límite, ninguna búsqueda pasa de 2 s más un
 * margen de 0,5 s, y el plan que devuelve nunca supera al óptimo.
 */
@RunWith(AndroidJUnit4::class)
class AgentDevicePerformanceTest {

    private val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
    private val catalog = EvaluationCatalog(
        object : RecipeAssetSource {
            override fun read(name: String): String = assets.open(name).bufferedReader().use { it.readText() }
        },
    )
    private val context = PlanContext(budgetCop = 100_000, servings = 4, store = Store.D1)
    private val planner = MealPlanner(catalog.ingredients)

    /** Una búsqueda medida. */
    private data class Measure(val millis: Double, val nodes: Long, val complete: Boolean, val utility: Double?, val heapKb: Long)

    @Test
    fun caRia0210_midePlanesNormalesYDeEstresEnElTelefono() {
        val normal = catalog.syntheticInventories()
        val stress = catalog.largeInventories(count = 6, seed = 2026)
        measure(normal.first(), limited = false) // Calentamiento: carga clases y JIT.

        report("Normales sin límite", normal.map { measure(it, limited = false) })
        // En el APK debug una búsqueda de estrés sin límite puede tardar minutos: se corta a los 60 s y se cuenta cuántas
        // no terminaron (el reporte dice "terminaron n/6").
        report("Estrés sin límite (tope de 60 s)", stress.map { measure(it, limit = STRESS_CAP) })
    }

    @Test
    fun caRia0213_conElLimiteNingunaBusquedaPasaDeDosSegundos() {
        val normal = catalog.syntheticInventories()
        val cases = normal + catalog.largeInventories(count = 6, seed = 2026)
        measure(cases.first(), limited = false) // Calentamiento.

        val limited = cases.map { measure(it, limited = true) }
        report("Normales y estrés con límite de 2 s", limited)
        // La brecha solo en los normales: el óptimo del estrés tardaría minutos en el APK debug.
        val unlimited = normal.map { planner.search(catalog.recipes, it, context)?.utility }
        val gaps = limited.take(normal.size).zip(unlimited)
            .mapNotNull { (m, best) -> if (m.utility != null && best != null) best - m.utility else null }
        log("Brecha con el óptimo (con límite): media ${fmt(gaps.average(), 4)}, peor ${fmt(gaps.maxOrNull() ?: 0.0, 4)}")

        limited.forEach { assertTrue("Con el límite tardó ${it.millis} ms", it.millis <= LIMIT_MARGIN_MILLIS) }
        gaps.forEach { assertTrue("El plan con límite no supera al óptimo", it >= -1e-9) }
    }

    private fun measure(pantry: List<PantryItem>, limited: Boolean): Measure =
        measure(pantry, limit = if (limited) PLAN_LIMIT else null)

    /** Mide una búsqueda con un límite de tiempo, o sin límite si [limit] es `null`. */
    private fun measure(pantry: List<PantryItem>, limit: Duration?): Measure {
        val runtime = Runtime.getRuntime()
        System.gc()
        val baseline = runtime.totalMemory() - runtime.freeMemory()
        val peak = AtomicLong(baseline)
        val running = AtomicBoolean(true)
        val sampler = thread(name = "muestreo-heap") {
            while (running.get()) {
                peak.accumulateAndGet(runtime.totalMemory() - runtime.freeMemory(), ::maxOf)
                Thread.sleep(SAMPLE_MILLIS)
            }
        }
        val deadline = limit?.let { SearchDeadline.after(it) } ?: SearchDeadline.NONE
        val started = System.nanoTime()
        // try/finally: si la búsqueda falla, el muestreo se detiene igual y la prueba muestra el error en vez de colgarse.
        val outcome = try {
            planner.searchOutcome(catalog.recipes, pantry, context, deadline)
        } finally {
            running.set(false)
            sampler.join()
        }
        val millis = (System.nanoTime() - started) / 1_000_000.0
        return Measure(millis, outcome.nodes, outcome.complete, outcome.plan?.utility, (peak.get() - baseline) / 1024)
    }

    private fun report(name: String, measures: List<Measure>) {
        val millis = measures.map { it.millis }
        log(
            "$name (${measures.size}): promedio ${fmt(millis.average(), 1)} ms, peor ${fmt(millis.max(), 1)} ms, " +
                "nodos promedio ${fmt(measures.map { it.nodes.toDouble() }.average(), 0)} (máx. ${measures.maxOf { it.nodes }}), " +
                "terminaron ${measures.count { it.complete }}/${measures.size}, con plan ${measures.count { it.utility != null }}, " +
                "heap extra promedio ${fmt(measures.map { it.heapKb.toDouble() }.average(), 0)} KB (máx. ${measures.maxOf { it.heapKb }} KB)",
        )
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        println("$TAG: $message")
    }

    private fun fmt(value: Double, decimals: Int) = "%.${decimals}f".format(Locale.ROOT, value)

    private companion object {
        const val TAG = "SmartFoodIA"
        const val SAMPLE_MILLIS = 2L
        const val LIMIT_MARGIN_MILLIS = 2_500.0
        val PLAN_LIMIT = 2.seconds
        val STRESS_CAP = 60.seconds
    }
}
