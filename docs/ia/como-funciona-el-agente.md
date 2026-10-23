# Cómo funciona el agente de SmartFood

El agente arma un **plan de 3 días** (una receta por día) con lo que hay en tu inventario, gastando primero lo que está por vencer y sin pasarse del presupuesto. Resuelve un **CSP con optimización**: no solo busca un plan que cumpla las reglas, busca **el mejor** de todos los que las cumplen.

Los resultados de la evaluación (comparación con las líneas base y tiempos) están en [`evaluacion-agente.md`](evaluacion-agente.md).

---

## 1. El pipeline (de punta a punta)

```
Persona toca "Generar recomendaciones"
        │
        ▼
1. PERCIBIR    GetAiContextUseCase
               - Lee el inventario (productos, cantidades, fechas).
               - Empareja cada producto con un ingrediente del catálogo (IngredientMatcher).
               - Lee las restricciones del Perfil (Vegano, Sin gluten…) y la tienda elegida.
        │
        ▼
2. CONTEXTO    Lo que la persona eligió: presupuesto, porciones, tiempo máximo, "Sin picante".
               Todo se junta en un PlanContext.
        │
        ▼
3. DECIDIR     GeneratePlanUseCase → MealPlanner.plan()
               a) Arma el dominio (filtra recetas que no sirven).
               b) Busca con backtracking + forward checking + ramificación y poda.
               c) Se queda con el plan de mayor utilidad.
        │
        ├── Hay plan    → 4a. EXPLICAR  PlanExplainer: "¿Por qué te conviene?"
        │
        └── No hay plan → 4b. SUGERIR   "Sube el presupuesto a COP $X", "Permite picante"…
        │
        ▼
5. ACTUAR      La pantalla muestra "Tu plan de 3 días" o "Sin coincidencias".
```

El cálculo corre fuera del hilo principal (`Dispatchers.Default`) para que la pantalla no se trabe. En el teléfono tarda entre 30 y 62 ms.

---

## 2. Las piezas del CSP

### Variables

| Variable | Qué representa |
|---|---|
| `D1` | La receta del Día 1 |
| `D2` | La receta del Día 2 |
| `D3` | La receta del Día 3 |

En el código, el número de días es `PlanContext.days` (vale 3). Los días se asignan **en orden**: primero `D1`, luego `D2`, luego `D3`, porque lo que cocinas el Día 1 cambia lo que queda para el Día 2.

### Dominio

El dominio de cada variable son **las recetas del catálogo (50) que sirven por sí solas**. Antes de buscar, se quitan las que nunca podrían entrar al plan. A esto se le llama **consistencia de nodo**:

| Se quita la receta si… | Cómo se identifica en el código |
|---|---|
| Tarda más que el tiempo máximo | `it.totalMinutes <= context.maxMinutes` |
| Es picante y la persona eligió "Sin picante" | `!(context.noSpicy && it.spicy)` |
| Algún ingrediente está prohibido por la dieta (por ejemplo, carne para Vegano) | `fitsDiet(it, context)`: compara las etiquetas del ingrediente (`carne`, `origen_animal`, `gluten`…) con lo que prohíbe cada `DietRule` |
| Sola, ya cuesta más que el presupuesto | `evaluate(...).costCop <= context.budgetCop` |

Si después del filtro quedan menos de 3 recetas, no hay plan posible y se pasa directo a las sugerencias.

### Restricciones

**De una sola receta** (unarias): son las del dominio, de la tabla anterior.

**Entre recetas** (binarias o globales): dependen de lo que ya se eligió para los otros días.

| Restricción | Regla | Cómo se identifica en el código |
|---|---|---|
| Todas distintas | `D1 ≠ D2 ≠ D3` | `it.id !in usedIds` |
| Presupuesto total | `costo(D1) + costo(D2) + costo(D3) ≤ presupuesto` | `it.evaluation.costCop <= budgetLeft` (se resta lo ya gastado) |
| Máximo 3 ingredientes por comprar | Cada receta puede necesitar **como mucho 3** cosas que no hay en casa | `it.evaluation.missingCount <= context.maxMissing` |
| Inventario compartido | Lo que usa el Día 1 ya no está para el Día 2 | Cada día se evalúa con el inventario **que quedó** (`stock` → `remaining`) |

Las compras se hacen **por empaques completos** (si falta 1 tomate, se compra la bolsa de 1 kg) y **lo que sobra queda para los días siguientes** sin volver a pagarlo. Los básicos de despensa (aceite, sal, comino, pimienta, azúcar) no se cobran.

### Objetivo (función de utilidad)

Entre todos los planes que cumplen las restricciones, el agente elige el de **mayor utilidad**:

```
U = 1,0 · rescate  −  0,3 · (costo / 10.000)  −  0,5 · desperdicio
```

| Término | Qué mide | Por qué |
|---|---|---|
| **Rescate** | Cuánto de lo que vence en 3 días o menos se usa antes de vencer, ponderado por urgencia `1/(días + 1)`: lo que vence hoy vale 1, mañana 0,5 y en 3 días 0,25 | Premia usar primero lo más urgente |
| **Costo** | Lo que hay que comprar, en decenas de miles de pesos | Castiga gastar de más |
| **Desperdicio** | Lo que vence en 3 días o menos y el plan **no** usa | Castiga dejar comida que se va a perder |

Los pesos (1,0, 0,3 y 0,5) están en `PlannerWeights`.

---

## 3. El algoritmo exacto

Es **backtracking** (búsqueda en profundidad que asigna un día a la vez y retrocede) con dos mejoras:

1. **Forward checking:** en cada día solo se prueban las recetas que **todavía** cumplen con el inventario y el presupuesto que quedan. Las demás ni se intentan.
2. **Ramificación y poda (branch and bound):** si una rama, en el mejor de los casos, no puede superar al mejor plan ya encontrado, se descarta sin explorarla.

### Pseudocódigo

```
dominio ← recetas que cumplen tiempo, picante, dieta y presupuesto (consistencia de nodo)
si |dominio| < 3: devolver "sin plan"

mejor ← ninguno

asignar(día, elegidas, inventario, presupuestoQueQueda, utilidadParcial):
    si día > 3:                                   # ya hay receta para los 3 días
        U ← utilidadParcial − 0,5 · desperdicio(inventario)
        si U > mejor.U  (o empata y los ids van antes): mejor ← este plan
        volver

    # FORWARD CHECKING: solo las recetas que todavía caben
    candidatas ← recetas del dominio que:
                   - no están en elegidas
                   - necesitan ≤ 3 ingredientes por comprar con el inventario que queda
                   - cuestan ≤ presupuestoQueQueda
    si |candidatas| < días que faltan: volver     # poda por factibilidad

    ordenar candidatas de mayor a menor utilidad (las más prometedoras primero)

    para cada receta r en candidatas:
        # RAMIFICACIÓN Y PODA
        cota ← utilidadParcial + U(r) + (mejores rescates posibles de los días que faltan)
        si cota < mejor.U: saltar r               # esta rama no puede ganar

        asignar(día + 1, elegidas + r, inventario − lo que usa r,
                presupuestoQueQueda − costo(r), utilidadParcial + U(r))

asignar(1, [], inventario completo, presupuesto, 0)
devolver mejor
```

**Por qué la poda no pierde el mejor plan:** la cota es optimista. Para los días que faltan suma el mejor rescate posible y no resta costo ni desperdicio, así que **nunca subestima**. Si ni así alcanza, esa rama de verdad no puede ganar. Las pruebas lo comprueban contra una **búsqueda exhaustiva (fuerza bruta)** que revisa todas las combinaciones.

**Por qué siempre da el mismo resultado:** si dos planes empatan en utilidad, gana el que tiene los ids de receta menores en orden alfabético.

### ¿Y si no hay plan?

El agente no solo dice "no hay": calcula qué cambiar, repitiendo la misma búsqueda con una restricción relajada:

| Sugerencia | Cómo se calcula |
|---|---|
| "Sube el presupuesto a COP $X" | Busca el plan **más barato** sin límite de presupuesto (pesos que solo miran el costo) y redondea su costo a miles |
| "Permite recetas picantes" | Repite la búsqueda sin "Sin picante"; si así hay plan, la sugiere |
| "Sube el tiempo a N min" | Repite sin tiempo máximo y sugiere el tiempo de la receta más larga del plan |
| "Agrega más productos a tu inventario" | Si nada de lo anterior funciona |

**Nunca** sugiere quitar las restricciones alimentarias: son de la persona.

---

## 4. ¿Dónde está el código?

Todo el agente vive en **`app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/`** (Kotlin puro, sin Android).

| Qué | Archivo | Dónde mirar |
|---|---|---|
| **El algoritmo CSP** | [`MealPlanner.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt) | `plan()` línea 43 · dominio (consistencia de nodo) en `search()`, líneas 58–70 · backtracking `assign()` línea 87 · forward checking, líneas 112–119 · ramificación y poda, líneas 123–131 · sugerencias `suggestions()` línea 170 |
| Evaluar una receta (uso del inventario, compras por empaque, costo, rescate, utilidad) | [`RecipeEvaluator.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/RecipeEvaluator.kt) | `evaluate()` línea 59 · compra por empaques en `take()`, línea 101 · rescate `rescueOf()` línea 151 · desperdicio `waste()` línea 88 |
| Explicación "¿Por qué te conviene?" | [`PlanExplainer.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/PlanExplainer.kt) | `explain()` línea 47 |
| Emparejar el inventario con el catálogo | [`IngredientMatcher.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/IngredientMatcher.kt) | `match()` línea 34 |
| Modelos: `PlanContext`, `DietRule`, `PlannerWeights`, `MealPlan`, `PlanResult` | [`MealPlan.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/model/MealPlan.kt) | — |
| Percibir el contexto (inventario, Perfil, tienda) | [`GetAiContextUseCase.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/usecase/GetAiContextUseCase.kt) | `invoke()` |
| Llamar al agente fuera del hilo principal | [`GeneratePlanUseCase.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/usecase/GeneratePlanUseCase.kt) | línea 47 |
| La pantalla que lo usa | [`AiPlanViewModel.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiPlanViewModel.kt) | `generate()` |
| Catálogo: 50 recetas y sus ingredientes con precios y etiquetas | [`recetas.json`](../../app/src/main/assets/recetas.json) y [`ingredientes.json`](../../app/src/main/assets/ingredientes.json) | — |

### Las pruebas que lo respaldan

| Prueba | Qué demuestra |
|---|---|
| [`MealPlannerTest`](../../app/src/test/java/com/smartfood/app/feature/recipes/domain/agent/MealPlannerTest.kt) | Cada restricción se cumple y el plan es **el óptimo**, comparado contra fuerza bruta en catálogos aleatorios |
| [`MealPlannerCatalogTest`](../../app/src/test/java/com/smartfood/app/feature/recipes/domain/agent/MealPlannerCatalogTest.kt) | Con el catálogo real hay plan para cada dieta |
| [`AgentEvaluationTest`](../../app/src/test/java/com/smartfood/app/feature/recipes/domain/agent/AgentEvaluationTest.kt) | El agente supera a las dos líneas base en 30 inventarios |
| [`MealPlannerPerformanceTest`](../../app/src/test/java/com/smartfood/app/feature/recipes/domain/agent/MealPlannerPerformanceTest.kt) | Con 50 recetas tarda menos de 2 s |

---

## 5. Por qué es un CSP y no "filtrar y ordenar"

Si solo se ordenaran las recetas y se tomaran las 3 mejores, podría pasar esto:

- Las 3 "mejores" usan el mismo pollo que vence mañana, pero solo hay pollo para una.
- Juntas se pasan del presupuesto, aunque cada una por separado cabe.
- Cada una compra su propia bolsa de tomate, cuando con una bolsa alcanzaba para las tres.

Las recetas **compiten por el mismo inventario y el mismo presupuesto**, así que la mejor receta de cada día por separado no da el mejor plan. Por eso hay que **buscar entre combinaciones** (50 × 49 × 48 ≈ 117.600 planes posibles) y podar las que no pueden ganar. En la evaluación, esa diferencia se nota: el agente aprovecha el **97,2 %** de lo que vence, frente al 77,2 % y el 80,6 % de las líneas base que eligen día por día sin buscar.
