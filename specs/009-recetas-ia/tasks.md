# Tareas 009: Recetas + IA

> Derivadas de [`plan.md`](./plan.md). Cada tarea es un commit en `feature/009-recetas-ia` (spec §10). Se marca `[x]`
> cuando cumple la Definition of Done de la [constitución](../constitution.md#7-definition-of-done).

## Fase 0: Preparación
- [ ] T001 La spec, el plan, las tareas y las capturas de `disenos-figma/Modulo 4 - Recetas + IA`.

## Issue 1: Catálogo, Descubrir y Detalle (intermedia)
- [x] T002 Modelos `Recipe`, `RecipeIngredient`, `RecipeStep`, `Ingredient` y contrato `RecipeCatalog` (RF-RIA-01). *Hecha en el #114, porque el agente los necesita; el #113 los reutiliza.*
- [x] T003 `assets/ingredientes.json` (con `"etiquetas"`, plan §2) y `assets/recetas.json` (30 a 50 recetas) con fotos en `drawable-nodpi` y sus fuentes en `docs/licenses/fotos-recetas.md`; `RecipeCatalogTest` (RNF-RIA-05).
- [x] T004 `AssetsRecipeCatalog` con kotlinx.serialization y `RecipesDataModule`.
- [x] T005 `IngredientMatcher` con `IngredientMatcherTest` (RF-RIA-02).
- [x] T006 Pestaña Recetas habilitada (Guardadas sigue gris, §9), rutas y `RecipesNavGraph` (CA-RIA-01-01).
- [x] T007 Pantalla Descubrir: búsqueda, filtros y tarjetas; "Para ti" con la utilidad de un paso (CA-RIA-01-01 a 01-04).
- [x] T008 Detalle de receta con porciones, ingredientes agrupados y "En tu inventario" (CA-RIA-04-01, 04-02); pruebas de las dos pantallas.

## Issue 2: Agente CSP y evaluación (avanzada) — solo `domain` y pruebas
- [x] T009 `PlanContext`, `MealPlan`, `PlannedDay`, `IngredientUse`, `PlanResult` y `PlannerWeights`.
- [x] T010 `RecipeEvaluator` (uso, faltantes, costo, rescate, desperdicio, utilidad) con `RecipeEvaluatorTest`.
- [x] T011 `MealPlanner` (backtracking + forward checking + ramificación y poda, sugerencias si no hay plan) con `MealPlannerTest` contra fuerza bruta (CA-RIA-02-04, 02-05, 02-07).
- [x] T012 `PlanExplainer` con `PlanExplainerTest` (CA-RIA-02-06).
- [x] T012a Restricciones alimentarias en el agente: `FoodTag`, `Ingredient.tags`, `DietRule` y `PlanContext.diet`; filtro del dominio en `MealPlanner` y casos en `MealPlannerTest`, incluida la fuerza bruta (CA-RIA-02-09).
- [x] T013 `AgentEvaluationTest` (30 inventarios, líneas base, métricas) y `MealPlannerPerformanceTest` (RNF-RIA-02, RNF-RIA-03). *Se corre con el catálogo real (`recetas.json` del #113), decisión de Zhair.*

## Issue 3: Flujo de la IA (intermedia) — después de los Issues 1 y 2
- [ ] T014 `GeneratePlanUseCase` (con las restricciones del Perfil como `PlanContext.diet`, CA-RIA-02-09) y `AiPlanViewModel` (`Context`, `Loading`, `Plan`, `NoPlan`, cancelar, ajustar) con pruebas.
- [ ] T015 Pantalla de contexto: inventario detectado, aviso del que vence primero, presupuesto, porciones, picante, tiempo y "Según tus preferencias" (CA-RIA-02-01).
- [ ] T016 "Analizando opciones" con el arco de progreso y "Cancelar" (CA-RIA-02-02).
- [ ] T017 "Tu plan de 3 días" con "¿Por qué te conviene?" y "Ajustar"; "Sin coincidencias" con sugerencias (CA-RIA-02-03, 02-06 a 02-08).
- [ ] T018 Ayuda y "Contactar soporte" (CA-RIA-03-01); `AiFlowScreenTest`.

## Issue 4: Cocina guiada (intermedia)
- [x] T019 "Antes de empezar" (sin video) y cocina guiada con pasos, "Anterior", "Siguiente" y "Salir" (CA-RIA-04-03, 04-04).
- [x] T020 Temporizador con sonido y vibración (CA-RIA-04-04).
- [x] T021 `CookingHistoryRepository` (`cookings`), reglas y `rules-test`; `FinishCookingUseCase` que descuenta el inventario y registra lo rescatado (CA-RIA-04-05).
- [x] T022 `GetMonthlySavingsUseCase` y tarjeta "Ahorrados este mes" del Perfil (CA-RIA-04-06).
- [x] T023 `CookingScreenTest` y pruebas de los casos de uso.

## Precios de mercado por tienda (Zhair, 2026-10-20)
- [x] T030 Presentación de mercado, precios por tienda y básicos en `ingredientes.json`; `harina-maiz` para las arepas; `Store`, `Ingredient.pack`/`prices`/`pantryBasic`; `RecipeCatalogTest` y `AssetsRecipeCatalogTest` (RF-RIA-01).
- [x] T031 `RecipeEvaluator` por empaques completos, sobrante para los días siguientes y básicos sin costo; `PlanContext.store` (CA-RIA-02-05).
- [x] T032 "Mi supermercado" en Preferencias alimentarias, `firestore.rules` y `rules-test` (spec 008, CA-PER-03-05).
- [x] T033 Detalle y Descubrir con los precios del supermercado elegido (CA-RIA-04-01, 04-07).

## Mejoras del catálogo propio (Zhair, 2026-10-20)
- [x] T034 Herramienta de precios en `tools/precios/` (`consultar.py`, `aplicar.py`, `busquedas.json`, README); `docs/precios/precios.json` como fuente de verdad.
- [x] T035 Revisión de calidad del catálogo (tiempos, kcal, cantidades por porción, pasos, sinónimos y fotos): se corrige lo claro y se lista lo dudoso.
- [x] T036 20 recetas nuevas (30 → 50; 7 veganas, 7 vegetarianas, 6 para todos) y 4 ingredientes (ahuyama, arveja, habichuela, champiñones) con precio y foto de licencia libre.
- [x] T037 Variedad con el catálogo real: ≥ 20 vegetarianas y ≥ 12 veganas en `RecipeCatalogTest`; `MealPlannerCatalogTest` arma un plan con cada restricción (CA-RIA-02-09).

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
| CA-RIA-01-03 | U `RankRecipesForMeUseCaseTest`, I `RecipesDiscoverScreenTest` |
| CA-RIA-01-04 | U `SearchRecipesUseCaseTest` |
| CA-RIA-02-01 | I `AiFlowScreenTest` |
| CA-RIA-02-02 | U `AiPlanViewModelTest`, I `AiFlowScreenTest` |
| CA-RIA-02-03 | U `AiPlanViewModelTest`, I `AiFlowScreenTest` |
| CA-RIA-02-04 | U `MealPlannerTest` (contra fuerza bruta) |
| CA-RIA-02-05 | U `MealPlannerTest` |
| CA-RIA-02-06 | U `PlanExplainerTest` |
| CA-RIA-02-07 | U `MealPlannerTest`, I `AiFlowScreenTest` |
| CA-RIA-02-08 | U `AiPlanViewModelTest` |
| CA-RIA-02-09 | U `MealPlannerTest` (contra fuerza bruta), U `MealPlannerCatalogTest`, U `GeneratePlanUseCaseTest` |
| CA-RIA-02-10 | U `MealPlannerPerformanceTest`, M (release) |
| CA-RIA-03-01 | I `AiFlowScreenTest` |
| CA-RIA-04-01 | I `RecipeDetailScreenTest` |
| CA-RIA-04-02 | U `RecipeDetailViewModelTest` |
| CA-RIA-04-03 | I `CookingScreenTest`, I `RecipeDetailScreenTest` |
| CA-RIA-04-04 | U `CookingViewModelTest`, I `CookingScreenTest`, I `TimerAlarmTest`, M |
| CA-RIA-04-05 | U `FinishCookingUseCaseTest`, U `CookingViewModelTest`, I `CookingScreenTest`, R |
| CA-RIA-04-06 | U `GetMonthlySavingsUseCaseTest`, U `FinishCookingUseCaseTest`, I `ProfileScreenTest` |
| CA-RIA-04-07 | U `RecipeDetailViewModelTest`, I `RecipeDetailScreenTest` |
| RNF-RIA-01 | U `MealPlannerTest` (mismo resultado dos veces) |
| RNF-RIA-02 | U `MealPlannerPerformanceTest`, M |
| RNF-RIA-03 | U `AgentEvaluationTest` |
| RNF-RIA-04 | R |
| RNF-RIA-05 | U `RecipeCatalogTest` (cada foto tiene fuente), revisión del PR |

### Checklist de verificación manual
1. Recetas: buscar y filtrar; "Para ti" pone primero lo que usa productos por vencer.
2. IA con presupuesto alto: aparece el plan de 3 días con su explicación en ≤ 2 s (release); también con "Vegano" en Preferencias y algunos básicos en el inventario (arroz, papa, cebolla, tomate): con el inventario vacío ninguna receta vegana cumple el máximo de 3 faltantes.
3. IA con COP $2.000: "Sin coincidencias" con el presupuesto sugerido; "Ajustar contexto" y "Ver recetas económicas".
4. "Sin picante" y 30 minutos: ninguna receta del plan los incumple.
5. Detalle: cambiar porciones; ingredientes "En tu inventario", "Básico de despensa" y el empaque con su precio; cambiar "Mi supermercado" en el Perfil y ver otros precios.
6. Cocinar una receta completa con temporizador; al terminar, el inventario baja y "Ahorrados este mes" sube.

## Próximo módulo (#117, no se implementa en la spec 009)
Decisión de Zhair del 2026-10-19: el ♡, la pestaña Guardadas y el contador "Recetas guardadas" del Perfil van en el próximo módulo, con su propia spec. Tareas previstas, sin numeración definitiva:
- `FavoritesRepository` (`favorites`), reglas y `rules-test`; `ToggleFavorite` y `ObserveFavorites`.
- Pestaña Guardadas con búsqueda, "Favoritas", "Recientes" y aviso sin conexión; `SavedRecipesScreenTest`.
- Tarjeta "Recetas guardadas" del Perfil.
