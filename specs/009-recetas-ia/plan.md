# Plan técnico 009: Recetas + IA

> Cómo se cumple la [spec](./spec.md). Escrito para seguirlo paso a paso (también con OpenCode + DeepSeek): cada
> sección dice qué archivo se crea, qué código existente se copia como modelo y qué no se toca.
> Ruta base: `app/src/main/java/com/smartfood/app/` (abreviada `…/`).

## 1. Arquitectura

```
feature/recipes/
├── domain/
│   ├── model/        Recipe, RecipeIngredient, RecipeStep, Ingredient, PlanContext, MealPlan, PlannedDay,
│   │                 IngredientUse, PlanExplanation, PlanResult (Found | NoPlan), CookingRecord
│   ├── agent/        ── el agente de IA (Kotlin puro, sin Android) ──
│   │   ├── IngredientMatcher.kt      inventario ↔ ingredientes del catálogo (nombre normalizado + sinónimos, unidades)
│   │   ├── RecipeEvaluator.kt        uso de inventario, faltantes, costo y utilidad de una receta o un plan
│   │   ├── MealPlanner.kt            CSP: backtracking + forward checking + ramificación y poda
│   │   └── PlanExplainer.kt          "¿Por qué te conviene?" a partir del plan
│   ├── repository/   RecipeCatalog, FavoritesRepository, CookingHistoryRepository
│   └── usecase/      GetRecipes, SearchRecipes, RankRecipesForMe, GeneratePlan, GetRecipeDetail,
│                     ToggleFavorite, ObserveFavorites, FinishCooking, GetMonthlySavings
├── data/
│   ├── catalog/      AssetsRecipeCatalog (lee assets/recetas.json e ingredientes.json con kotlinx.serialization)
│   ├── repository/   FirestoreFavoritesRepository, FirestoreCookingHistoryRepository
│   └── di/           RecipesDataModule
└── ui/
    ├── discover/     Descubrir (pestaña Recetas)
    ├── ai/           Contexto, Cargando, Plan, Sin coincidencias, Ayuda (un ViewModel compartido: AiPlanViewModel)
    ├── detail/       Detalle de receta
    ├── cooking/      Antes de empezar y Cocina guiada (temporizador)
    ├── saved/        Guardadas
    └── RecipesNavGraph.kt
app/src/main/assets/recetas.json, ingredientes.json
```

Modelos a copiar: estructura de `feature/inventory` (contratos `XContract.kt`, `XViewModel`, `XScreen` sin estado,
`XNavGraph`), `FirestoreInventoryRepository` para Firestore con `Map`, `InventoryDataModule` para la DI.

## 2. Catálogo (`assets/`)

```jsonc
// ingredientes.json
[{ "id": "pollo", "nombre": "Pollo", "sinonimos": ["pechuga", "muslo", "pollo entero"],
   "seccion": "Carnes", "unidad": "g", "precioCop": 14500, "precioPor": 1000, "vidaUtilDias": 2 }]
// recetas.json
[{ "id": "pollo-guisado", "nombre": "Pollo guisado con arroz", "foto": "receta_pollo_guisado",
   "porciones": 4, "prepMin": 15, "coccionMin": 25, "reposoMin": 0, "dificultad": "Media",
   "picante": false, "kcal": 520,
   "ingredientes": [{ "id": "pollo", "cantidad": 200 }, { "id": "tomate", "cantidad": 3 }, …],
   "antesDeEmpezar": ["Lava y corta el tomate", "Ten una olla mediana lista"],
   "pasos": [{ "titulo": "Sofríe la cebolla y el tomate", "texto": "Cocina a fuego medio…", "minutos": 5 }] }]
```

- `unidad` ∈ `g`, `ml`, `und`. `precioCop` es el precio de `precioPor` unidades (p. ej. 1000 g).
- Las fotos van en `res/drawable-nodpi/receta_*.webp` (≤ 150 KB cada una); su fuente, en `docs/licenses/fotos-recetas.md`.
- Contenido: 30 a 50 recetas de cocina colombiana cotidiana. El catálogo se valida con una prueba unitaria
  (`RecipeCatalogTest`): ids únicos, cada ingrediente existe, cantidades > 0, al menos un paso.

## 3. El agente

### 3.1 Emparejamiento (`IngredientMatcher`)
1. Normalizar: minúsculas, sin tildes, sin espacios dobles (reutilizar `SearchInventoryUseCase.normalize` de la
   spec 006).
2. Un producto del inventario corresponde al ingrediente si su nombre normalizado **contiene** el nombre o algún
   sinónimo del ingrediente ("Leche Entera" → `leche`). Si varios ingredientes coinciden, gana el texto más largo.
3. Convertir cantidades a la unidad del ingrediente: kg→g ×1000, lb→g ×453,6, l→ml ×1000; `UNITS`→`und`. Si las
   unidades no son compatibles (g vs und), el producto cuenta como "suficiente para una receta" (spec §7).

### 3.2 Evaluación (`RecipeEvaluator`)
Para una receta escalada a `porciones`, con el inventario **restante**:
- `usado[i]` = mín(necesario, disponible); `faltante[i]` = necesario − usado; `costo` = Σ faltante·precio.
- `aprovechamiento` = ingredientes cubiertos por el inventario / ingredientes de la receta.
- `rescate(día d)` = Σ sobre productos que vencen en ≤ 3 días y `d ≤ díasParaVencer`: `usado · 1/(días+1)`, en
  unidades normalizadas (cada producto aporta su fracción usada, entre 0 y 1, para no favorecer las unidades grandes).
- `desperdicio(plan)` = Σ fracción no usada de los productos que vencen en ≤ 3 días.
- `U(plan) = 1,0·Σ rescate − 0,3·(costo/10.000) − 0,5·desperdicio` (pesos en `PlannerWeights`, un `data class` con
  valores por defecto, para la prueba de sensibilidad).

### 3.3 Búsqueda (`MealPlanner.plan(context): PlanResult`)
```
dominio = recetas que cumplen tiempo ≤ máx, picante permitido y faltantes ≤ 3 con el inventario completo
mejor = null
fun buscar(día, asignadas, inventarioRestante, presupuestoRestante, utilidadParcial):
    si día == 4: si utilidad > mejor.utilidad → mejor = plan; return
    candidatas = dominio − asignadas, filtradas (forward checking) por: costo con inventarioRestante ≤ presupuestoRestante
                 y faltantes ≤ 3
    si candidatas vacía: return                                  // poda por factibilidad
    ordenar candidatas por utilidad de un paso, de mayor a menor   // primero las prometedoras
    para cada r en candidatas:
        cota = utilidadParcial + U(r, día) + cotaOptimista(días restantes)
        si mejor != null y cota ≤ mejor.utilidad: continue      // ramificación y poda
        buscar(día+1, asignadas+r, restar(inventarioRestante, r), presupuestoRestante − costo(r), …)
return mejor?.let(::Found) ?: NoPlan(sugerencias)
```
- `cotaOptimista` = suma de los mayores `U` de un paso entre las candidatas restantes (nunca subestima).
- **Determinismo:** desempates por id de receta.
- **Sin plan:** `NoPlan` calcula sugerencias: el presupuesto mínimo con el que existiría un plan (repitiendo la
  búsqueda sin la restricción de presupuesto y tomando el costo del mejor plan) y si quitar "Sin picante" o subir el
  tiempo daría plan.
- `PlanExplainer` arma las frases de CA-RIA-02-06 a partir de los `IngredientUse` del plan.

### 3.4 Pruebas del agente
- `MealPlannerTest`: restricciones (tiempo, picante, distintas, presupuesto, faltantes) y **optimalidad contra fuerza
  bruta** en catálogos pequeños aleatorios con semilla fija (CA-RIA-02-04, 02-05).
- `IngredientMatcherTest` (tabla de casos), `RecipeEvaluatorTest`, `PlanExplainerTest`.
- `AgentEvaluationTest` (RNF-RIA-03): 30 inventarios sintéticos con semilla fija; compara contra
  `BaselineByMatches` y `BaselineByExpiry` e imprime la tabla de métricas para el informe de IA.
- `MealPlannerPerformanceTest`: 50 recetas, ≤ 2 s en la JVM (orientativo; la medición real se hace en el teléfono).

## 4. Datos en Firestore

| Ruta | Campos | Reglas |
|---|---|---|
| `users/{uid}/favorites/{recipeId}` | `savedAt` (timestamp del servidor) | dueño; solo ese campo |
| `users/{uid}/cookings/{id}` | `recipeId` (string), `cookedAt` (timestamp del servidor), `rescuedValueCop` (int ≥ 0) | dueño; crear y leer, no editar |

Se escriben con `Map`, como el inventario. Funcionan sin conexión gracias a la caché de Firestore (CA-RIA-05-03).
"Ahorrados este mes" = Σ `rescuedValueCop` de `cookings` con `cookedAt` en el mes actual; `rescuedValueCop` =
valor de referencia de los ingredientes por vencer usados.

## 5. Pantallas y navegación

- `MainTab.RECIPES` y `MainTab.SAVED` pasan a `enabled = true` (`core/navigation/MainTabScaffold.kt`); rutas nuevas
  en `Routes.kt`: `Recipes`, `Saved`, `AiContext`, `AiPlan`, `AiHelp`, `RecipeDetail(id)`, `CookingIntro(id)`,
  `Cooking(id)`; registradas en `RecipesNavGraph.kt` dentro de `mainGraph`.
- El flujo de la IA (contexto → cargando → plan / sin coincidencias) usa **un solo** `AiPlanViewModel` con
  `AiUiState` (`Context`, `Loading`, `Plan`, `NoPlan`) para no pasar el plan entre rutas. El cálculo corre en
  `Dispatchers.Default` y se cancela con "Cancelar".
- Componentes a reutilizar: la barra de búsqueda de `InventorySearchScreen`, `CategoryChips` para los
  filtros, tarjetas con `AsyncImage`, `SmartFoodPrimaryButton`, `SmartFoodResultDialog`/`SmartFoodWarningDialog`,
  el arco "Progreso" como `CircularProgressIndicator` con los colores del Figma.
- Temporizador: `CookingViewModel` con un `Job` de `delay(1000)`; al llegar a 0, `RingtoneManager` (notificación por
  defecto) y `Vibrator`. Sin servicio en primer plano: si la app se cierra, el temporizador se detiene (documentado).
- "Contactar soporte": `Intent.ACTION_SENDTO` con `mailto:` al correo del equipo.

## 6. Descuento del inventario (`FinishCookingUseCase`)

1. Para cada `IngredientUse` del producto `p`: si `p` no tiene cantidad o le queda ≤ 0 → `DeleteItemUseCase(p.id)`;
   si no → `UpdateItemUseCase` con la cantidad restante en su unidad original.
2. Guardar `cookings/{id}` con el valor rescatado.
3. Todo con la cola sin conexión del inventario (spec 006 §13); el resultado se muestra con "Guardado".

## 7. Decisiones (ADR)

| Decisión | Alternativas | Motivo |
|---|---|---|
| CSP de plan de 3 días con backtracking + forward checking + ramificación y poda | Filtrar y ordenar; CSP por receta; aprendizaje | Pedido del profesor (CSP, no filtro) y decisión de Zhair. |
| Agente en el teléfono (Kotlin puro) | Cloud Function / API de IA externa | Sin costo, sin conexión, determinista y probable con pruebas unitarias. |
| Catálogo en `assets` | Firestore | Decisión de Zhair: sin conexión y revisable en el PR. |
| Emparejar por nombre + sinónimos | Lista cerrada en el inventario | Decisión de Zhair: no cambia la spec 006. |
| Pesos fijos en `PlannerWeights` | Pesos aprendidos | No hay datos de uso aún (Avance 2). |

## 8. Estrategia de pruebas

- **Unitarias:** las del agente (§3.4), `RecipeCatalogTest`, casos de uso (`FinishCookingUseCaseTest`,
  `GetMonthlySavingsUseCaseTest`, `ToggleFavoriteUseCaseTest`) y un `…ViewModelTest` por pantalla (`AiPlanViewModel`
  con `Context → Loading → Plan/NoPlan`, cancelar, ajustar).
- **Instrumentadas** (con `PantallaEnFlujo`): `RecipesDiscoverScreenTest`, `AiFlowScreenTest`,
  `RecipeDetailScreenTest`, `CookingScreenTest`, `SavedRecipesScreenTest`; `MainNavigationTest` para las pestañas.
- **Reglas:** `rules-test/firestore.test.js` para `favorites` y `cookings`.
- **Manual:** checklist de `tasks.md`, con el tiempo del plan medido en el teléfono (release).
