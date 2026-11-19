# Tareas 011: Cocinas del mundo

> Rama `feature/011-cocinas-del-mundo`, un commit por tarea. Contratos en [`plan.md`](./plan.md).

- [x] T001 La spec, el plan, las tareas y sus referencias en la spec 009 §6, `AGENTS.md`, `README.md` y `tools/ca_coverage.py`.
- [x] T002 **Cocina en el catálogo** (RF-COC-01, CA-COC-01-02): `RecipeCuisine`, `Recipe.cuisine`, `RecetaDto.cocina`,
  `cocinaARecipeCuisine`, y clasificar las 50 recetas actuales en `recetas.json`.
- [x] T003 **Ingredientes y precios** (CA-COC-01-04): `busquedas.json`, `docs/precios/precios.json`, `ingredientes.json` y
  la tabla del mes con `aplicar.py`.
- [x] T004 **20 recetas nuevas** (CA-COC-01-01): `recetas.json`, revisadas con `revisar_catalogo.py`.
- [x] T005 **Fotos** (CA-COC-01-03): `drawable-nodpi/receta_*.webp` y `docs/licenses/fotos-recetas.md`.
- [x] T006 **Agente** (RF-COC-02, RF-COC-04, HU-COC-02, CA-COC-03-02, 03-03): `PlanContext.cuisines`, `fitsCuisine`,
  `PlanSuggestion.RemoveCuisine`, `GetAiContextUseCase` y `GeneratePlanUseCase`.
- [x] T007 **Flujo de la IA** (CA-COC-03-01, 03-04): `AiUiState`, `AiPlanViewModel`, `AiFlowScreen`, `AiReasonText` y textos.
- [x] T008 **Evaluación** (RNF-COC-02): repetir las pruebas del agente y agregar una sección fechada en
  `docs/ia/evaluacion-agente.md` y `docs/ia/sustentacion-agente.md`.


## Correcciones después de la entrega (punto 5 de `docs/pendientes.md`)

- [x] T009 Enmienda de CA-COC-02-02, 02-03 y 03-02; CA-COC-02-06, 02-07 y 03-05 con su desviación; §8 de `docs/ia/evaluacion-agente.md`.
- [x] T010 **Agente:** `PlanContext.minCuisineRecipes`, búsqueda escalonada 3 → 2 → 1 en `MealPlanner.plan` y `PlanResult.Found.cuisineRecipes`; `MealPlannerTest` contra fuerza bruta y `MealPlannerPerformanceTest` (CA-COC-02-06, 02-07).
- [x] T011 **Aviso en el plan:** `AiPlanOutcome.Found.cuisineShortfall`, `AiUiState.cuisineNotice`, `AiReasonText` y `AiFlowScreen` (CA-COC-03-05).

## Matriz de trazabilidad

| CA | Prueba |
|---|---|
| CA-COC-01-01 | `RecipeCatalogTest` |
| CA-COC-01-02 | `RecipeCatalogTest`, `AssetsRecipeCatalogTest` |
| CA-COC-01-03 | `RecipeCatalogTest` |
| CA-COC-01-04 | `RecipeCatalogTest`, `aplicar.py --check` |
| CA-COC-02-01 a 02-05 | `MealPlannerTest` |
| CA-COC-03-01 | `AiPlanViewModelTest`, `AiFlowScreenTest` |
| CA-COC-03-02, 03-03 | `MealPlannerTest`, `AiFlowScreenTest` |
| CA-COC-02-06 | `MealPlannerTest` (contra fuerza bruta), `MealPlannerPerformanceTest`, `GeneratePlanUseCaseTest` |
| CA-COC-02-07 | `MealPlannerTest` (contra fuerza bruta) |
| CA-COC-03-04 | `GetAiContextUseCaseTest` |
| CA-COC-03-05 | `AiPlanViewModelTest`, `AiFlowScreenTest` |

### Checklist de verificación manual

- [x] Elegir Italiana en el Perfil: el plan solo trae recetas italianas y el contexto muestra "Cocina favorita: Italiana".
- [x] Elegir Italiana y Mexicana: el plan mezcla solo esas dos.
- [x] Sin cocinas: el plan puede traer cualquier cocina.
- [x] Vegano más una cocina con presupuesto bajo: sin plan, aparece "Quita tu cocina favorita en el Perfil".
- [x] Las 20 recetas nuevas abren con su foto en Recetas.
