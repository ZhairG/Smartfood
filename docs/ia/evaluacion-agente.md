# Evaluación del agente de recetas (spec 009)

Resultados para el informe del curso de Inteligencia Artificial (spec 009, RNF-RIA-02, RNF-RIA-03, CA-RIA-02-10, T029).
Se reproducen con:

```bash
./gradlew :app:testDebugUnitTest --tests "*AgentEvaluationTest" --tests "*MealPlannerPerformanceTest"
```

La comparación con otros algoritmos, la heurística y su admisibilidad están en la §6. La guía para estudiar la sustentación
está en [`sustentacion-agente.md`](sustentacion-agente.md).

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

## 6. Problema de búsqueda y heurísticas (2026-11-05)

Retroalimentación de la Clase 24: comparar el CSP con otros algoritmos, definir el problema de búsqueda y justificar la
heurística y su admisibilidad.

### 6.1 Problema de búsqueda

| Elemento | Definición |
|---|---|
| Estado | Las recetas elegidas hasta ahora, con el inventario y el presupuesto que dejaron. |
| Estado inicial | Sin recetas, con todo el inventario y todo el presupuesto. |
| Acción | Asignar una receta al siguiente día. |
| Meta | Un plan de 3 días que cumple todas las restricciones. |
| Objetivo | Mayor utilidad `U`, no solo encontrar un plan. Por eso es un COP y no un CSP puro. |

El orden de los días es fijo: la evaluación de un día depende de lo que dejaron los anteriores, porque el inventario es
compartido y los productos vencen.

### 6.2 Heurísticas

| Heurística | Qué hace | Cuándo actúa | Estado |
|---|---|---|---|
| Consistencia de nodo | Quita las recetas que nunca podrían servir | Antes de buscar | Se usa |
| Forward checking (comprobación hacia adelante) | Abandona una rama si no quedan recetas para los días que faltan | Antes de probar los valores del día | Se usa |
| Orden de valores por utilidad | Prueba primero la receta que más aporta | Al elegir la receta del día | Se usa |
| Cota optimista (`h`) | Calcula lo máximo que una rama aún puede lograr y poda las que no alcanzan al mejor plan | Antes de bajar a la siguiente receta | Se usa |
| Valor menos restrictivo (LCV) | Prueba primero la receta que deja más opciones al día siguiente | Al elegir la receta del día | Se midió; no se adopta (§6.4) |
| Valores mínimos restantes (MRV) | Elige primero la variable con menos valores | Al elegir la variable | No aplica: el orden de los días es fijo (§6.1) |

### 6.3 Admisibilidad

**La cota es `f(n) = g(n) + h(n)`:**
- `g(n)`: la utilidad acumulada del estado `n`.
- `h(n)`: `rescate · rescuePotential`, todo el rescate que el inventario todavía puede dar.

En el curso, una heurística es admisible si no sobreestima el costo real, como la línea recta frente a la ruta real. Aquí
se **maximiza**, así que la condición se ve al revés: **`h` no debe subestimar lo que falta por ganar**. Una `h` que
"promete de más" es lo correcto, porque solo así la poda no descarta el óptimo.

Es la misma definición. Con una constante `M` mayor que cualquier utilidad posible, el costo de un plan es
`C = M − U`. Si `U*(n)` es la mejor utilidad real alcanzable desde `n`, entonces:

```
g(n) + h(n) ≥ U*(n)    ⇔    M − (g(n) + h(n)) ≤ M − U*(n)
```

La estimación de costo nunca supera el costo real: es la línea recta del curso.

**Por qué `h` nunca subestima:**
1. `h` no resta el costo de las recetas futuras, y ese costo solo puede restar utilidad.
2. `h` no resta el desperdicio final, que también solo resta.
3. `h` suma el rescate de **todo** lo que queda sin usar, como si los días que faltan pudieran aprovecharlo por completo.
4. Ninguna receta rescata más de lo que queda de un producto. Por eso el rescate real de los días que faltan no supera
   a `h`.

**Comprobación:** `AlgorithmComparisonTest` compara la cota con la mejor utilidad real, calculada por fuerza bruta, en
**825 estados** de 30 catálogos aleatorios: **ninguno la viola**, y solo en 25 estados es exacta. Es admisible, pero
holgada: la mayoría de las veces promete más de lo que se puede lograr. Es una evidencia empírica sobre esos catálogos, que
acompaña al argumento de arriba.

### 6.4 Comparación de algoritmos

Mismo catálogo real, misma función de utilidad y mismas restricciones. 9 perfiles de usuario con 6 inventarios cada uno
(`RealCatalog.evaluationProfiles`), semilla fija. Se reproduce con:

```bash
SMARTFOOD_COMPARISON=1 ./gradlew :app:testDebugUnitTest --tests "*AlgorithmComparisonTest"
```

El resultado completo queda en `app/build/comparacion-agente.md` y en el anexo de este documento. El *aprovechamiento*
de estas tablas cuenta todos los productos por vencer, también los que ninguna receta usa, así que no se compara con el de
§3. Los tiempos son de un portátil con 7,3 GB de RAM y la JVM en frío: sirven como orden de magnitud.

**Cuánto aporta cada heurística** (nodos por inventario, promedio; todas hallan el óptimo):

| Perfil | Backtracking puro | + forward checking | + orden | + poda (agente) | Poda con LCV |
|---|---|---|---|---|---|
| Sin restricciones | 9.177 | 9.177 | 9.177 | **498** | 940 |
| Alérgico | 6.169 | 6.169 | 6.169 | **521** | 668 |
| Nada por vencer | 11.395 | 11.395 | 11.395 | **30** | 60 |
| Inventario enorme | 113.449 | 113.449 | 113.449 | **86.683** | 102.480 |

- **La poda hace casi todo el trabajo.** Forward checking casi no reduce nodos en estos perfiles, porque rara vez se agotan
  las recetas. El orden por sí solo no cambia los nodos, porque sin poda se recorre todo el árbol; su valor aparece junto
  con la poda, al encontrar pronto un buen plan.
- **LCV no se adopta:** expande más nodos que el orden por utilidad en estos perfiles y tarda de 6 a más de 50 veces más,
  porque evalúa el día siguiente para cada receta.

**Agente contra los rivales:**

| Perfil | Agente: nodos / ms | A\*: nodos / frontera / ms | Recocido: brecha / ms | Voraz: brecha / ms |
|---|---|---|---|---|
| Sin restricciones | 498 / 60 | 123 / 3.066 / 46 | 0,050 / 40 | 0,199 / 1,5 |
| Vegetariano | 422 / 13 | 91 / 1.369 / 16 | 0 / 34 | 0,154 / 0,5 |
| Alérgico | 521 / 26 | 111 / 2.361 / 31 | 0 / 34 | 0,208 / 0,9 |
| Poco presupuesto | 843 / 33 | 143 / 1.033 / 38 | 0 / 36 | 0,199 / 0,8 |
| Poco tiempo | 185 / 5 | 43 / 334 / 5 | 0 / 27 | 0,271 / 0,3 |
| Inventario enorme | 86.683 / 2.505 | 2.438 / 110.997 / 2.750 | 0 / 181 | 0,177 / 2,9 |
| Nada por vencer | 30 / 6 | 23 / 503 / 6 | 0 / 29 | 0,045 / 0,8 |

La *brecha* es la utilidad promedio que le falta para llegar al óptimo. Vegano sin lactosa no aparece porque los métodos
empatan (3 de 6 inventarios con plan, en todos). Con COP 2.000 ninguno arma plan, y ninguno incumplió una restricción dura.

**Conclusiones:**
- **Voraz:** el más rápido, pero siempre más lejos del óptimo en los perfiles con plan, salvo el empate del vegano. Elegir
  día por día no alcanza: confirma que el problema es de decisión conjunta, no un filtro.
- **Recocido simulado:** llegó al óptimo en casi todos los perfiles y fue unas 14 veces más rápido con el inventario
  enorme, pero en "Sin restricciones" falló en al menos un inventario y no tiene garantía de óptimo.
- **A\*:** halla el mismo óptimo expandiendo muchos menos nodos, pero guarda la frontera en memoria (hasta 110.997 estados)
  y su tiempo fue parecido al del agente; en esta implementación, cada expansión reconstruye el inventario. La primera versión, con el inventario en cada
  nodo, agotó los 512 MB de la prueba.
- **Agente:** se mantiene porque garantiza el óptimo, usa memoria mínima (solo la rama actual) y ya funciona bien en los
  casos normales. Con todo, A\* es un rival serio: la decisión es por memoria, no por nodos.

### 6.5 Limitaciones nuevas

- **Inventario enorme:** con 8 productos por vencer, el agente promedió **2,5 s** en este equipo, por encima de la meta de
  2 s (RNF-RIA-02). La cota holgada poda poco en ese caso. Hay que medirlo en el teléfono con release; una cota más ajustada
  sería la mejora natural, revisándola contra `MealPlannerTest`.
- El recocido se probó con un solo juego de parámetros (3.000 iteraciones, `T = 1`, enfriamiento 0,998).
- Son 54 evaluaciones en 9 perfiles sintéticos, no usuarios reales.

### 6.6 Preguntas probables en la sustentación

| Pregunta | Respuesta |
|---|---|
| ¿Por qué CSP y no otro? | Porque garantiza el óptimo con memoria mínima. El voraz se aleja del óptimo y el recocido no lo garantiza; A\* lo halla, pero guarda hasta 110.997 estados. |
| ¿Solo existe el backtracking? | No: se compararon A\*, recocido simulado y voraz sobre los mismos datos (§6.4). |
| ¿Cuál es el problema de búsqueda? | §6.1: estado, acción, meta y objetivo. |
| ¿Qué heurística usan y cuándo actúa? | §6.2: la cota optimista, antes de bajar a cada receta, y es la que más reduce nodos. |
| ¿Por qué es admisible si "sobreestima"? | Porque se maximiza: no debe subestimar lo que falta por ganar. Pasada a costo, nunca sobreestima (§6.3). Se comprobó en 825 estados. |
| ¿Es un simple filtro? | No: el voraz, que es lo más parecido a filtrar, queda lejos del óptimo en casi todos los perfiles. |
| ¿Y con otros usuarios? | 9 perfiles: dietas, alergias, poco presupuesto o tiempo, inventario enorme, nada por vencer y sin plan. |
| ¿Dónde falla? | Con inventarios enormes supera 2 s en un portátil modesto (§6.5). |

## Anexo: tablas completas por perfil (2026-11-05)

### Sin restricciones (6 inventarios)

| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |
|---|---|---|---|---|---|---|---|---|---|
| Agente CSP (orden + FC + poda) | 6/6 | -0.318 | 0.000 | 100.0 % | 0.53 | 20238 | 498 | 0 | 60.0 |
| Backtracking puro | 6/6 | -0.318 | 0.000 | 100.0 % | 0.53 | 20238 | 9177 | 0 | 138.6 |
| Backtracking + FC | 6/6 | -0.318 | 0.000 | 100.0 % | 0.53 | 20238 | 9177 | 0 | 112.5 |
| Backtracking + FC + orden | 6/6 | -0.318 | 0.000 | 100.0 % | 0.53 | 20238 | 9177 | 0 | 114.9 |
| Backtracking + FC + poda + LCV | 6/6 | -0.318 | 0.000 | 100.0 % | 0.53 | 20238 | 940 | 0 | 626.5 |
| A* | 6/6 | -0.318 | 0.000 | 100.0 % | 0.53 | 20238 | 123 | 3066 | 46.3 |
| Recocido simulado | 6/6 | -0.368 | 0.050 | 100.0 % | 0.53 | 21895 | 2850 | 0 | 40.1 |
| Voraz | 6/6 | -0.517 | 0.199 | 66.7 % | 0.88 | 17470 | 3 | 0 | 1.5 |

### Vegetariano (6 inventarios)

| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |
|---|---|---|---|---|---|---|---|---|---|
| Agente CSP (orden + FC + poda) | 6/6 | -0.367 | 0.000 | 83.3 % | 0.71 | 16923 | 422 | 0 | 12.8 |
| Backtracking puro | 6/6 | -0.367 | 0.000 | 83.3 % | 0.71 | 16923 | 2873 | 0 | 25.3 |
| Backtracking + FC | 6/6 | -0.367 | 0.000 | 83.3 % | 0.71 | 16923 | 2873 | 0 | 25.0 |
| Backtracking + FC + orden | 6/6 | -0.367 | 0.000 | 83.3 % | 0.71 | 16923 | 2873 | 0 | 25.5 |
| Backtracking + FC + poda + LCV | 6/6 | -0.367 | 0.000 | 83.3 % | 0.71 | 16923 | 733 | 0 | 141.1 |
| A* | 6/6 | -0.367 | 0.000 | 83.3 % | 0.71 | 16923 | 91 | 1369 | 16.3 |
| Recocido simulado | 6/6 | -0.367 | 0.000 | 83.3 % | 0.71 | 16923 | 2667 | 0 | 34.1 |
| Voraz | 6/6 | -0.521 | 0.154 | 66.7 % | 0.89 | 17300 | 3 | 0 | 0.5 |

### Vegano sin lactosa (6 inventarios)

| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |
|---|---|---|---|---|---|---|---|---|---|
| Agente CSP (orden + FC + poda) | 3/6 | -0.168 | 0.000 | 100.0 % | 0.76 | 12073 | 120 | 0 | 1.7 |
| Backtracking puro | 3/6 | -0.168 | 0.000 | 100.0 % | 0.76 | 12073 | 244 | 0 | 2.0 |
| Backtracking + FC | 3/6 | -0.168 | 0.000 | 100.0 % | 0.76 | 12073 | 244 | 0 | 1.9 |
| Backtracking + FC + orden | 3/6 | -0.168 | 0.000 | 100.0 % | 0.76 | 12073 | 244 | 0 | 1.9 |
| Backtracking + FC + poda + LCV | 3/6 | -0.168 | 0.000 | 100.0 % | 0.76 | 12073 | 149 | 0 | 8.9 |
| A* | 3/6 | -0.168 | 0.000 | 100.0 % | 0.76 | 12073 | 26 | 183 | 2.0 |
| Recocido simulado | 3/6 | -0.168 | 0.000 | 100.0 % | 0.76 | 12073 | 2120 | 0 | 22.1 |
| Voraz | 3/6 | -0.168 | 0.000 | 100.0 % | 0.76 | 12073 | 2 | 0 | 0.1 |

### Alérgico: sin gluten ni frutos secos (6 inventarios)

| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |
|---|---|---|---|---|---|---|---|---|---|
| Agente CSP (orden + FC + poda) | 6/6 | -0.568 | 0.000 | 75.0 % | 0.70 | 20185 | 521 | 0 | 25.5 |
| Backtracking puro | 6/6 | -0.568 | 0.000 | 75.0 % | 0.70 | 20185 | 6169 | 0 | 61.0 |
| Backtracking + FC | 6/6 | -0.568 | 0.000 | 75.0 % | 0.70 | 20185 | 6169 | 0 | 61.3 |
| Backtracking + FC + orden | 6/6 | -0.568 | 0.000 | 75.0 % | 0.70 | 20185 | 6169 | 0 | 65.1 |
| Backtracking + FC + poda + LCV | 6/6 | -0.568 | 0.000 | 75.0 % | 0.70 | 20185 | 668 | 0 | 358.7 |
| A* | 6/6 | -0.568 | 0.000 | 75.0 % | 0.70 | 20185 | 111 | 2361 | 30.6 |
| Recocido simulado | 6/6 | -0.568 | 0.000 | 75.0 % | 0.70 | 20185 | 2783 | 0 | 34.0 |
| Voraz | 6/6 | -0.776 | 0.208 | 50.0 % | 1.04 | 18313 | 3 | 0 | 0.9 |

### Poco presupuesto (COP 25.000, 2 porciones) (6 inventarios)

| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |
|---|---|---|---|---|---|---|---|---|---|
| Agente CSP (orden + FC + poda) | 6/6 | -0.478 | 0.000 | 100.0 % | 0.76 | 16993 | 843 | 0 | 33.0 |
| Backtracking puro | 6/6 | -0.478 | 0.000 | 100.0 % | 0.76 | 16993 | 1515 | 0 | 41.9 |
| Backtracking + FC | 6/6 | -0.478 | 0.000 | 100.0 % | 0.76 | 16993 | 1514 | 0 | 43.6 |
| Backtracking + FC + orden | 6/6 | -0.478 | 0.000 | 100.0 % | 0.76 | 16993 | 1514 | 0 | 49.9 |
| Backtracking + FC + poda + LCV | 6/6 | -0.478 | 0.000 | 100.0 % | 0.76 | 16993 | 896 | 0 | 257.2 |
| A* | 6/6 | -0.478 | 0.000 | 100.0 % | 0.76 | 16993 | 143 | 1033 | 37.9 |
| Recocido simulado | 6/6 | -0.478 | 0.000 | 100.0 % | 0.76 | 16993 | 2852 | 0 | 36.0 |
| Voraz | 6/6 | -0.677 | 0.199 | 91.7 % | 1.09 | 15012 | 3 | 0 | 0.8 |

### Poco tiempo (30 min) (6 inventarios)

| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |
|---|---|---|---|---|---|---|---|---|---|
| Agente CSP (orden + FC + poda) | 6/6 | -0.906 | 0.000 | 72.2 % | 0.94 | 28458 | 185 | 0 | 4.7 |
| Backtracking puro | 6/6 | -0.906 | 0.000 | 72.2 % | 0.94 | 28458 | 575 | 0 | 7.4 |
| Backtracking + FC | 6/6 | -0.906 | 0.000 | 72.2 % | 0.94 | 28458 | 575 | 0 | 6.5 |
| Backtracking + FC + orden | 6/6 | -0.906 | 0.000 | 72.2 % | 0.94 | 28458 | 575 | 0 | 7.4 |
| Backtracking + FC + poda + LCV | 6/6 | -0.906 | 0.000 | 72.2 % | 0.94 | 28458 | 244 | 0 | 29.4 |
| A* | 6/6 | -0.906 | 0.000 | 72.2 % | 0.94 | 28458 | 43 | 334 | 5.4 |
| Recocido simulado | 6/6 | -0.906 | 0.000 | 72.2 % | 0.94 | 28458 | 2564 | 0 | 26.5 |
| Voraz | 6/6 | -1.178 | 0.271 | 47.2 % | 1.12 | 32282 | 3 | 0 | 0.3 |

### Inventario enorme (6 inventarios)

| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |
|---|---|---|---|---|---|---|---|---|---|
| Agente CSP (orden + FC + poda) | 6/6 | -0.873 | 0.000 | 58.3 % | 4.50 | 12443 | 86683 | 0 | 2504.6 |
| Backtracking puro | 6/6 | -0.873 | 0.000 | 58.3 % | 4.50 | 12443 | 113449 | 0 | 2521.8 |
| Backtracking + FC | 6/6 | -0.873 | 0.000 | 58.3 % | 4.50 | 12443 | 113449 | 0 | 2548.9 |
| Backtracking + FC + orden | 6/6 | -0.873 | 0.000 | 58.3 % | 4.50 | 12443 | 113449 | 0 | 2554.9 |
| Backtracking + FC + poda + LCV | 6/6 | -0.873 | 0.000 | 58.3 % | 4.50 | 12443 | 102480 | 0 | 21673.0 |
| A* | 6/6 | -0.873 | 0.000 | 58.3 % | 4.50 | 12443 | 2438 | 110997 | 2749.5 |
| Recocido simulado | 6/6 | -0.873 | 0.000 | 58.3 % | 4.50 | 12443 | 2820 | 0 | 180.6 |
| Voraz | 6/6 | -1.050 | 0.177 | 54.2 % | 4.91 | 8397 | 3 | 0 | 2.9 |

### Nada por vencer (6 inventarios)

| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |
|---|---|---|---|---|---|---|---|---|---|
| Agente CSP (orden + FC + poda) | 6/6 | -0.459 | 0.000 | — | 0.00 | 15285 | 30 | 0 | 6.1 |
| Backtracking puro | 6/6 | -0.459 | 0.000 | — | 0.00 | 15285 | 11395 | 0 | 103.5 |
| Backtracking + FC | 6/6 | -0.459 | 0.000 | — | 0.00 | 15285 | 11395 | 0 | 103.4 |
| Backtracking + FC + orden | 6/6 | -0.459 | 0.000 | — | 0.00 | 15285 | 11395 | 0 | 108.1 |
| Backtracking + FC + poda + LCV | 6/6 | -0.459 | 0.000 | — | 0.00 | 15285 | 60 | 0 | 339.5 |
| A* | 6/6 | -0.459 | 0.000 | — | 0.00 | 15285 | 23 | 503 | 5.7 |
| Recocido simulado | 6/6 | -0.459 | 0.000 | — | 0.00 | 15285 | 2841 | 0 | 28.6 |
| Voraz | 6/6 | -0.504 | 0.045 | — | 0.00 | 16797 | 3 | 0 | 0.8 |

### Sin plan posible (COP 2.000) (6 inventarios)

| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |
|---|---|---|---|---|---|---|---|---|---|
| Agente CSP (orden + FC + poda) | 0/6 | — | — | — | — | — | 1 | 0 | 0.0 |
| Backtracking puro | 0/6 | — | — | — | — | — | 1 | 0 | 0.0 |
| Backtracking + FC | 0/6 | — | — | — | — | — | 1 | 0 | 0.0 |
| Backtracking + FC + orden | 0/6 | — | — | — | — | — | 1 | 0 | 0.0 |
| Backtracking + FC + poda + LCV | 0/6 | — | — | — | — | — | 1 | 0 | 0.0 |
| A* | 0/6 | — | — | — | — | — | 1 | 1 | 0.0 |
| Recocido simulado | 0/6 | — | — | — | — | — | 0 | 0 | 0.0 |
| Voraz | 0/6 | — | — | — | — | — | 1 | 0 | 0.0 |
