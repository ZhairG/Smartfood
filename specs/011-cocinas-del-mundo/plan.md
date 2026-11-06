# Plan técnico 011: Cocinas del mundo

> Cómo se cumple la [spec](./spec.md). Ruta base: `app/src/main/java/com/smartfood/app/` (abreviada `…/`).

## 1. Arquitectura

La cocina viaja por el mismo camino que la dieta, sin clases nuevas de datos:

```
Preferencias (Cuisine) → GetAiContextUseCase → AiContext.cuisines → GeneratePlanUseCase → PlanContext.cuisines → MealPlanner
```

`RecipeCuisine` (recetas) tiene los mismos nombres que `Cuisine` (Perfil), con el patrón `DietRule` / `DietaryRestriction`:
el dominio de recetas no conoce el modelo del Perfil en el agente y el flujo de la IA traduce 1:1 por nombre.

## 2. Modelo de datos

### 2.1 `assets/recetas.json`

Campo obligatorio `"cocina"`, después de `"foto"`. Valores: `colombiana`, `italiana`, `asiatica`, `mexicana`, `mediterranea`.

### 2.2 Dominio

```kotlin
// …/feature/recipes/domain/model/Recipe.kt
enum class RecipeCuisine { COLOMBIAN, ITALIAN, ASIAN, MEXICAN, MEDITERRANEAN }

data class Recipe(/* … */ val cuisine: RecipeCuisine = RecipeCuisine.COLOMBIAN)

// …/feature/recipes/domain/model/MealPlan.kt
data class PlanContext(/* … */ val cuisines: Set<RecipeCuisine> = emptySet())

sealed interface PlanSuggestion {
    /** Quitando las cocinas favoritas sí habría plan. */
    data object RemoveCuisine : PlanSuggestion
}

// …/feature/recipes/domain/model/AiPlan.kt
data class AiContext(/* … */ val cuisines: Set<Cuisine> = emptySet())
```

### 2.3 DTO

`RecetaDto.cocina: String` no tiene valor por defecto: si falta, la lectura falla. `cocinaARecipeCuisine(cocina, id)` copia
el patrón de `etiquetaAFoodTag` y lanza `IllegalArgumentException` si el valor es desconocido.

## 3. Agente

`MealPlanner.search` agrega el filtro de una sola receta junto a `fitsDiet`:

```kotlin
fun fitsCuisine(recipe: Recipe, context: PlanContext): Boolean =
    context.cuisines.isEmpty() || recipe.cuisine in context.cuisines
```

`suggestions` agrega `RemoveCuisine` si `context.cuisines` no está vacío y `search` encuentra plan con
`context.copy(cuisines = emptySet())`. `RaiseBudget` y `AllowMoreTime` conservan las cocinas.

**No cambian** la utilidad, la cota de la poda, `RecipeEvaluator` ni `PlanExplainer`.

## 4. Flujo de la IA

- `GetAiContextUseCase.build`: lee `cuisines` de las mismas preferencias que `restrictions`. Si no se pueden leer, queda vacío.
- `GeneratePlanUseCase`: `cuisines = context.cuisines.mapTo(LinkedHashSet()) { RecipeCuisine.valueOf(it.name) }`.
- `AiUiState.cuisines`, `AiPlanViewModel.showContext` y `AiFlowScreen`: línea debajo de "Según tus preferencias", con
  `Cuisine.titleRes` de `FoodPreferencesText.kt` y `recipes_ai_cuisines`.
- `AiReasonText.kt`: `PlanSuggestion.RemoveCuisine` con `recipes_ai_suggest_cuisine`.

## 5. Catálogo

- **Precios:** búsquedas en `tools/precios/busquedas.json`, tabla en `docs/precios/precios.json` y `aplicar.py`, como dice
  `tools/precios/README.md`.
- **Fotos:** Wikimedia Commons, revisadas a ojo y con licencia comprobada. Webp de 800 px con la misma calidad de las actuales.
- **`tools/precios/revisar_catalogo.py`:** se corre para las recetas nuevas. Lo que señale se revisa a mano.

## 6. Decisiones (ADR)

| Decisión | Alternativa | Por qué |
|---|---|---|
| Restricción dura | Preferencia blanda en la utilidad | Decisión de Zhair. Es más simple de explicar y no cambia los pesos ya justificados. |
| Enum propio en recetas | Usar `Cuisine` del Perfil en el agente | Mismo patrón de `DietRule`; el agente no depende de otra feature. |
| `cocina` obligatorio en el JSON | Valor por defecto | Una receta sin clasificar sería una falla silenciosa del filtro. |
| Sugerencia solo de texto | Botón que quite la cocina | Las sugerencias del agente ya son texto; un botón es otra feature. |

## 7. Estrategia de pruebas

- `RecipeCatalogTest`: 70 recetas, cocinas válidas, 5 nuevas por cocina como mínimo, fotos y licencias.
- `RecipeCatalogDtoTest` o la prueba del DTO existente: valor desconocido y campo ausente.
- `MealPlannerTest`: filtro, varias cocinas, dieta más cocina, sugerencias y fuerza bruta con el filtro.
- `GetAiContextUseCaseTest` y `GeneratePlanUseCaseTest`: las cocinas llegan al agente y un fallo de lectura no filtra.
- `AiPlanViewModelTest` y `AiFlowScreenTest`: línea de cocinas y sugerencia.
- `AgentEvaluationTest`, `MealPlannerPerformanceTest` y `AlgorithmComparisonTest` con el catálogo nuevo.
