# Cómo funciona el agente de SmartFood

**En una frase:** el agente mira lo que tienes en la nevera y arma un **plan de 3 días** (una receta por día) que
gasta primero lo que está por vencer, sin pasarse del presupuesto ni romper tu dieta.

Los resultados completos de la evaluación (líneas base, métricas y tiempos) están en
[`evaluacion-agente.md`](evaluacion-agente.md).

---

## 1. El problema, con un ejemplo

Este ejemplo se usa en todo el documento:

| En la nevera | Vence en | Urgencia `1/(días + 1)` |
|---|---|---|
| Pollo, 500 g | 1 día | 0,50 |
| Tomate, 4 unidades | 2 días | 0,33 |
| Arroz, 1 kg | 60 días | (no está por vencer) |

**Presupuesto:** COP $60.000 para los 3 días, con 4 porciones por receta.

**La idea fácil, que falla:** "ordeno las recetas de mejor a peor y tomo las 3 primeras". Las tres primeras pueden ser
*Arroz con pollo*, *Pollo guisado con arroz* y *Sopa de pollo*. Todas necesitan el mismo pollo, pero solo hay para una. Además,
cada una puede caber en el presupuesto por separado y pasarse cuando se suman.

**Lo que hace el agente:** piensa en los 3 días **a la vez**:

- **Día 1:** el pollo, porque vence mañana.
- **Día 2:** los tomates que quedan, porque vencen pasado mañana.
- **Día 3:** el arroz, que aguanta más.

Además, revisa que la suma de las compras quepa en los COP $60.000.

> Las recetas **compiten** por el mismo inventario y el mismo presupuesto. Por eso hay que buscar entre
> **combinaciones** y no solo ordenar recetas.

---

## 2. El agente en términos de IA (PEAS)

| | En SmartFood |
|---|---|
| **Tipo** | Agente **basado en utilidad**: entre todos los planes válidos, elige el que más vale |
| **Medida de desempeño** | Usar lo que vence antes de que se dañe, gastar poco y dejar poco desperdicio |
| **Entorno** | El inventario de la persona, su Perfil (dieta, tienda) y el catálogo de 50 recetas con precios reales |
| **Actuadores** | Muestra el plan y sus razones; al cocinar, descuenta el inventario y registra el ahorro |
| **Sensores** | Lee el inventario (Firestore), el Perfil y lo que la persona elige en pantalla |

El ciclo se cierra: lo que el agente hace al cocinar cambia el inventario que verá la próxima vez.

---

## 3. Cómo funciona: el pipeline

```
La persona toca "Generar recomendaciones"
   │
   ▼
① PERCIBIR      GetAiContextUseCase
   │            Lee el inventario, lo empareja con el catálogo (IngredientMatcher),
   │            lee la dieta del Perfil y la tienda.        →  AiContext
   ▼
② CONTEXTO      Lo que la persona eligió: presupuesto, porciones, tiempo, "Sin picante".
   │                                                         →  PlanContext
   ▼
③ DECIDIR       GeneratePlanUseCase → MealPlanner.plan()
   │            Busca el mejor plan de 3 días (el CSP).      →  PlanResult
   ▼
④ EXPLICAR      Si hay plan    → PlanExplainer: "¿Por qué te conviene?"
   o SUGERIR    Si no hay plan → "Sube el presupuesto a COP $X", "Permite picante"…
   │                                                         →  AiPlanOutcome
   ▼
⑤ MOSTRAR       AiPlanViewModel: "Tu plan de 3 días" o "Sin coincidencias".
   ▼
⑥ ACTUAR        Cocina guiada → FinishCookingUseCase
                Descuenta el inventario y guarda users/{uid}/cookings.
```

**En el ejemplo:** en ① el agente ve "pollo, vence en 1 día" y en ② recibe "COP $60.000 y 4 porciones". En ③
encuentra el plan, en ④ explica "usa el pollo que vence mañana el Día 1" y en ⑥, al cocinar, el pollo sale del
inventario.

El cálculo corre fuera del hilo principal (`Dispatchers.Default`) y en el teléfono tarda **entre 30 y 62 ms**.

---

## 4. El CSP en palabras simples

Un **CSP** (problema de satisfacción de restricciones) tiene tres partes: **variables** (lo que hay que decidir),
**dominio** (los valores posibles) y **restricciones** (las reglas). El nuestro tiene además un **objetivo**: no
busca cualquier plan válido, sino **el mejor**.

| Parte | Qué es | En el ejemplo |
|---|---|---|
| **Variables** | `D1`, `D2`, `D3`: la receta de cada día | `D1 = ?`, `D2 = ?`, `D3 = ?` |
| **Dominio** | Las 50 recetas, sin las que nunca podrían servir | Si eliges "Sin picante", el *Pollo picante con arroz* sale antes de empezar |
| **Restricciones** | Las reglas que debe cumplir el plan | Ver la tabla siguiente |
| **Objetivo** | La fórmula que dice qué plan es mejor | Ver "La fórmula de utilidad" |

### Las reglas (restricciones)

| Regla | Ejemplo |
|---|---|
| Las 3 recetas son distintas | No puede salir *Arroz con pollo* dos veces |
| El total cabe en el presupuesto | La suma de las compras de los 3 días es ≤ COP $60.000 |
| Máximo 3 ingredientes por comprar en cada receta | Una receta que pide 5 cosas que no tienes no entra |
| El inventario es compartido | Si el Día 1 usa todo el pollo, el Día 2 ya no lo tiene |
| La dieta se respeta siempre | Con "Vegano", ninguna receta lleva carne, huevo ni lácteos |
| Tiempo y picante, si los eligió | Con "≤ 30 min", no entra una receta de 45 min |

**Dos detalles reales de las compras:**
- Se compra **el empaque completo**. Si falta 1 cebolla, se compra la bolsa entera, y lo que sobra queda gratis para
  los días siguientes.
- Los **básicos de despensa** (aceite, sal, comino, pimienta, azúcar) no se cobran.

### La fórmula de utilidad

```
U = 1,0 · rescate  −  0,3 · (costo / 10.000)  −  0,5 · desperdicio
```

- **Rescate:** cuánto de lo que vence en 3 días o menos se usa, multiplicado por su urgencia (hoy vale 1; mañana 0,5;
  en 3 días 0,25).
- **Costo:** lo que hay que comprar, en decenas de miles de pesos.
- **Desperdicio:** lo que vence pronto y el plan **no** usa.

**Ejemplo (números ilustrativos):** el *Arroz con pollo* usa todo el pollo (1 × 0,50) y 2 de los 4 tomates
(0,5 × 0,33 = 0,17), y hay que comprar una cebolla de COP $3.000:

```
U(Arroz con pollo) = 1,0 · (0,50 + 0,17) − 0,3 · (3.000 / 10.000) = 0,67 − 0,09 = 0,58
```

Si al final del plan quedaran 2 tomates sin usar, se restaría `0,5 · 0,5 = 0,25` por desperdicio. Por eso el agente
prefiere un plan que **también** use esos tomates.

Los pesos están en `PlannerWeights` (`domain/model/MealPlan.kt`).

---

## 5. Cómo busca el mejor plan

Hay 50 × 49 × 48 ≈ **117.600** planes posibles. El agente no los revisa todos: usa tres técnicas.

| Técnica | Analogía | Qué hace |
|---|---|---|
| **Backtracking** | Armar un rompecabezas: pones una pieza y, si no encaja, la quitas y pruebas otra | Elige `D1`, después `D2` y después `D3`. Si un camino no sirve, vuelve atrás |
| **Forward checking** | Antes de elegir el Día 2, tachas lo que ya no cabe | En cada día solo prueba las recetas que **todavía** cumplen con el inventario y el presupuesto que quedan |
| **Ramificación y poda** | Si ya tienes un plan de 9/10 y este camino no puede pasar de 7, lo abandonas | Calcula lo **máximo** que podría dar una rama y, si no supera al mejor plan encontrado, no la explora |

### En el ejemplo (ilustrativo)

```
D1 = Arroz con pollo        (usa el pollo)     → quedan 2 tomates y el arroz
 ├─ D2 = Sancocho de pollo  ✗ forward checking: le faltan 4 ingredientes (máximo 3)
 ├─ D2 = Huevos pericos     (usa los tomates)  → queda el arroz
 │   └─ D3 = Arroz con verduras                → plan completo, U = 1,02  ← mejor hasta ahora
 └─ D2 = Macarrones con queso
     └─ ✂ poda: aun en el mejor caso no llega a 1,02, así que no se explora
```

### Pseudocódigo

```
dominio ← recetas que cumplen tiempo, picante, dieta y presupuesto por sí solas
si hay menos de 3: no hay plan

asignar(día, elegidas, inventario, presupuesto que queda):
    si ya hay 3 días: calcular U (restando el desperdicio) y guardar si es el mejor
    candidatas ← recetas no usadas, con ≤ 3 faltantes y que caben en el presupuesto   # forward checking
    ordenarlas de mayor a menor utilidad
    para cada candidata:
        si su mejor caso posible < mejor plan: saltarla                               # poda
        asignar(día + 1, elegidas + candidata, inventario − lo que usa, presupuesto − su costo)
```

**¿La poda puede perder el mejor plan?** No. El "mejor caso" de una rama es optimista: suma el mejor rescate posible
de los días que faltan y no resta costo ni desperdicio, así que **nunca se queda corto**. Si ni así alcanza, esa rama
no puede ganar. `MealPlannerTest` lo comprueba comparando con una búsqueda que revisa **todas** las combinaciones.

**¿Siempre da lo mismo?** Sí. Si dos planes empatan, gana el que tiene los ids de receta menores en orden alfabético.

### Si no hay plan

El agente no se queda en "no hay": repite la búsqueda aflojando **una** regla y sugiere qué cambiar.

| Sugerencia | Cómo la calcula |
|---|---|
| "Sube el presupuesto a COP $X" | Busca el plan **más barato** sin límite de presupuesto |
| "Permite recetas picantes" | Repite la búsqueda sin "Sin picante" |
| "Sube el tiempo a N min" | Repite sin tiempo máximo |
| "Agrega más productos a tu inventario" | Si nada de lo anterior funciona |

**Nunca** sugiere quitar la dieta: es una decisión de la persona.

---

## 6. ¿En qué se diferencia de *filtering*?

"Filtering" son las técnicas clásicas de los **sistemas de recomendación**. Las tres que se suelen comparar con el
agente son estas:

| | Filtrado simple (el de la app) | Filtrado basado en contenido | Filtrado colaborativo | **Agente CSP** |
|---|---|---|---|---|
| **Qué pregunta responde** | "¿Qué receta me sirve hoy?" | "¿Qué se parece a lo que me gustó?" | "¿Qué cocinaron personas como yo?" | "¿Qué cocino estos 3 días?" |
| **Qué datos usa** | Mi inventario y el catálogo | Las características de las recetas y mis gustos | El historial de muchos usuarios | Inventario, dieta, presupuesto y catálogo con precios |
| **Mira combinaciones** | No, una receta a la vez | No | No | **Sí**, los 3 días juntos |
| **Presupuesto total** | No, solo el costo de cada receta | No | No | **Sí**, es una regla |
| **Inventario compartido** | No | No | No | **Sí**, lo que usa el Día 1 no está el Día 2 |
| **Garantiza las reglas** | Solo las de cada receta | No | No | **Sí**, todas |
| **Explica por qué** | Poco ("usa tu pollo") | "Porque te gustó X" | "A otros les gustó" | Sí: qué rescata, qué compra y cuánto queda |
| **Necesita historial** | No | Sí, de la persona | Sí, de muchas personas | **No** |

### El mismo ejemplo resuelto por cada uno

- **Filtrado simple:** "Para ti" ordena *Arroz con pollo*, *Pollo guisado con arroz* y *Sopa de pollo* arriba, porque cada una
  rescata el pollo **por separado**. Sirve para elegir **una** receta hoy, pero no arma un plan.
- **Basado en contenido:** si te gustó el *Ajiaco sencillo*, sugiere el *Sancocho de pollo*, aunque no tengas los ingredientes y el
  pollo se dañe mañana.
- **Colaborativo:** "personas como tú cocinaron *Carne desmechada*". Necesita miles de usuarios con historial, y la app
  no los tiene (el llamado *arranque en frío*).
- **Agente CSP:** Día 1 *Arroz con pollo*, Día 2 *Huevos pericos*, Día 3 *Arroz con verduras*, todo dentro de los
  COP $60.000 y con la explicación de por qué.

### La app usa los dos

| Dónde | Qué usa | Para qué |
|---|---|---|
| Pantalla de recetas: "Para ti", "≤ 30 min", "Económicas" | **Filtrado simple** (`RankRecipesForMeUseCase`) | Explorar y elegir una receta rápido |
| "Sugerencia con IA" | **Agente CSP** (`MealPlanner`) | Planear 3 días sin botar comida |

> **En pocas palabras:** el filtering **ordena** recetas una por una. El agente **decide** un plan completo que
> cumple todas las reglas y es el mejor posible.

---

## 7. El código en 5 minutos

Todo el agente está en
[`feature/recipes/domain/agent/`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/), en
**Kotlin puro**: no usa Android ni Firebase, así que se prueba con pruebas unitarias. Estos son los archivos en el
orden en que se ejecutan.

### ① Percibir: `GetAiContextUseCase` ([código](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/usecase/GetAiContextUseCase.kt), línea 35)

```kotlin
val inventory = observeInventory().first().flatMap { it.items }     // inventario de Firestore
val pantry = matcher.match(inventory, ingredients)                  // "Pechuga de pollo" → ingrediente "pollo"
val restrictions = getFoodPreferences().getOrNull()?.restrictions.orEmpty()
```

[`IngredientMatcher.match()`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/IngredientMatcher.kt)
(línea 34) busca el nombre del ingrediente o un sinónimo dentro del nombre del producto (gana el texto más largo),
convierte la cantidad a la unidad base (kg → g, l → ml) y calcula los días para vencer.

### ② Armar el problema: `GeneratePlanUseCase` ([código](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/usecase/GeneratePlanUseCase.kt), línea 29)

```kotlin
withContext(Dispatchers.Default) {                                  // fuera del hilo de la pantalla
    val planContext = PlanContext(budgetCop, servings, maxMinutes, noSpicy, diet = context.diet, store = context.store)
    when (val result = MealPlanner(ingredients).plan(recipes, context.pantry, planContext)) {
        is PlanResult.Found  -> AiPlanOutcome.Found(result.plan, PlanExplainer().explain(result.plan, context.pantry))
        is PlanResult.NoPlan -> AiPlanOutcome.NoPlan(result.suggestions)
    }
}
```

### ③ Decidir: `MealPlanner` ([código](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt))

| Función | Línea | Qué hace |
|---|---|---|
| `plan()` | 43 | Busca el plan; si no hay, pide las sugerencias |
| `search()` | 58 | Arma el **dominio** (filtros de tiempo, picante, dieta y costo) y empieza la búsqueda |
| `assign()` | 87 | El **backtracking**: asigna un día y se llama a sí misma para el siguiente |
| Forward checking | 112–119 | Deja solo las recetas que caben con el inventario y el presupuesto que quedan |
| Poda | 130–131 | `if (bound < best.utility) continue`: salta las ramas que no pueden ganar |
| `suggestions()` | 170 | Qué cambiar cuando no hay plan |

```kotlin
// Forward checking (línea 112)
val candidates = domain.asSequence()
    .filter { it.id !in usedIds }                                   // recetas distintas
    .map { Assigned(it, evaluator.evaluate(it, context.servings, day, stock, context.store)) }
    .filter { it.evaluation.missingCount <= context.maxMissing && it.evaluation.costCop <= budgetLeft }
```

### ④ Evaluar una receta: `RecipeEvaluator` ([código](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/RecipeEvaluator.kt))

- `evaluate()` (línea 59): qué sale del inventario, qué hay que comprar, cuánto cuesta, cuánto rescata y su utilidad.
- `take()` (línea 101): primero usa lo que vence antes, después el sobrante de empaques ya comprados y, al final,
  compra empaques completos.
- `waste()` (línea 88): el desperdicio del plan.
- `PantryStock` (línea 188): el inventario **inmutable**. Cada consumo crea un estado nuevo, así el backtracking
  puede volver atrás sin deshacer nada.

### ⑤ Explicar: `PlanExplainer.explain()` ([código](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/PlanExplainer.kt), línea 47)

Devuelve una lista de razones en este orden: qué rescata y en qué día, cuántos ingredientes salen de la casa, qué
falta comprar, cuánto queda del presupuesto y qué no alcanza a usar. El texto lo pone
[`AiReasonText.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiReasonText.kt).

### ⑥ Mostrar y actuar

- [`AiPlanViewModel.generate()`](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiPlanViewModel.kt)
  (línea 102): pasa la pantalla de `Context` a `Loading` y luego a `Plan` o `NoPlan`, y escribe en el log cuánto tardó
  el agente (`adb logcat -s SmartFoodIA`).
- [`FinishCookingUseCase`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/usecase/FinishCookingUseCase.kt)
  (línea 45): al terminar de cocinar, descuenta los ingredientes del inventario, calcula el valor rescatado y guarda
  `users/{uid}/cookings`.

### Datos

- [`recetas.json`](../../app/src/main/assets/recetas.json): 50 recetas.
- [`ingredientes.json`](../../app/src/main/assets/ingredientes.json): etiquetas de dieta, empaque y precio en Éxito,
  Carulla, Olímpica y D1.
- Modelos: [`MealPlan.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/model/MealPlan.kt)
  (`PlanContext`, `DietRule`, `PlannerWeights`, `PlanResult`) y
  [`AiPlan.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/model/AiPlan.kt) (`AiContext`,
  `AiPlanOutcome`).

---

## 8. Cómo sabemos que funciona

| Prueba | Qué demuestra |
|---|---|
| [`MealPlannerTest`](../../app/src/test/java/com/smartfood/app/feature/recipes/domain/agent/MealPlannerTest.kt) | Se cumple cada regla y el plan es **el óptimo**, comparado con fuerza bruta |
| [`MealPlannerCatalogTest`](../../app/src/test/java/com/smartfood/app/feature/recipes/domain/agent/MealPlannerCatalogTest.kt) | Con el catálogo real hay plan para cada dieta |
| [`AgentEvaluationTest`](../../app/src/test/java/com/smartfood/app/feature/recipes/domain/agent/AgentEvaluationTest.kt) | El agente supera a dos líneas base en 30 inventarios |
| [`MealPlannerPerformanceTest`](../../app/src/test/java/com/smartfood/app/feature/recipes/domain/agent/MealPlannerPerformanceTest.kt) | Con 50 recetas tarda menos de 2 s |

| Método | Aprovecha lo que vence | Desperdicio medio | Costo medio |
|---|---|---|---|
| **Agente CSP** | **97,2 %** | **0,64** | **COP 22.656** |
| Línea base: más coincidencias | 77,2 % | 1,20 | COP 49.474 |
| Línea base: vence primero | 80,6 % | 1,08 | COP 50.662 |

Las líneas base eligen **día por día** con un solo criterio, como un filtrado simple. La diferencia muestra el valor de
buscar entre combinaciones. Para reproducirlo:

```bash
./gradlew :app:testDebugUnitTest --tests "*AgentEvaluationTest" --tests "*MealPlannerPerformanceTest"
```

---

## 9. ¿Por qué no Machine Learning? Y sus límites

**¿Por qué no ML?**
- No hay datos históricos de usuarios para entrenar un modelo.
- El presupuesto y la dieta son reglas **duras**: un modelo no garantiza cumplirlas y un CSP sí.
- El resultado se puede **explicar** y **comprobar** que es el óptimo.

**Límites que conviene decir:**
- Es una receta por día durante 3 días. No planea desayuno, almuerzo y cena.
- Los pesos 1,0, 0,3 y 0,5 se eligieron a criterio y no se calibraron con usuarios.
- Los precios son de octubre de 2026 y se actualizan con un script (`tools/precios/`), no en tiempo real.
- El emparejamiento depende del nombre que escribe la persona ("pechuga" encuentra pollo; un nombre raro, no).
- Los 30 inventarios de la evaluación son sintéticos. Falta una prueba con usuarios reales.

---

## 10. Preguntas frecuentes de la sustentación

**¿Por qué es un agente y no un filtro?**
Porque percibe, decide entre combinaciones con un objetivo, explica y actúa sobre el inventario. Un filtro solo
ordena recetas sueltas.

**¿Qué tipo de agente es?**
Basado en utilidad: entre todos los planes válidos elige el de mayor `U`.

**¿Cómo saben que el plan es el mejor?**
`MealPlannerTest` compara el resultado con una búsqueda que revisa todas las combinaciones, y siempre coinciden.

**¿Por qué esos pesos?**
Rescatar comida es lo más importante (1,0), desperdiciar castiga más que gastar (0,5 frente a 0,3), y el costo se
divide entre 10.000 para que esté en la misma escala que los demás términos. Calibrarlos con usuarios es trabajo
futuro.

**¿Qué pasa si no encuentra plan?**
Sugiere qué cambiar (presupuesto, picante o tiempo), sin tocar nunca la dieta.

**¿Escala?**
Con 50 recetas tarda milisegundos gracias al dominio filtrado y a la poda. Con miles de recetas habría que medirlo de
nuevo.
