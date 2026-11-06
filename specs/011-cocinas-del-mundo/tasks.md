# Tareas 011: Cocinas del mundo

> Rama `feature/011-cocinas-del-mundo`, un commit por tarea. Contratos en [`plan.md`](./plan.md).

- [x] T001 La spec, el plan, las tareas y sus referencias en la spec 009 §6, `AGENTS.md`, `README.md` y `tools/ca_coverage.py`.
- [x] T002 **Cocina en el catálogo** (RF-COC-01, CA-COC-01-02): `RecipeCuisine`, `Recipe.cuisine`, `RecetaDto.cocina`,
  `cocinaARecipeCuisine`, y clasificar las 50 recetas actuales en `recetas.json`.
- [x] T003 **Ingredientes y precios** (CA-COC-01-04): `busquedas.json`, `docs/precios/precios.json`, `ingredientes.json` y
  la tabla del mes con `aplicar.py`.
- [x] T004 **20 recetas nuevas** (CA-COC-01-01): `recetas.json`, revisadas con `revisar_catalogo.py`.
- [ ] T005 **Fotos** (CA-COC-01-03): `drawable-nodpi/receta_*.webp` y `docs/licenses/fotos-recetas.md`.
- [ ] T006 **Agente** (RF-COC-02, RF-COC-04, HU-COC-02, CA-COC-03-02, 03-03): `PlanContext.cuisines`, `fitsCuisine`,
  `PlanSuggestion.RemoveCuisine`, `GetAiContextUseCase` y `GeneratePlanUseCase`.
- [ ] T007 **Flujo de la IA** (CA-COC-03-01, 03-04): `AiUiState`, `AiPlanViewModel`, `AiFlowScreen`, `AiReasonText` y textos.
- [ ] T008 **Evaluación** (RNF-COC-02): repetir las pruebas del agente y agregar una sección fechada en
  `docs/ia/evaluacion-agente.md` y `docs/ia/sustentacion-agente.md`.

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
| CA-COC-03-04 | `GetAiContextUseCaseTest` |

### Checklist de verificación manual

- [ ] Elegir Italiana en el Perfil: el plan solo trae recetas italianas y el contexto muestra "Cocina favorita: Italiana".
- [ ] Elegir Italiana y Mexicana: el plan mezcla solo esas dos.
- [ ] Sin cocinas: el plan puede traer cualquier cocina.
- [ ] Vegano más una cocina con presupuesto bajo: sin plan, aparece "Quita tu cocina favorita en el Perfil".
- [ ] Las 20 recetas nuevas abren con su foto en Recetas.
