package com.smartfood.app.feature.recipes.ui.ai

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.recipes.domain.agent.PlanExplainer
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.MealPlan
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion
import com.smartfood.app.feature.recipes.domain.model.PlannedDay
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import com.smartfood.app.feature.recipes.domain.model.Store
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: AiFlowScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas del flujo de la IA sin estado (spec 009, T015 a T018; Figma `633:2948`,
 * `666:3546`, `557:2903`, `557:2515` y `557:2476`): el contexto con el aviso y las preferencias (CA-RIA-02-01), los
 * chips que abren las hojas, "Analizando opciones" con "Cancelar" (CA-RIA-02-02), "Tu plan de 3 días" con
 * "¿Por qué te conviene?" (CA-RIA-02-03, 02-06, 02-08), "Sin coincidencias" (CA-RIA-02-07) y la Ayuda (CA-RIA-03-01).
 */
class AiFlowScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<AiEvent>()
    private val estado = mutableStateOf(AiUiState())

    private fun mostrar(inicial: AiUiState) {
        estado.value = inicial
        composeTestRule.setContent {
            SmartfoodTheme {
                AiFlowScreen(state = estado.value, onEvent = { eventos += it }, onBack = {})
            }
        }
    }

    private val pollo = InventoryItem("p1", "Pollo", FoodCategory.PROTEINS, LocalDate.now().plusDays(1))
    private val arroz = InventoryItem("a1", "Arroz", FoodCategory.GRAINS, LocalDate.now().plusDays(60))

    private val contexto = AiUiState(
        isLoadingContext = false,
        detected = listOf(pollo, arroz),
        expiringIds = setOf("p1"),
        firstExpiring = pollo,
        firstExpiringDays = 1,
        store = Store.D1,
    )

    private fun receta(id: String, nombre: String, minutos: Int) = Recipe(
        id = id,
        name = nombre,
        servings = 4,
        prepMinutes = 10,
        cookMinutes = minutos - 10,
        difficulty = "Fácil",
        kcal = 400,
        ingredients = emptyList(),
        steps = listOf(RecipeStep("Cocinar", "Cocina")),
    )

    private val plan = MealPlan(
        days = listOf(
            PlannedDay(1, receta("pollo-guisado", "Pollo guisado con arroz", 35), emptyList(), 7_800, 0.92, 1.0),
            PlannedDay(2, receta("arroz-con-huevo", "Arroz con huevo", 20), emptyList(), 3_000, 0.5, 0.5),
            PlannedDay(3, receta("lentejas", "Lentejas guisadas", 45), emptyList(), 6_500, 0.4, 0.2),
        ),
        totalCostCop = 17_300,
        remainingBudgetCop = 32_700,
        wastedItems = emptyList(),
        utility = 1.7,
    )

    private val ingredientes = mapOf(
        "pollo" to Ingredient("pollo", "Pollo", section = "Carnes", unit = IngredientUnit.GRAM, pack = 1000.0, prices = mapOf(Store.D1 to 14_000L)),
        "cilantro" to Ingredient("cilantro", "Cilantro", section = "Verduras", unit = IngredientUnit.GRAM, pack = 50.0, prices = mapOf(Store.D1 to 1_200L)),
    )

    @Test
    fun caRia0201_elContextoMuestraInventarioAvisoPresupuestoYOpciones() {
        mostrar(contexto)

        composeTestRule.onNodeWithText("Sugerencia con IA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cocina con lo que ya tienes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pollo").assertIsDisplayed()
        composeTestRule.onNodeWithText("⚠ Pollo vence mañana.").assertIsDisplayed()
        composeTestRule.onNodeWithText("$50.000").assertIsDisplayed()
        composeTestRule.onNodeWithText("COP $50.000 · D1").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 porciones").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sin límite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Generar recomendaciones").performScrollTo().performClick()

        assertEquals(listOf<AiEvent>(AiEvent.GenerateClicked), eventos)
    }

    @Test
    fun caRia0201_conRestriccionesMuestraSegunTusPreferencias() {
        mostrar(contexto.copy(restrictions = setOf(DietaryRestriction.VEGAN)))

        composeTestRule.onNodeWithText("Según tus preferencias: Vegano").assertIsDisplayed()
    }

    @Test
    fun caRia0201_porcionesYTiempoAbrenLaHojaYPicanteSeAlterna() {
        mostrar(contexto)

        composeTestRule.onNodeWithText("4 porciones").performClick()
        composeTestRule.onNodeWithText("Sin picante").performClick()
        composeTestRule.onNodeWithText("Sin límite").performClick()
        estado.value = contexto.copy(sheet = AiSheet.Time)
        composeTestRule.waitForIdle()

        assertEquals(listOf(AiEvent.ServingsClicked, AiEvent.SpicyToggled, AiEvent.TimeClicked), eventos)
        composeTestRule.onNodeWithText("Tiempo máximo por receta").assertIsDisplayed()
    }

    @Test
    fun caRia0202_analizandoMuestraElPresupuestoYCancelarAvisa() {
        mostrar(contexto.copy(step = AiStep.Loading))

        composeTestRule.onNodeWithText("Analizando opciones").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buscando el mejor aprovechamiento…").assertIsDisplayed()
        composeTestRule.onNodeWithText("presupuesto de COP $50.000", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").performClick()

        assertEquals(listOf<AiEvent>(AiEvent.CancelClicked), eventos)
    }

    @Test
    fun caRia0203_elPlanMuestraTresDiasConSuTarjetaYAbreElDetalle() {
        mostrar(contexto.copy(step = AiStep.Plan, plan = plan))

        composeTestRule.onNodeWithText("Tu plan de 3 días").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Recomendaciones listas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Día 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("92 % del inventario · COP $7.800 · 35 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("Día 3").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Pollo guisado con arroz").performScrollTo().performClick()

        assertEquals(listOf<AiEvent>(AiEvent.RecipeClicked("pollo-guisado")), eventos)
    }

    @Test
    fun caRia0206_porQueTeConvieneExplicaConDatosDelPlan() {
        mostrar(
            contexto.copy(
                step = AiStep.Plan,
                plan = plan,
                ingredients = ingredientes,
                reasons = listOf(
                    PlanExplainer.Reason.RescuesExpiring("pollo", 200.0, daysToExpire = 1, day = 1),
                    PlanExplainer.Reason.UsesPantry(5, 6),
                    PlanExplainer.Reason.NeedsToBuy("cilantro", 50.0, 1_200),
                    PlanExplainer.Reason.BudgetLeft(32_700),
                )
            )
        )

        composeTestRule.onNodeWithText("¿Por qué te conviene?").performScrollTo().assertIsDisplayed()
        val texto = "Usa 200 g de pollo que vence mañana (Día 1)."
        // Las razones van en un solo párrafo, debajo de las tres tarjetas: hay que desplazarse hasta él.
        composeTestRule.onNodeWithText(texto, substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Aprovecha 5 de 6 ingredientes de tu inventario.", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Falta cilantro (aprox. COP $1.200).", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Queda COP $32.700 de tu presupuesto.", substring = true).assertIsDisplayed()
    }

    @Test
    fun caRia0208_ajustarDelPlanAvisa() {
        mostrar(contexto.copy(step = AiStep.Plan, plan = plan))

        composeTestRule.onNodeWithText("Ajustar").performClick()

        assertEquals(listOf<AiEvent>(AiEvent.AdjustClicked), eventos)
    }

    @Test
    fun caRia0207_sinCoincidenciasMuestraLasSugerenciasYSusAcciones() {
        mostrar(
            contexto.copy(
                step = AiStep.NoPlan,
                budgetCop = 2_000,
                suggestions = listOf(PlanSuggestion.RaiseBudget(45_000), PlanSuggestion.AllowSpicy)
            )
        )

        composeTestRule.onNodeWithText("Sin coincidencias").assertIsDisplayed()
        composeTestRule.onNodeWithText("No encontramos una opción segura").assertIsDisplayed()
        composeTestRule.onNodeWithText("Con COP $2.000", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sube el presupuesto a COP $45.000", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Permite recetas picantes", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ajustar contexto").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Ver recetas económicas").performScrollTo().performClick()

        assertEquals(listOf(AiEvent.AdjustClicked, AiEvent.EconomicRecipesClicked), eventos)
    }

    @Test
    fun caRia0301_laAyudaMuestraLosCriteriosYContactarSoporte() {
        var contacto = false
        composeTestRule.setContent {
            SmartfoodTheme { AiHelpScreen(onBack = {}, onContactSupport = { contacto = true }) }
        }

        composeTestRule.onNodeWithText("¿Cómo decide la IA?").assertIsDisplayed()
        composeTestRule.onNodeWithText("1. Urgencia de vencimiento").assertIsDisplayed()
        composeTestRule.onNodeWithText("Controles y seguridad").assertIsDisplayed()
        composeTestRule.onNodeWithText("La IA puede equivocarse").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Contactar soporte").performScrollTo().performClick()

        assertTrue(contacto)
    }
}
