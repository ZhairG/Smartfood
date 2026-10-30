# `MealPlanner.kt`, línea por línea

Explicación detallada del agente de recetas de SmartFood:
[`MealPlanner.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt) (205 líneas).
Cada línea de código lleva encima un comentario que explica qué hace, por qué está ahí y qué idea de Kotlin o de IA
aplica. La explicación general, sin código, está en [`como-funciona-el-agente.md`](como-funciona-el-agente.md), y los
resultados en [`evaluacion-agente.md`](evaluacion-agente.md).

## Índice

1. [Qué es y dónde está en la app](#1-qué-es-y-dónde-está-en-la-app)
2. [Mapa del archivo](#2-mapa-del-archivo)
3. [El código, línea por línea](#3-el-código-línea-por-línea)
4. [Por qué encuentra siempre el mejor plan](#4-por-qué-encuentra-siempre-el-mejor-plan)
5. [Ejemplo paso a paso](#5-ejemplo-paso-a-paso)
6. [Cuánto trabajo hace](#6-cuánto-trabajo-hace)
7. [Anexo: lo que usa de otros archivos](#7-anexo-lo-que-usa-de-otros-archivos)

---

## 1. Qué es y dónde está en la app

`MealPlanner` es el **agente**: recibe las recetas del catálogo, el inventario de la persona y lo que eligió
(presupuesto, porciones, tiempo, picante, dieta y tienda), y devuelve **el mejor plan de 3 días** o, si no existe
ninguno, **sugerencias** de qué cambiar.

```
AiPlanViewModel ──► GeneratePlanUseCase ──► MealPlanner(ingredients).plan(recipes, pantry, planContext)
                    (en Dispatchers.Default)            │
                                                        ├─► PlanResult.Found(MealPlan)        → "Tu plan de 3 días"
                                                        └─► PlanResult.NoPlan(suggestions)    → "Sin coincidencias"
```

Es **Kotlin puro**: no importa nada de Android ni de Firebase. Por eso se prueba con pruebas unitarias en el computador
(`MealPlannerTest`) y corre igual en el teléfono que en la JVM.

Formalmente resuelve un **CSP con optimización** (un problema de satisfacción de restricciones en el que, además de
cumplir las reglas, hay que encontrar la mejor solución):

| Parte del CSP | En el código |
|---|---|
| **Variables** | `D1`, `D2`, `D3`: la receta de cada día. En el código, el parámetro `day` de `assign` y la lista `chosen` |
| **Dominio** | `domain` en `search`: las recetas que sirven por sí solas |
| **Restricciones** | Recetas distintas (`usedIds`), presupuesto total (`budgetLeft`), máximo 3 faltantes (`maxMissing`), inventario compartido (`stock` → `remaining`) |
| **Objetivo** | Maximizar `U = 1,0·rescate − 0,3·costo/10.000 − 0,5·desperdicio` (`PlannerWeights`) |
| **Búsqueda** | `assign`: backtracking con forward checking y ramificación y poda |

---

## 2. Mapa del archivo

| Líneas | Bloque | Papel |
|---|---|---|
| [1–11](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L1-L11) | Paquete e imports | Los modelos que usa |
| [13–37](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L13-L37) | KDoc | Describe el CSP |
| [38–42](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L38-L42) | Clase y evaluador | Configuración del agente |
| [44–47](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L44-L47) | `plan()` | Punto de entrada: plan o sugerencias |
| [49–57](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L49-L57) | `fitsDiet()` | Restricción de dieta |
| [59–77](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L59-L77) | `search()` | Dominio (consistencia de nodo) y arranque de la búsqueda |
| [79–87](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L79-L87) | `Assigned`, `Best` | Datos de la búsqueda |
| [89–144](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L89-L144) | `assign()` | El corazón: backtracking, forward checking y poda |
| [146–165](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L146-L165) | `toPlan()` | Convierte la solución en un `MealPlan` |
| [167–183](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L167-L183) | `suggestions()` | Qué cambiar cuando no hay plan |
| [185–205](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L185-L205) | `companion object` | Constantes y funciones de apoyo |

---

## 3. El código, línea por línea

### 3.1 Paquete e imports

Archivo: [MealPlanner.kt, líneas 1–11](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L1-L11)

```kotlin
// El paquete dice dónde vive el archivo: dentro de la feature "recipes", en la capa "domain" (lógica pura) y en el
// subpaquete "agent". Que esté en domain significa que no puede depender de Android, Firebase ni la interfaz.
package com.smartfood.app.feature.recipes.domain.agent

// Ingredient: un ingrediente del catálogo (nombre, unidad, empaque, precio por tienda y etiquetas de dieta como
// "carne" o "gluten"). El agente lo necesita para saber qué prohíbe cada dieta y cuánto cuesta comprar.
import com.smartfood.app.feature.recipes.domain.model.Ingredient
// MealPlan: el resultado final, un plan de varios días con su costo total, lo que sobra del presupuesto, lo que se
// desperdicia y la utilidad.
import com.smartfood.app.feature.recipes.domain.model.MealPlan
// PantryItem: un producto del inventario ya emparejado con un ingrediente del catálogo, con su cantidad y los días que
// le faltan para vencer. Es lo que el agente "percibe" del mundo.
import com.smartfood.app.feature.recipes.domain.model.PantryItem
// PlanContext: lo que eligió la persona (presupuesto, porciones, tiempo máximo, sin picante, dieta, tienda) y los
// límites del problema (3 días, máximo 3 faltantes por receta).
import com.smartfood.app.feature.recipes.domain.model.PlanContext
// PlanResult: lo que devuelve el agente; es una sealed interface con dos casos, Found (hay plan) y NoPlan (no hay).
import com.smartfood.app.feature.recipes.domain.model.PlanResult
// PlanSuggestion: cada sugerencia cuando no hay plan (subir presupuesto, permitir picante, más tiempo, faltan recetas).
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion
// PlannedDay: una receta asignada a un día dentro del plan final, con sus compras y su utilidad.
import com.smartfood.app.feature.recipes.domain.model.PlannedDay
// PlannerWeights: los tres pesos de la función de utilidad (rescate 1,0; costo 0,3; desperdicio 0,5).
import com.smartfood.app.feature.recipes.domain.model.PlannerWeights
// Recipe: una receta del catálogo (ingredientes, porciones, tiempos, si es picante).
import com.smartfood.app.feature.recipes.domain.model.Recipe
```

**En palabras simples:** el agente solo trabaja con modelos de dominio. `RecipeEvaluator` y `PantryStock` no se
importan porque viven en el mismo paquete (`agent`).

### 3.2 La KDoc (la descripción del archivo)

Ver el código: [MealPlanner.kt, líneas 13–37](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L13-L37)

La cabecera KDoc es obligatoria en el curso (Nombre, Autor, Fecha, Descripción). Aquí además resume el CSP completo.
Así se lee cada parte:

| Líneas | Dice | Qué significa |
|---|---|---|
| 14–16 | Nombre, autor y fecha | Cabecera exigida por el curso |
| 17–18 | "Agente de IA… satisfacción de restricciones con optimización" | El tipo de problema: cumplir reglas **y** buscar el mejor |
| 19 | **Variables** `D1…Dn` | Una variable por día; `n` sale de `PlanContext.days` (3) |
| 20–23 | **Dominio** | Se quitan antes las recetas que no sirven solas: tiempo, picante, dieta y costo. Los faltantes **no** se filtran aquí, porque el sobrante de un empaque comprado otro día puede cubrir un ingrediente |
| 24–25 | **Restricciones entre variables** | Las que dependen de los otros días: recetas distintas, inventario compartido, presupuesto total y faltantes con el inventario que queda |
| 26 | **Objetivo** | Maximizar la utilidad del plan |
| 27–31 | **Búsqueda** | Backtracking día por día, forward checking por presupuesto y faltantes, y ramificación y poda con una cota que nunca subestima |
| 33 | Determinista | Ante un empate en utilidad gana el plan con los ids de receta menores: la misma entrada da siempre la misma salida (RNF-RIA-01) |
| 35–36 | `@param` | Los dos parámetros del constructor |

### 3.3 La clase y su evaluador

Archivo: [MealPlanner.kt, líneas 38–42](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L38-L42)

```kotlin
// Clase normal (no data class, no object): cada vez que se necesita planear se crea un MealPlanner nuevo con el
// catálogo. GeneratePlanUseCase hace MealPlanner(ingredients).plan(...). No guarda estado entre llamadas.
class MealPlanner(
    // Todos los ingredientes del catálogo, indexados por su id ("pollo", "tomate"…). Un Map permite buscar un
    // ingrediente en tiempo constante, O(1), lo que importa porque se consulta miles de veces durante la búsqueda.
    // "private val" lo convierte en una propiedad privada: nadie de afuera puede leerlo ni cambiarlo.
    private val ingredients: Map<String, Ingredient>,
    // Los pesos de la función de utilidad. Tiene valor por defecto (1,0 / 0,3 / 0,5), así que en la app normal no se
    // pasa. Se cambia en dos casos: en suggestions(), para buscar el plan más barato, y en las pruebas de sensibilidad.
    private val weights: PlannerWeights = PlannerWeights(),
) {
    // El evaluador sabe calcular, para UNA receta en UN día con UN inventario, qué usa, qué compra, cuánto cuesta y
    // cuánto rescata. Se crea una vez por MealPlanner, con los mismos ingredientes y pesos, y se reutiliza en toda la
    // búsqueda. Separar "evaluar una receta" (RecipeEvaluator) de "buscar el mejor plan" (MealPlanner) mantiene cada
    // archivo con una sola responsabilidad.
    private val evaluator = RecipeEvaluator(ingredients, weights)
```

**En palabras simples:** el agente se configura con el catálogo y los pesos, y delega la aritmética de cada receta en
`RecipeEvaluator`.

### 3.4 `plan()`: la puerta de entrada

Archivo: [MealPlanner.kt, líneas 44–47](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L44-L47)

```kotlin
    /** Busca el plan de mayor utilidad; si no hay ninguno válido, devuelve sugerencias (CA-RIA-02-03, 02-07). */
    // Función pública con cuerpo de expresión (el "=" en lugar de llaves): devuelve directamente el resultado de la
    // expresión de abajo. Recibe las recetas del catálogo, el inventario emparejado y el contexto de la persona.
    fun plan(recipes: List<Recipe>, pantry: List<PantryItem>, context: PlanContext): PlanResult =
        // search() devuelve el mejor MealPlan o null si no hay ninguno. "?.let" solo se ejecuta si NO es null:
        // envuelve el plan en PlanResult.Found, el caso "hay plan" de la sealed interface.
        search(recipes, pantry, context)?.let { PlanResult.Found(it) }
            // Operador Elvis "?:": si lo de la izquierda dio null (no hubo plan), usa lo de la derecha. Solo entonces
            // se calculan las sugerencias, que son costosas (repiten la búsqueda), así que no se pagan si hay plan.
            ?: PlanResult.NoPlan(suggestions(recipes, pantry, context))
```

**En palabras simples:** intenta encontrar el mejor plan; si no existe, explica qué cambiar. El agente nunca responde
solo "no se pudo".

### 3.5 `fitsDiet()`: la restricción de dieta

Archivo: [MealPlanner.kt, líneas 49–57](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L49-L57)

```kotlin
    /**
     * `true` si ningún ingrediente de [recipe] tiene una etiqueta que la dieta de [context] prohíbe (CA-RIA-02-09).
     * Un ingrediente que no está en el catálogo no tiene etiquetas.
     */
    // Es pública porque las pruebas (MealPlannerTest y MealPlannerCatalogTest) la llaman directamente para comprobar
    // cada dieta. Devuelve Boolean: true = la receta sirve para esta dieta.
    fun fitsDiet(recipe: Recipe, context: PlanContext): Boolean {
        // context.diet es un conjunto de reglas (por ejemplo, {VEGAN, GLUTEN_FREE}). Cada regla tiene sus etiquetas
        // prohibidas: VEGAN prohíbe {MEAT, SEAFOOD, ANIMAL_PRODUCT}. flatMapTo junta TODAS las etiquetas prohibidas de
        // todas las reglas en un solo conjunto mutable. Si la persona es vegana y sin gluten, queda
        // {MEAT, SEAFOOD, ANIMAL_PRODUCT, GLUTEN}. Las reglas se SUMAN.
        val forbidden = context.diet.flatMapTo(mutableSetOf()) { it.forbidden }
        // Atajo: sin dieta no hay nada prohibido y cualquier receta sirve. Ahorra recorrer los ingredientes de las 50
        // recetas en el caso más común.
        if (forbidden.isEmpty()) return true
        // none { } es true si NINGÚN elemento cumple la condición. Para cada ingrediente de la receta (ri) se busca
        // el ingrediente en el catálogo por su id; "?." evita fallar si no está, y ".tags.orEmpty()" da un conjunto
        // vacío en ese caso (un ingrediente desconocido no tiene etiquetas). any { it in forbidden } es true si
        // alguna de sus etiquetas está prohibida. Resultado: la receta sirve solo si ningún ingrediente tiene una
        // etiqueta prohibida.
        return recipe.ingredients.none { ri -> ingredients[ri.ingredientId]?.tags.orEmpty().any { it in forbidden } }
    }
```

**En palabras simples:** junta todo lo que prohíben las dietas elegidas y descarta la receta si **cualquier**
ingrediente tiene una de esas etiquetas. Es una restricción **dura**: nunca se sugiere quitarla.

### 3.6 `search()`: el dominio y el arranque de la búsqueda

Archivo: [MealPlanner.kt, líneas 59–77](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L59-L77)

```kotlin
    /** El plan de mayor utilidad que cumple todas las restricciones, o `null` si no existe. */
    // Devuelve MealPlan? (nullable): null significa "no hay ningún plan que cumpla todo". Es pública porque
    // suggestions() la llama con contextos modificados y las pruebas la comparan con la fuerza bruta.
    fun search(recipes: List<Recipe>, pantry: List<PantryItem>, context: PlanContext): MealPlan? {
        // Convierte la lista de productos en un PantryStock: el inventario "vivo" de la búsqueda, que sabe cuánto
        // queda de cada producto. Es INMUTABLE: consumir algo devuelve un PantryStock nuevo y deja este igual. Por eso
        // el backtracking puede volver atrás sin "deshacer" nada: la versión anterior sigue intacta.
        val full = PantryStock.of(pantry)
        // Consistencia de nodo: las restricciones de una sola receta. Lo que cuesta sola con el inventario completo es lo
        // mínimo que el plan tiene que pagar por ella (los empaques se compran igual, en ese día o antes).
        // "Consistencia de nodo" es el término de CSP para quitar del dominio los valores que violan una restricción
        // UNARIA (de una sola variable). Se hace una vez, antes de buscar, para no evaluar esas recetas miles de veces.
        val domain = recipes
            // 1) Tiempo: si la persona no puso límite (maxMinutes == null), pasa todo; si lo puso, solo las recetas
            //    cuyo tiempo total (preparación + cocción + reposo) no lo supera.
            .filter { context.maxMinutes == null || it.totalMinutes <= context.maxMinutes }
            // 2) Picante: se descarta la receta solo si la persona pidió "Sin picante" Y la receta es picante.
            //    "!(a && b)" se lee "no es el caso de que ambas sean ciertas".
            .filter { !(context.noSpicy && it.spicy) }
            // 3) Dieta: la función de la sección anterior.
            .filter { fitsDiet(it, context) }
            // 4) Costo individual: se evalúa la receta como si se cocinara el día 1 con TODO el inventario. Ese costo
            //    es el mínimo posible para esa receta (con menos inventario solo puede costar igual o más), así que si
            //    ni así cabe en el presupuesto, nunca entrará en un plan. Ojo: aquí NO se filtran los faltantes
            //    (maxMissing), porque el sobrante de un empaque comprado otro día puede cubrir un ingrediente.
            .filter {
                evaluator.evaluate(it, context.servings, day = 1, stock = full, store = context.store).costCop <= context.budgetCop
            }
            // Orden por id: hace la búsqueda DETERMINISTA. Con la misma entrada se recorren las recetas en el mismo
            // orden y sale siempre el mismo plan.
            .sortedBy { it.id }
        // Si quedan menos recetas que días (por ejemplo, 2 recetas para 3 días), es imposible armar un plan con
        // recetas distintas. Se responde "no hay plan" sin buscar.
        if (domain.size < context.days) return null

        // El "mejor hasta ahora": empieza vacío, con utilidad -infinito, para que el primer plan completo lo mejore.
        val best = Best()
        // Arranca la búsqueda recursiva en el día 1, sin recetas elegidas, con el inventario completo, todo el
        // presupuesto y utilidad acumulada 0. "domain, context, best" van sin nombre porque están en su posición.
        assign(day = 1, chosen = emptyList(), stock = full, budgetLeft = context.budgetCop, partial = 0.0, domain, context, best)
        // Al terminar, best.plan es el mejor plan encontrado, o null si ninguna rama llegó al final.
        return best.plan
    }
```

**En palabras simples:** primero se tachan las recetas que nunca podrían entrar (consistencia de nodo); después se
recorren las combinaciones con `assign`, que va dejando el mejor plan en `best`.

### 3.7 `Assigned` y `Best`: los datos de la búsqueda

Archivo: [MealPlanner.kt, líneas 79–87](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L79-L87)

```kotlin
    /** Una receta asignada a un día, con su evaluación en ese momento de la búsqueda. */
    // data class privada: solo existe dentro del agente. Junta la receta con su evaluación (uso de ingredientes,
    // costo, rescate, utilidad e inventario que queda después de cocinarla). Se guarda la evaluación para no
    // recalcularla: la misma receta da resultados distintos según el día y el inventario.
    private data class Assigned(val recipe: Recipe, val evaluation: RecipeEvaluator.Evaluation)

    /** Mejor plan encontrado hasta ahora. */
    // Clase normal (no data class) y MUTABLE a propósito: todas las ramas de la recursión comparten el mismo objeto
    // Best y lo actualizan cuando encuentran algo mejor. Así la poda de una rama usa el mejor plan hallado en
    // cualquier otra rama, aunque sea de otra parte del árbol.
    private class Best {
        // Utilidad del mejor plan. Empieza en -infinito: cualquier plan completo es mejor que "ninguno".
        var utility = Double.NEGATIVE_INFINITY
        // Ids de las recetas del mejor plan, en orden de día. Sirven para desempatar.
        var ids: List<String> = emptyList()
        // El plan completo, ya convertido a MealPlan. Null hasta encontrar el primero.
        var plan: MealPlan? = null
    }
```

**En palabras simples:** `Assigned` es "esta receta, este día, con este resultado", y `Best` es la memoria compartida
de la mejor solución encontrada.

### 3.8 `assign()`: el caso base (un plan completo)

Archivo: [MealPlanner.kt, líneas 89–110](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L89-L110)

```kotlin
    // Función RECURSIVA: asigna una receta al día "day" y se llama a sí misma para el día siguiente. Cada llamada es
    // un nodo del árbol de búsqueda; volver de la llamada es el "backtracking".
    private fun assign(
        // El día que se está decidiendo: 1, 2 o 3 (y 4 cuando ya se decidieron los tres).
        day: Int,
        // Las recetas ya elegidas en los días anteriores, en orden. Es una lista inmutable: "chosen + candidate"
        // crea otra lista, así cada rama tiene la suya.
        chosen: List<Assigned>,
        // El inventario que queda después de cocinar las recetas de "chosen". Inmutable.
        stock: PantryStock,
        // El presupuesto que queda: el inicial menos lo que cuestan las recetas de "chosen".
        budgetLeft: Long,
        // La utilidad acumulada de las recetas de "chosen" (rescate y costo de cada una). Todavía SIN restar el
        // desperdicio, porque el desperdicio solo se sabe al final del plan.
        partial: Double,
        // El dominio calculado en search(); siempre el mismo.
        domain: List<Recipe>,
        // El contexto de la persona; siempre el mismo.
        context: PlanContext,
        // La memoria compartida del mejor plan.
        best: Best,
    ) {
        // CASO BASE: si el día ya pasó del último (4 > 3), las tres variables tienen receta: es un plan completo.
        if (day > context.days) {
            // Utilidad final: lo acumulado menos el desperdicio ponderado (0,5 × lo que vence en ≤ 3 días y no se usó).
            // Se calcula sobre "stock", el inventario después de los tres días.
            val utility = partial - weights.waste * evaluator.waste(stock)
            // Los ids del plan, en orden de día, para el desempate.
            val ids = chosen.map { it.recipe.id }
            // Este plan reemplaza al mejor si: (a) todavía no había ninguno; (b) su utilidad es MAYOR, con un margen
            // EPSILON para no confundir errores de redondeo de Double con diferencias reales; o (c) empata (dentro del
            // margen) y sus ids van antes en orden alfabético. La regla (c) es la que hace el resultado determinista.
            if (best.plan == null || utility > best.utility + EPSILON ||
                (utility >= best.utility - EPSILON && compareIds(ids, best.ids) < 0)
            ) {
                // Guarda la nueva mejor utilidad…
                best.utility = utility
                // …sus ids…
                best.ids = ids
                // …y el plan completo, convertido a MealPlan con sus totales.
                best.plan = toPlan(chosen, stock, context, utility)
            }
            // Termina esta rama: vuelve a la llamada anterior, que probará el siguiente candidato (backtracking).
            return
        }
```

**En palabras simples:** cuando ya hay receta para los tres días, se calcula la utilidad final (restando el
desperdicio) y, si es la mejor, se guarda.

### 3.9 `assign()`: forward checking

Archivo: [MealPlanner.kt, líneas 111–124](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L111-L124)

```kotlin
        // Forward checking. Una receta que hoy no cabe en el presupuesto restante tampoco cabrá otro día: lo que otra receta
        // compre antes se paga igual dentro del plan. Los faltantes, en cambio, pueden bajar (el sobrante de un empaque
        // comprado antes cubre un ingrediente), así que solo deciden qué recetas sirven hoy, no si la rama tiene salida.
        // "Forward checking" (comprobación hacia adelante): antes de asignar el día, se miran los valores que todavía
        // son posibles con lo que ya se decidió. Si no alcanzan para los días que faltan, la rama se abandona YA, sin
        // explorar sus hijos.
        // Conjunto (HashSet) con los ids ya usados. Buscar en un HashSet es O(1); en una lista sería O(n).
        val usedIds = chosen.mapTo(HashSet()) { it.recipe.id }
        // asSequence(): procesa la cadena de forma perezosa, receta por receta, sin crear listas intermedias en cada
        // paso. Importa porque esto se ejecuta en cada nodo del árbol.
        val affordable = domain.asSequence()
            // Restricción "todas distintas": fuera las recetas que ya se eligieron en otro día.
            .filter { it.id !in usedIds }
            // Evalúa cada receta HOY, con el inventario que dejaron los días anteriores. Es la parte costosa: calcula
            // qué productos usa, qué compra en empaques completos, cuánto cuesta y cuánto rescata.
            .map { Assigned(it, evaluator.evaluate(it, context.servings, day, stock, context.store)) }
            // Restricción de presupuesto total: la receta debe caber en lo que queda.
            .filter { it.evaluation.costCop <= budgetLeft }
            // Materializa el resultado en una lista, porque se usa dos veces (para contar y para filtrar).
            .toList()
        // Días que faltan contando hoy: el día 1 de 3 deja 3; el día 3 deja 1.
        val daysLeft = context.days - day + 1
        // Comprobación hacia adelante: si las recetas que caben en el presupuesto son menos que los días que faltan,
        // esta rama nunca llegará a un plan completo (cada día necesita una receta distinta). Se abandona aquí.
        if (affordable.size < daysLeft) return
        // De las que caben, sirven HOY solo las que tienen como mucho maxMissing (3) ingredientes por comprar con el
        // inventario actual. No se usa para abandonar la rama porque una receta con 4 faltantes hoy podría tener 3
        // mañana, si otra receta compra un empaque y sobra.
        val candidates = affordable
            .filter { it.evaluation.missingCount <= context.maxMissing }
            // ORDEN DE VALORES: primero las de mayor utilidad (las "más prometedoras"); a igual utilidad, por id.
            // Probar primero las mejores hace que "best" se llene pronto con un plan bueno, y así la poda corta más.
            .sortedWith(compareByDescending<Assigned> { it.evaluation.utility }.thenBy { it.recipe.id })
```

**En palabras simples:** antes de probar el día, se dejan solo las recetas posibles. Si ni contando las que caben en
el presupuesto alcanza para los días que faltan, la rama muere aquí. Las que quedan se prueban de la más prometedora a
la menos.

### 3.10 `assign()`: la cota, la poda y la recursión

Archivo: [MealPlanner.kt, líneas 126–144](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L126-L144)

```kotlin
        // Prueba cada receta candidata para este día. Cada vuelta del for es una RAMA del árbol de búsqueda.
        for (candidate in candidates) {
            // Cota optimista: lo que lleva el plan, más esta receta, más todo el rescate que el inventario todavía puede dar
            // en los días que faltan, sin descontar costo ni desperdicio. El rescate de una receta puede subir de un día a
            // otro (si otra se lleva un producto, saca de otro que vence pronto), pero nunca supera lo que queda sin usar.
            // RAMIFICACIÓN Y PODA (branch and bound). "future" es lo máximo que los días siguientes podrían rescatar:
            // la fracción sin usar de cada producto por vencer que siga en buen estado el día siguiente, por su
            // urgencia 1/(días + 1). En el último día no hay "siguientes", así que es 0.
            val future = if (day < context.days) evaluator.rescuePotential(candidate.evaluation.remaining, day + 1) else 0.0
            // "bound" es lo MEJOR que podría llegar a valer cualquier plan que empiece así: lo acumulado, más esta
            // receta, más todo el rescate posible a futuro (con su peso). Es optimista porque supone que el futuro
            // rescata TODO y no cuesta nada ni desperdicia nada. Por eso nunca se queda corta.
            val bound = partial + candidate.evaluation.utility + weights.rescue * future
            // PODA: si ya hay un mejor plan y ni en el mejor caso esta rama lo alcanza, se salta ("continue") sin
            // explorar sus hijos. Se usa "< best − EPSILON" (estrictamente menor): una rama que podría EMPATAR se
            // explora, porque podría ganar el desempate por ids.
            if (best.plan != null && bound < best.utility - EPSILON) continue
            // RECURSIÓN: asigna esta receta al día actual y pasa al siguiente con todo actualizado.
            assign(
                // El día siguiente.
                day + 1,
                // Las elegidas, más esta (lista nueva; la de esta llamada no cambia).
                chosen + candidate,
                // El inventario DESPUÉS de cocinar esta receta, calculado por el evaluador.
                candidate.evaluation.remaining,
                // El presupuesto que queda, menos lo que compra esta receta.
                budgetLeft - candidate.evaluation.costCop,
                // La utilidad acumulada, más la de esta receta.
                partial + candidate.evaluation.utility,
                // Lo que no cambia.
                domain,
                context,
                best,
            )
            // Al volver de la recursión, el for sigue con el siguiente candidato: ESE es el backtracking. No hay nada
            // que deshacer, porque chosen, stock, budgetLeft y partial de esta llamada nunca se modificaron.
        }
    }
```

**En palabras simples:** para cada receta posible, se calcula lo **máximo** que podría valer el plan si se elige. Si
ni así supera al mejor encontrado, se descarta la rama entera; si sí, se asigna y se pasa al día siguiente.

### 3.11 `toPlan()`: de la búsqueda al resultado

Archivo: [MealPlanner.kt, líneas 146–165](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L146-L165)

```kotlin
    // Convierte la solución interna (lista de Assigned) en el modelo que entiende el resto de la app (MealPlan).
    // Solo se llama cuando un plan mejora al mejor, no en cada nodo.
    private fun toPlan(chosen: List<Assigned>, finalStock: PantryStock, context: PlanContext, utility: Double): MealPlan {
        // mapIndexed recorre la lista con su posición (i = 0, 1, 2) y crea un PlannedDay por receta.
        val days = chosen.mapIndexed { i, a ->
            PlannedDay(
                // Día del plan desde 1: la posición 0 es el día 1.
                day = i + 1,
                // La receta elegida.
                recipe = a.recipe,
                // Cómo usa cada ingrediente: qué sale del inventario, qué se compra y cuántos empaques.
                uses = a.evaluation.uses,
                // Lo que cuestan las compras de esta receta.
                costCop = a.evaluation.costCop,
                // Fracción de los ingredientes que cubre la casa, para "92 % del inventario" en la tarjeta del día.
                pantryShare = a.evaluation.pantryShare,
                // Lo que aporta esta receta a la utilidad.
                utility = a.evaluation.utility,
            )
        }
        // Costo total del plan: la suma de lo que compra cada día. sumOf suma un valor de cada elemento.
        val total = days.sumOf { it.costCop }
        return MealPlan(
            // Los tres días.
            days = days,
            // El costo total.
            totalCostCop = total,
            // Lo que queda del presupuesto: "Te sobran COP $X" en "¿Por qué te conviene?".
            remainingBudgetCop = context.budgetCop - total,
            // Los productos que vencen pronto y el plan no alcanza a usar, para avisar a la persona.
            wastedItems = evaluator.wastedItems(finalStock),
            // La utilidad final, ya con el desperdicio restado.
            utility = utility,
        )
    }
```

**En palabras simples:** arma el plan que verá la persona, con el costo de cada día, el total, lo que sobra del
presupuesto y lo que se perdería.

### 3.12 `suggestions()`: qué cambiar si no hay plan

Archivo: [MealPlanner.kt, líneas 167–183](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L167-L183)

```kotlin
    /**
     * Qué cambiar para que exista un plan (CA-RIA-02-07): el presupuesto mínimo (el plan más barato, buscado con la
     * misma búsqueda y pesos que solo miran el costo), quitar "Sin picante" o subir el tiempo máximo.
     */
    // Idea: RELAJAR una restricción a la vez y volver a buscar. Si así hay plan, esa restricción era el problema.
    private fun suggestions(recipes: List<Recipe>, pantry: List<PantryItem>, context: PlanContext): List<PlanSuggestion> {
        // Un segundo agente con pesos que SOLO miran el costo (rescate 0, costo 1, desperdicio 0). Para él, "el plan
        // de mayor utilidad" es el más barato, porque la utilidad es −costo. Reutiliza la misma búsqueda sin escribir
        // otro algoritmo.
        val cheapest = MealPlanner(ingredients, CHEAPEST)
        // El mismo contexto, pero con un presupuesto prácticamente infinito. copy() crea un PlanContext nuevo con un
        // solo cambio; el original no se toca.
        val unlimited = context.copy(budgetCop = UNLIMITED_BUDGET)
        // Lista mutable donde se juntan las sugerencias que funcionen.
        val out = mutableListOf<PlanSuggestion>()
        // 1) Presupuesto: busca el plan más barato sin límite de dinero. Si existe, sugiere "Sube el presupuesto a X",
        //    con X = su costo redondeado hacia arriba a miles. "out += x" agrega a la lista.
        cheapest.search(recipes, pantry, unlimited)?.let { out += PlanSuggestion.RaiseBudget(roundUpToThousand(it.totalCostCop)) }
        // 2) Picante: solo si la persona pidió "Sin picante". Repite la búsqueda permitiéndolo; si así hay plan, sugiere
        //    "Permite recetas picantes".
        if (context.noSpicy && search(recipes, pantry, context.copy(noSpicy = false)) != null) out += PlanSuggestion.AllowSpicy
        // 3) Tiempo: solo si puso un tiempo máximo.
        if (context.maxMinutes != null) {
            // Repite la búsqueda sin límite de tiempo…
            search(recipes, pantry, context.copy(maxMinutes = null))?.let { plan ->
                // …y sugiere como tiempo el de la receta más larga de ese plan: con ese límite, el plan cabe.
                out += PlanSuggestion.AllowMoreTime(plan.days.maxOf { it.recipe.totalMinutes })
            }
        }
        // Si nada funcionó, la única sugerencia es "Agrega más productos a tu inventario" (NotEnoughRecipes).
        // ifEmpty { } devuelve la lista si tiene algo, o la del bloque si está vacía.
        // La DIETA nunca se relaja: es una decisión de la persona, no un ajuste.
        return out.ifEmpty { listOf(PlanSuggestion.NotEnoughRecipes) }
    }
```

**En palabras simples:** prueba qué pasaría con más presupuesto, con picante o con más tiempo, y sugiere solo lo que
de verdad produce un plan. La dieta no se toca.

### 3.13 `companion object`: constantes y funciones de apoyo

Archivo: [MealPlanner.kt, líneas 185–205](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/MealPlanner.kt#L185-L205)

```kotlin
    // companion object: lo que pertenece a la CLASE y no a cada instancia (como "static" en Java). "private": solo se
    // usa dentro de MealPlanner.
    private companion object {
        // Margen para comparar Double. Las sumas con decimales tienen errores de redondeo minúsculos (0,1 + 0,2 no es
        // exactamente 0,3). Con este margen, dos utilidades que difieren en menos de 0,000000001 cuentan como iguales.
        const val EPSILON = 1e-9

        /** Presupuesto sin límite para calcular las sugerencias. */
        // Un número enorme en lugar de Long.MAX_VALUE: se divide entre 4 para que sumar o restar costos no lo haga
        // desbordarse (pasar del máximo de Long y volverse negativo).
        const val UNLIMITED_BUDGET = Long.MAX_VALUE / 4

        /** Pesos que solo miran el costo: el plan "de mayor utilidad" es el más barato. */
        // "val" y no "const val" porque PlannerWeights es un objeto; const solo admite tipos primitivos y String.
        val CHEAPEST = PlannerWeights(rescue = 0.0, cost = 1.0, waste = 0.0)

        // Redondea hacia arriba a miles con aritmética entera: (12 345 + 999) / 1000 = 13 (la división entera trunca),
        // × 1000 = 13 000. Un múltiplo exacto queda igual: (12 000 + 999) / 1000 = 12 → 12 000.
        fun roundUpToThousand(cop: Long): Long = (cop + 999) / 1000 * 1000

        /** Orden lexicográfico de dos listas de ids del mismo tamaño. */
        // Compara dos planes "como en el diccionario": primero el id del día 1; si empatan, el del día 2, y así.
        // Devuelve negativo si "a" va antes, positivo si va después y 0 si son iguales.
        fun compareIds(a: List<String>, b: List<String>): Int {
            // Recorre las posiciones de "a" (0, 1, 2).
            for (i in a.indices) {
                // compareTo de String compara alfabéticamente. getOrElse da "" si "b" fuera más corta, para no fallar.
                val c = a[i].compareTo(b.getOrElse(i) { "" })
                // La primera posición distinta decide.
                if (c != 0) return c
            }
            // Si todas las posiciones coinciden, la lista más corta va antes. En la práctica las dos tienen 3 ids.
            return a.size - b.size
        }
    }
}
```

**En palabras simples:** valores fijos para comparar decimales, para el presupuesto sin límite y para el plan más
barato, más dos utilidades: redondear a miles y desempatar planes.

---

## 4. Por qué encuentra siempre el mejor plan

La búsqueda descarta ramas sin explorarlas. Eso solo es correcto si **nunca descarta la rama del mejor plan**. Hay
cuatro razones por las que no lo hace:

1. **El dominio no quita nada útil.** El costo de una receta sola con todo el inventario es el **mínimo** que puede
   costar; si ni así cabe, ningún plan la incluye. Los faltantes no se filtran ahí, porque pueden bajar con lo que
   sobra de un empaque.
2. **El forward checking solo usa el presupuesto para abandonar ramas.** Una receta que hoy no cabe en el presupuesto
   tampoco cabrá otro día, porque lo que compre otra receta se paga igual dentro del plan. Los faltantes solo deciden
   qué recetas se prueban **hoy**, no si la rama tiene salida.
3. **La cota nunca subestima.** `bound` suma lo acumulado, la receta de hoy y **todo** el rescate que todavía puede dar
   el inventario (`rescuePotential`), sin restar costo ni desperdicio. Ningún plan real puede rescatar más de lo que
   queda sin usar, así que ningún plan de esa rama supera `bound`. Si `bound` no alcanza al mejor, la rama de verdad no
   puede ganar.
4. **Los empates se exploran.** La poda usa `bound < best − EPSILON`: una rama que podría empatar se explora, porque
   podría ganar el desempate por ids.

**Cómo se comprueba.** [`MealPlannerTest`](../../app/src/test/java/com/smartfood/app/feature/recipes/domain/agent/MealPlannerTest.kt)
compara el resultado con una búsqueda **por fuerza bruta** (`bruteForce`), que revisa todas las combinaciones:
- `CA-RIA-02-04 el plan es el de mayor utilidad comparado contra la fuerza bruta`: 40 escenarios aleatorios.
- `CA-RIA-02-04 coincide con la fuerza bruta con productos repetidos y recetas con muchos faltantes`: 150 escenarios.
- `CA-RIA-02-04 la poda no pierde el mejor plan cuando el rescate de una receta sube de un dia a otro`: el caso que
  motivó la cota con `rescuePotential` (commit `7977b1b`).
- `CA-RIA-02-04 una receta con mas de 3 faltantes hoy puede entrar otro dia con el sobrante de un empaque`: el motivo
  de no filtrar los faltantes en el dominio.
- `RNF-RIA-01 el mismo inventario y contexto dan siempre el mismo plan`: el determinismo.

---

## 5. Ejemplo paso a paso

> **Ilustrativo:** los números son inventados para seguir el algoritmo. No salen del catálogo real.

**Entrada.** Presupuesto COP $60.000, 3 días.

| Producto | Vence en | Urgencia `1/(días + 1)` |
|---|---|---|
| Pollo | 1 día | 0,50 |
| Tomate | 2 días | 0,33 |
| Leche | 3 días | 0,25 |

| Receta | Rescata | Compra | `U` de la receta (`rescate − 0,3·costo/10.000`) |
|---|---|---|---|
| A · Arroz con pollo | Pollo (0,50) | $3.000 | 0,50 − 0,09 = **0,41** |
| B · Huevos pericos | Tomate (0,33) | $4.000 | 0,33 − 0,12 = **0,21** |
| C · Avena con leche | Leche (0,25) | $2.000 | 0,25 − 0,06 = **0,19** |
| D · Pasta | Nada | $10.000 | 0 − 0,30 = **−0,30** |
| E · Bandeja | — | $70.000 | No entra |

**1. `search` arma el dominio.** E sola cuesta $70.000 > $60.000, así que sale. Dominio: A, B, C, D (4 ≥ 3 días,
sigue).

**2. `assign` recorre el árbol.** Los candidatos de cada día se ordenan por `U`: A, B, C, D.

```
Día 1 = A (U 0,41)  cota = 0 + 0,41 + (tomate 0,33 + leche 0,25) = 0,99   → explora (no hay mejor todavía)
 ├─ Día 2 = B (0,21)  cota = 0,41 + 0,21 + leche 0,25 = 0,87               → explora
 │   ├─ Día 3 = C (0,19)  cota = 0,62 + 0,19 = 0,81                        → PLAN A-B-C, U = 0,81, desperdicio 0  ★ mejor
 │   └─ Día 3 = D (−0,30) cota = 0,62 − 0,30 = 0,32 < 0,81                 → ✂ poda
 ├─ Día 2 = C (0,19)  cota = 0,41 + 0,19 + tomate 0,33 = 0,93 ≥ 0,81       → explora
 │   ├─ Día 3 = B (0,21)  cota = 0,60 + 0,21 = 0,81 (empata)               → PLAN A-C-B, U = 0,81; "C" > "B": no gana el desempate
 │   └─ Día 3 = D         cota = 0,60 − 0,30 = 0,30                        → ✂ poda
 └─ Día 2 = D (−0,30) cota = 0,41 − 0,30 + 0,58 = 0,69 < 0,81              → ✂ poda (no se exploran sus hijos)
Día 1 = B, C          cotas de 0,96 y 1,02: se exploran, pero ningún plan supera 0,81
Día 1 = D (−0,30)     cota = −0,30 + (0,50 + 0,33 + 0,25) = 0,78 < 0,81    → ✂ poda: toda la rama, sin evaluar nada debajo
```

**3. Resultado.** `PlanResult.Found` con A → B → C:
- utilidad 0,81;
- costo $9.000, sobran $51.000;
- nada se desperdicia.

**Un detalle que muestra el ejemplo.** El pollo vence en 1 día, así que solo sirve los días 1 y 2
(`isUsableOn(day)` exige `daysToExpire ≥ day − 1`). Si A cayera en el día 3, tendría que **comprar** el pollo y no
rescataría nada. Por eso el agente pone lo más urgente primero, sin que esa regla esté escrita en ningún lado: sale de
maximizar `U`.

---

## 6. Cuánto trabajo hace

- **Peor caso:** con `n` recetas en el dominio y 3 días hay `n × (n − 1) × (n − 2)` planes ordenados. Con 50 recetas son
  117.600, y cada uno exige evaluar recetas (`evaluate`), que recorre sus ingredientes.
- **Qué lo reduce:**
  - el **dominio**, que quita de entrada las recetas imposibles;
  - el **forward checking**, que solo evalúa las que caben en el presupuesto restante;
  - el **orden** de los candidatos, que encuentra pronto un plan bueno;
  - la **poda**, que descarta ramas enteras.
- **Medido:** entre 30 y 62 ms en el teléfono con el APK release, frente al límite de 2 s (CA-RIA-02-10). Los detalles
  están en [`evaluacion-agente.md`](evaluacion-agente.md).
- **Memoria:** la recursión tiene como mucho 4 niveles (días 1 a 3 y el caso base) y cada nivel guarda su lista y su
  inventario inmutables, así que la memoria es pequeña y no hay riesgo de desbordar la pila.

---

## 7. Anexo: lo que usa de otros archivos

**De [`RecipeEvaluator.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/RecipeEvaluator.kt)**

| Función o clase | Qué devuelve |
|---|---|
| [`evaluate(recipe, servings, day, stock, store)`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/RecipeEvaluator.kt#L59) | Una `Evaluation` con: `uses` (uso de cada ingrediente), `costCop` (compras en empaques completos), `missingCount` (ingredientes por comprar), `pantryShare`, `rescue` (lo que vence pronto y usa, por urgencia), `utility` (`1,0·rescue − 0,3·costo/10.000`) y `remaining` (el inventario después de cocinarla) |
| [`waste(stock)`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/RecipeEvaluator.kt#L88) | Suma de la fracción sin usar de los productos que vencen en ≤ 3 días |
| [`wastedItems(stock)`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/RecipeEvaluator.kt#L84) | Los ids de esos productos, para avisar a la persona |
| [`rescuePotential(stock, fromDay)`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/RecipeEvaluator.kt#L97) | Lo máximo que todavía se puede rescatar desde ese día: la fracción sin usar de cada producto por vencer que siga en buen estado, por su urgencia. Es la parte futura de la cota |
| `PantryStock` | El inventario inmutable: `of(pantry)` lo crea completo, y cada consumo devuelve uno nuevo |

**De [`MealPlan.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/domain/model/MealPlan.kt)**

| Modelo | Qué es |
|---|---|
| `PlanContext` | `budgetCop`, `servings`, `maxMinutes?`, `noSpicy`, `maxMissing` (3), `days` (3), `diet` y `store` |
| `PlannerWeights` | `rescue` 1,0, `cost` 0,3 y `waste` 0,5 |
| `DietRule` | `VEGETARIAN`, `VEGAN`, `GLUTEN_FREE`, `LACTOSE_FREE` y `NUT_FREE`, cada una con sus etiquetas prohibidas |
| `PlannedDay` | `day`, `recipe`, `uses`, `costCop`, `pantryShare` y `utility` |
| `MealPlan` | `days`, `totalCostCop`, `remainingBudgetCop`, `wastedItems` y `utility` |
| `PlanResult` | `Found(plan)` o `NoPlan(suggestions)` |
| `PlanSuggestion` | `RaiseBudget(minimumBudgetCop)`, `AllowSpicy`, `AllowMoreTime(minutes)` o `NotEnoughRecipes` |
