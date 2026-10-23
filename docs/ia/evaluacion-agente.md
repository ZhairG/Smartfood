# Evaluación del agente de recetas (spec 009)

Resultados para el informe del curso de Inteligencia Artificial (spec 009, RNF-RIA-02, RNF-RIA-03, CA-RIA-02-10, T029).
Se reproducen con:

```bash
./gradlew :app:testDebugUnitTest --tests "*AgentEvaluationTest" --tests "*MealPlannerPerformanceTest"
```

## 1. Formulación

El agente es **basado en utilidad** y arma un **plan de 3 días** (una receta por día) a partir de lo que hay en el
inventario, priorizando lo que vence primero, dentro de un **presupuesto total** para el plan. Se modela como un
**problema de satisfacción de restricciones con optimización (COP)**:

| Elemento | Definición |
|---|---|
| Variables | `D1`, `D2`, `D3`: la receta de cada día. |
| Dominio | Las 50 recetas del catálogo que cumplen las restricciones de una sola receta (consistencia de nodo): tiempo máximo, "Sin picante", restricciones alimentarias del Perfil y costo ≤ presupuesto. |
| Restricciones | Las tres recetas son distintas; el inventario es compartido entre los días (lo que usa el Día 1 ya no está el Día 2); las compras de los tres días suman ≤ presupuesto; ninguna receta necesita más de 3 ingredientes por comprar; ninguna incumple la dieta. Las compras son por empaques completos y el sobrante queda para los días siguientes. |
| Objetivo | Maximizar `U = 1,0·rescate − 0,3·(costo / 10.000) − 0,5·desperdicio`. *Rescate*: fracción usada de cada producto que vence en ≤ 3 días, ponderada por la urgencia `1/(días + 1)`. *Desperdicio*: lo que vence en ≤ 3 días y el plan no usa. |
| Búsqueda | Backtracking día por día con *forward checking* (inventario y presupuesto restantes), candidatas ordenadas por utilidad de un paso, **ramificación y poda** con una cota optimista que nunca subestima y desempate por id (determinista). |
| Sin plan | Sugerencias calculadas: presupuesto mínimo con el que habría plan, permitir picante o subir el tiempo. Nunca sugiere quitar las restricciones alimentarias. |
| Explicación | `PlanExplainer`: qué productos por vencer usa y en qué día, cuántos ingredientes aprovecha, qué falta comprar con su costo y cuánto queda del presupuesto. |

Código: `app/src/main/java/com/smartfood/app/feature/recipes/domain/agent/` (`MealPlanner`, `RecipeEvaluator`,
`PlanExplainer`, `IngredientMatcher`). La optimalidad se comprueba contra una búsqueda exhaustiva (fuerza bruta) en
`MealPlannerTest` (CA-RIA-02-04).

## 2. Método de evaluación

- **Escenarios:** 30 inventarios sintéticos con semilla fija (`RealCatalog.syntheticInventories`). Cada uno parte de una
  receta al azar, de la que toma 1 a 3 ingredientes que vencen en 0 a 3 días, más 4 a 8 productos que vencen después;
  en 3 de cada 10, aproximadamente, se agrega otro producto por vencer al azar.
- **Contexto:** catálogo real (50 recetas, 50 ingredientes con precios de supermercado), COP $100.000, 4 porciones,
  tienda D1, sin restricciones de tiempo, picante ni dieta.
- **Líneas base** (Avance 2): arman el plan día por día con las mismas restricciones duras, pero ordenando por un solo
  criterio y sin búsqueda:
  - **Por coincidencias:** la receta con más ingredientes en el inventario.
  - **Por vencimiento:** la receta que usa el producto que vence antes.

## 3. Resultados (2026-10-23)

| Método | Planes completos | Aprovechamiento del plan | Aprovechamiento del Día 1 | En presupuesto | Desperdicio medio | Costo medio |
|---|---|---|---|---|---|---|
| **Agente CSP** | 30/30 | **97,2 %** | 73,6 % | 100 % | **0,64** | **COP 22.656** |
| Línea base: coincidencias | 30/30 | 77,2 % | 74,3 % | 100 % | 1,20 | COP 49.474 |
| Línea base: vencimiento | 30/30 | 80,6 % | 70,8 % | 100 % | 1,08 | COP 50.662 |

- **Aprovechamiento del plan:** % de los productos que vencen en ≤ 3 días (y alguna receta usa) que el plan usa antes de
  que venzan. **Meta RNF-RIA-03: ≥ 80 % y superar a las dos líneas base → cumplida (97,2 %).**
- **Aprovechamiento del Día 1:** métrica del Avance 2, que recomendaba una sola receta; se reporta sin meta. Con el plan
  de 3 días, lo que vence en 2 o 3 días puede cocinarse el Día 2 o el 3 sin perderse, así que el agente reparte los
  productos entre los días (decisión de Zhair del 2026-10-20).
- **Presupuesto:** restricción dura; el 100 % de los planes cabe en el presupuesto.
- **Desperdicio y costo:** el agente deja menos de la mitad del desperdicio y gasta menos de la mitad que las líneas base,
  porque optimiza los tres días juntos (inventario y presupuesto compartidos) en lugar de elegir la mejor receta de cada
  día por separado.

## 4. Rendimiento (CA-RIA-02-10, RNF-RIA-02: ≤ 2 s con 50 recetas)

| Dónde | Peor caso | Promedio |
|---|---|---|
| JVM (`MealPlannerPerformanceTest`, 30 inventarios) | 157 ms | 23 ms |
| Teléfono Samsung SM-A356E (Android 16), APK release con R8 (T028, 4 planes) | 62 ms | 44 ms |

En el teléfono se lee con `adb logcat -s SmartFoodIA` ("Plan calculado en N ms", que registra `AiPlanViewModel`). La
pantalla "Analizando opciones" se muestra igual al menos 1,5 s (desviación aprobada, spec §9).

## 5. Limitaciones

- Los inventarios son sintéticos; falta una prueba piloto con usuarios (métricas de "calidad del ranking" y
  "aceptación" del Avance 2).
- Los pesos `1,0 / 0,3 / 0,5` son heurísticos (`PlannerWeights`).
- Los precios son una referencia de octubre de 2026 por tienda (`docs/precios/`), sin integración en vivo con los
  supermercados.
