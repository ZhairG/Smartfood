# Tareas 009: Recetas + IA

> Derivadas de [`plan.md`](./plan.md). Cada Issue es un commit en `feature/009-recetas-ia` (spec §10). Se marca `[x]`
> cuando cumple la Definition of Done de la [constitución](../constitution.md#7-definition-of-done).

## Fase 0: Preparación
- [ ] T001 La spec, el plan, las tareas y las capturas de `disenos-figma/Modulo 4 - Recetas + IA`.

## Issue 1: Catálogo, Descubrir y Detalle (intermedia)
- [ ] T002 Modelos `Recipe`, `RecipeIngredient`, `RecipeStep`, `Ingredient` y contrato `RecipeCatalog` (RF-RIA-01).
- [ ] T003 `assets/ingredientes.json` y `assets/recetas.json` (30 a 50 recetas) con fotos en `drawable-nodpi` y sus fuentes en `docs/licenses/fotos-recetas.md`; `RecipeCatalogTest` (RNF-RIA-05).
- [ ] T004 `AssetsRecipeCatalog` con kotlinx.serialization y `RecipesDataModule`.
- [ ] T005 `IngredientMatcher` con `IngredientMatcherTest` (RF-RIA-02).
- [ ] T006 Pestañas Recetas y Guardadas habilitadas, rutas y `RecipesNavGraph` (CA-RIA-01-01).
- [ ] T007 Pantalla Descubrir: búsqueda, filtros y tarjetas; "Para ti" con la utilidad de un paso (CA-RIA-01-01 a 01-04).
- [ ] T008 Detalle de receta con porciones, ingredientes agrupados y "En tu inventario" (CA-RIA-04-01, 04-02); pruebas de las dos pantallas.

## Issue 2: Agente CSP y evaluación (avanzada) — solo `domain` y pruebas
- [ ] T009 `PlanContext`, `MealPlan`, `PlannedDay`, `IngredientUse`, `PlanResult` y `PlannerWeights`.
- [ ] T010 `RecipeEvaluator` (uso, faltantes, costo, rescate, desperdicio, utilidad) con `RecipeEvaluatorTest`.
- [ ] T011 `MealPlanner` (backtracking + forward checking + ramificación y poda, sugerencias si no hay plan) con `MealPlannerTest` contra fuerza bruta (CA-RIA-02-04, 02-05, 02-07).
- [ ] T012 `PlanExplainer` con `PlanExplainerTest` (CA-RIA-02-06).
- [ ] T013 `AgentEvaluationTest` (30 inventarios, líneas base, métricas) y `MealPlannerPerformanceTest` (RNF-RIA-02, RNF-RIA-03).

## Issue 3: Flujo de la IA (intermedia) — después de los Issues 1 y 2
- [ ] T014 `GeneratePlanUseCase` y `AiPlanViewModel` (`Context`, `Loading`, `Plan`, `NoPlan`, cancelar, ajustar) con pruebas.
- [ ] T015 Pantalla de contexto: inventario detectado, aviso del que vence primero, presupuesto, porciones, picante y tiempo (CA-RIA-02-01).
- [ ] T016 "Analizando opciones" con el arco de progreso y "Cancelar" (CA-RIA-02-02).
- [ ] T017 "Tu plan de 3 días" con "¿Por qué te conviene?" y "Ajustar"; "Sin coincidencias" con sugerencias (CA-RIA-02-03, 02-06 a 02-08).
- [ ] T018 Ayuda y "Contactar soporte" (CA-RIA-03-01); `AiFlowScreenTest`.

## Issue 4: Cocina guiada (intermedia) — puede ir en paralelo con el 5
- [ ] T019 "Antes de empezar" (sin video) y cocina guiada con pasos, "Anterior", "Siguiente" y "Salir" (CA-RIA-04-03, 04-04).
- [ ] T020 Temporizador con sonido y vibración (CA-RIA-04-04).
- [ ] T021 `CookingHistoryRepository` (`cookings`), reglas y `rules-test`; `FinishCookingUseCase` que descuenta el inventario y registra lo rescatado (CA-RIA-04-05).
- [ ] T022 `GetMonthlySavingsUseCase` y tarjeta "Ahorrados este mes" del Perfil (CA-RIA-04-06).
- [ ] T023 `CookingScreenTest` y pruebas de los casos de uso.

## Issue 5: Guardadas (inicial) — puede ir en paralelo con el 4
- [ ] T024 `FavoritesRepository` (`favorites`), reglas y `rules-test`; `ToggleFavorite` y `ObserveFavorites` (CA-RIA-01-05).
- [ ] T025 Pestaña Guardadas con búsqueda, "Favoritas", "Recientes" y aviso sin conexión (CA-RIA-05-01, 05-03).
- [ ] T026 Tarjeta "Recetas guardadas" del Perfil (CA-RIA-05-02); `SavedRecipesScreenTest`.

## Cierre (en el mismo PR)
- [ ] T027 Suite instrumentada completa, `rules-test` y despliegue de reglas (con confirmación de Zhair).
- [ ] T028 Medición del tiempo del plan en el teléfono (release) y checklist manual; capturas Figma vs app en el PR.
- [ ] T029 Tabla de métricas de `AgentEvaluationTest` para el informe de IA; matriz, `README.md` y `AGENTS.md` al día.

## Matriz de trazabilidad

Leyenda: **U** = unitaria, **I** = instrumentada, **R** = `rules-test`, **M** = manual.

| Criterio | Pruebas |
|---|---|
| CA-RIA-01-01 | I `RecipesDiscoverScreenTest`, I `MainNavigationTest` |
| CA-RIA-01-02 | U `RankRecipesForMeUseCaseTest`, I `RecipesDiscoverScreenTest` |
| CA-RIA-01-03 | U `DiscoverViewModelTest` |
| CA-RIA-01-04 | U `SearchRecipesUseCaseTest` |
| CA-RIA-01-05 | U `ToggleFavoriteUseCaseTest`, R |
| CA-RIA-02-01 | I `AiFlowScreenTest` |
| CA-RIA-02-02 | U `AiPlanViewModelTest`, I `AiFlowScreenTest` |
| CA-RIA-02-03 | U `AiPlanViewModelTest`, I `AiFlowScreenTest` |
| CA-RIA-02-04 | U `MealPlannerTest` (contra fuerza bruta) |
| CA-RIA-02-05 | U `MealPlannerTest` |
| CA-RIA-02-06 | U `PlanExplainerTest` |
| CA-RIA-02-07 | U `MealPlannerTest`, I `AiFlowScreenTest` |
| CA-RIA-02-08 | U `AiPlanViewModelTest` |
| CA-RIA-02-09 | U `MealPlannerPerformanceTest`, M (release) |
| CA-RIA-03-01 | I `AiFlowScreenTest` |
| CA-RIA-04-01 | I `RecipeDetailScreenTest` |
| CA-RIA-04-02 | U `RecipeDetailViewModelTest` |
| CA-RIA-04-03 | I `CookingScreenTest` |
| CA-RIA-04-04 | U `CookingViewModelTest`, I `CookingScreenTest`, M |
| CA-RIA-04-05 | U `FinishCookingUseCaseTest`, R |
| CA-RIA-04-06 | U `GetMonthlySavingsUseCaseTest` |
| CA-RIA-05-01 | I `SavedRecipesScreenTest` |
| CA-RIA-05-02 | U `ProfileViewModelTest` |
| CA-RIA-05-03 | M |
| RNF-RIA-01 | U `MealPlannerTest` (mismo resultado dos veces) |
| RNF-RIA-02 | U `MealPlannerPerformanceTest`, M |
| RNF-RIA-03 | U `AgentEvaluationTest` |
| RNF-RIA-04 | R |
| RNF-RIA-05 | U `RecipeCatalogTest` (cada foto tiene fuente), revisión del PR |

### Checklist de verificación manual
1. Recetas: buscar, filtrar y marcar favoritas; "Para ti" pone primero lo que usa productos por vencer.
2. IA con presupuesto alto: aparece el plan de 3 días con su explicación en ≤ 2 s (release).
3. IA con COP $2.000: "Sin coincidencias" con el presupuesto sugerido; "Ajustar contexto" y "Ver recetas económicas".
4. "Sin picante" y 30 minutos: ninguna receta del plan los incumple.
5. Detalle: cambiar porciones; ingredientes "En tu inventario" y costos de lo que falta.
6. Cocinar una receta completa con temporizador; al terminar, el inventario baja y "Ahorrados este mes" sube.
7. Guardadas con y sin conexión; el Perfil cuenta las favoritas.
