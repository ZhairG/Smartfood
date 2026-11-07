# SmartFood: sustentación del agente de recetas

Respuesta a la retroalimentación de la Clase 24. Código y tablas completas en la rama
`docs/evaluacion-comparativa-agente` (`docs/ia/evaluacion-agente.md`, secciones 1 a 6 y anexo).

## 1. Qué es el agente y qué cambió

Es un agente **basado en utilidad** que arma un plan de 3 días, una receta por día. Lo modelamos como un **CSP con
optimización (COP)**: no solo busca un plan válido, sino el de mayor utilidad.

`U = 1,0·rescate − 0,3·(costo / 10.000) − 0,5·desperdicio`

**Variables:** `D1`, `D2`, `D3`. **Dominio:** las 70 recetas que cumplen tiempo, picante, dieta, cocina y presupuesto.
**Restricciones:** recetas distintas, inventario compartido, presupuesto total, máximo 3 ingredientes por comprar y dieta.

| Lo que pidió el profesor | Dónde está |
|---|---|
| Comparar contra otros algoritmos | Punto 5 |
| Problema de búsqueda | Punto 2 |
| Heurística: cuál, por qué y cuándo | Punto 3 |
| Admisibilidad | Punto 4 |
| Más usuarios y más datos | Punto 5: 9 perfiles, 54 evaluaciones |
| Los frijoles que no se actualizaban | Corregido (`CA-RIA-02-11`): la pantalla de la IA usaba datos viejos; el plan ahora usa el inventario y las restricciones actuales |

## Guía de estudio: qué pidió el profesor y qué cambió

Basada en la retroalimentación de la Clase 24 (transcripción de Word, 01:12 a 01:20). Todo esto ya está en `master` (PR #132).

| Minuto | Lo que dijo el profesor | Qué se hizo | Dónde mirarlo |
|---|---|---|---|
| 01:14:03 | "No se casen con solo CSP": busquen otros mecanismos para que haya una comparativa | Se comparó con A\*, recocido simulado, voraz y variantes sin heurísticas | `app/src/test/.../domain/agent/AlgorithmComparison.kt` |
| 01:14:46 | "¿Solo existe el backtracking?" | Tabla por perfil con óptimo, nodos, memoria y tiempo | Punto 5 de este archivo |
| 01:15:15 | "¿Y la heurística?", "¿cómo ayudaría a su problema?" | Se identificó la cota optimista y cuándo actúa cada heurística | `MealPlanner.kt`, función `assign()` |
| 01:15:40 | Nombró valores mínimos restantes, valor menos restrictivo y comprobación directa | Se midió el valor menos restrictivo; MRV no aplica | Punto 3 |
| 01:17:06 | "¿Cuál es el problema de búsqueda?" | Estado, acción, meta y objetivo | Punto 2 |
| 01:18:23 | "¿Por qué es admisible esa heurística?" | Argumento y prueba contra fuerza bruta en 825 estados | Punto 4 y `AlgorithmComparisonTest.kt` |
| 01:13:42 | Muchos ingredientes y varios usuarios con características diferentes | 9 perfiles, 54 evaluaciones | `RealCatalog.evaluationProfiles` |
| 01:14:46 | "Detallitos": los frijoles no aparecen | Corregido: `CA-RIA-02-11` | `AiPlanViewModel.kt` y `GetAiContextUseCase.kt` |

### El orden para estudiar

1. **Problema de búsqueda (punto 2):** es la base de todo lo demás.
2. **Heurísticas (punto 3):** aprende cuál usamos, cuándo actúa y por qué no usamos MRV.
3. **Admisibilidad (punto 4):** es la pregunta más delicada.
4. **Comparación (punto 5):** memoriza las tres conclusiones de abajo.
5. **Preguntas probables (punto 7):** practícalas en voz alta.

### Lo que debes poder explicar sin leer

- **Por qué es un COP y no solo un CSP:** no basta con un plan válido; buscamos el de mayor utilidad.
- **Qué hace la cota:** suma lo acumulado más todo el rescate que aún es posible, y descarta una rama si ni así alcanza al mejor plan.
- **La aparente contradicción:** el profesor dijo que una heurística que da un valor más alto "no es admisible". La nuestra da un valor más alto a propósito, porque **maximizamos**. Pasada a costo (`C = M − U`), nunca sobreestima: es la misma idea de la línea recta.
- **La poda es la que más ayuda:** de 9.177 a 498 nodos sin restricciones. Forward checking casi no reduce nodos, y hay que admitirlo.
- **Por qué nos quedamos con el CSP:** garantiza el óptimo con memoria mínima. A\* también lo garantiza y expande menos nodos, pero guarda hasta 110.997 estados.

### El fallo de los frijoles

- **Causa:** la pantalla de la IA seguía abierta al cambiar de pestaña, con el inventario que leyó al entrar.
- **Era más serio:** también podía ignorar una restricción alimentaria agregada después, algo grave con una alergia.
- **Corrección:** la pantalla observa el inventario y vuelve a leer todo justo antes de generar el plan. Si el inventario no se puede leer, no se cierra.
- **Cómo se comprobó:** primero se reprodujo con una prueba que fallaba, y luego se corrigió.

### Lo que no debes prometer

- Los perfiles son **sintéticos**, no usuarios reales.
- Los tiempos son de un portátil de 7,3 GB, solo como orden de magnitud.
- La admisibilidad se comprobó en esos 825 estados, junto con el argumento escrito: no es una demostración formal completa.
- Con inventario enorme, el agente promedió 2,5 s, por encima de la meta de 2 s. No se ha medido en el teléfono con release.
- El recocido se probó con un solo juego de parámetros.

### Cómo volver a generar las tablas

```bash
git switch master && git pull
SMARTFOOD_COMPARISON=1 ./gradlew :app:testDebugUnitTest --tests "*AlgorithmComparisonTest"
```

El resultado queda en `app/build/comparacion-agente.md`. Las tablas completas están en el anexo de `docs/ia/evaluacion-agente.md`.

## 2. Problema de búsqueda

| Elemento | Definición |
|---|---|
| Estado | Las recetas elegidas hasta ahora, con el inventario y el presupuesto que dejaron. |
| Estado inicial | Sin recetas, con todo el inventario y todo el presupuesto. |
| Acción | Asignar una receta al siguiente día. |
| Meta | Un plan de 3 días que cumple todas las restricciones. |
| Objetivo | Mayor utilidad `U`, no solo encontrar un plan. Por eso es un COP y no un CSP puro. |

El orden de los días es fijo: la evaluación de un día depende de lo que dejaron los anteriores, porque el inventario es
compartido y los productos vencen.

## 3. Heurísticas

| Heurística | Qué hace | Cuándo actúa | Estado |
|---|---|---|---|
| Consistencia de nodo | Quita las recetas que nunca podrían servir | Antes de buscar | Se usa |
| Forward checking (comprobación hacia adelante) | Abandona una rama si no quedan recetas para los días que faltan | Antes de probar los valores del día | Se usa |
| Orden de valores por utilidad | Prueba primero la receta que más aporta | Al elegir la receta del día | Se usa |
| Cota optimista (`h`) | Calcula lo máximo que una rama aún puede lograr y poda las que no alcanzan al mejor plan | Antes de bajar a la siguiente receta | Se usa |
| Valor menos restrictivo (LCV) | Prueba primero la receta que deja más opciones al día siguiente | Al elegir la receta del día | Se midió; no se adopta (punto 5) |
| Valores mínimos restantes (MRV) | Elige primero la variable con menos valores | Al elegir la variable | No aplica: el orden de los días es fijo (punto 2) |

## 4. Admisibilidad

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

## 5. Comparación de algoritmos

Mismo catálogo real, misma función de utilidad y mismas restricciones. 9 perfiles de usuario con 6 inventarios cada uno
(`RealCatalog.evaluationProfiles`), semilla fija. Se reproduce con:

```bash
SMARTFOOD_COMPARISON=1 ./gradlew :app:testDebugUnitTest --tests "*AlgorithmComparisonTest"
```

El resultado completo queda en `app/build/comparacion-agente.md` y en el anexo de `docs/ia/evaluacion-agente.md`. El *aprovechamiento*
de estas tablas cuenta todos los productos por vencer, también los que ninguna receta usa, así que no se compara con el 97,2 % de la evaluación anterior. Los tiempos son de un portátil con 7,3 GB de RAM y la JVM en frío: sirven como orden de magnitud.

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

## 6. Limitaciones nuevas

- **Inventario enorme:** con 8 productos por vencer, el agente promedió **2,5 s** en este equipo, por encima de la meta de
  2 s (RNF-RIA-02). La cota holgada poda poco en ese caso. Hay que medirlo en el teléfono con release; una cota más ajustada
  sería la mejora natural, revisándola contra `MealPlannerTest`.
- El recocido se probó con un solo juego de parámetros (3.000 iteraciones, `T = 1`, enfriamiento 0,998).
- Son 54 evaluaciones en 9 perfiles sintéticos, no usuarios reales.

## 7. Preguntas probables en la sustentación

| Pregunta | Respuesta |
|---|---|
| ¿Por qué CSP y no otro? | Porque garantiza el óptimo con memoria mínima. El voraz se aleja del óptimo y el recocido no lo garantiza; A\* lo halla, pero guarda hasta 110.997 estados. |
| ¿Solo existe el backtracking? | No: se compararon A\*, recocido simulado y voraz sobre los mismos datos (punto 5). |
| ¿Cuál es el problema de búsqueda? | punto 2: estado, acción, meta y objetivo. |
| ¿Qué heurística usan y cuándo actúa? | punto 3: la cota optimista, antes de bajar a cada receta, y es la que más reduce nodos. |
| ¿Por qué es admisible si "sobreestima"? | Porque se maximiza: no debe subestimar lo que falta por ganar. Pasada a costo, nunca sobreestima (punto 4). Se comprobó en 825 estados. |
| ¿Es un simple filtro? | No: el voraz, que es lo más parecido a filtrar, queda lejos del óptimo en casi todos los perfiles. |
| ¿Y con otros usuarios? | 9 perfiles: dietas, alergias, poco presupuesto o tiempo, inventario enorme, nada por vencer y sin plan. |
| ¿Dónde falla? | Con inventarios enormes supera 2 s en un portátil modesto (punto 6). |

## 8. Cocina Favorita y 70 recetas (2026-11-06)

**Qué cambió:** el catálogo pasó de 50 a 70 recetas y el agente ahora respeta la Cocina Favorita del Perfil.

- **Es una restricción dura de una sola receta:** va en la consistencia de nodo, igual que la dieta. Por eso no cambia la función
  de utilidad, la cota ni su admisibilidad. Solo se reduce el dominio.
- **Reglas:** sin cocinas elegidas no filtra; con varias, sirve cualquiera; la dieta siempre manda.
- **Si no hay plan:** el agente comprueba con la misma búsqueda si quitando la cocina lo habría, y solo entonces sugiere "Quita tu
  cocina favorita en el Perfil".
- **Resultados:** 88,9 % de aprovechamiento, sobre la meta del 80 % y por encima de las dos líneas base (85,0 % y 74,7 %).

**Lo que no debes prometer:**

- Que cada cocina siempre da plan. Con 6 inventarios sintéticos: Italiana 3, Mediterránea 2, Asiática 1, Mexicana 1 y Asiática con
  vegetariano 0. La causa es que hay pocas recetas por cocina y máximo 3 ingredientes por comprar. Zhair decidió mantenerlo.
- Que el agente cumple 2 s en todos los casos. Con el inventario enorme promedió **8,3 s** en el portátil, y ese caso **no se midió
  en el teléfono**. Con inventarios normales, el peor plan tardó 537 ms.
- Que el 88,9 % empeora por las recetas nuevas. No se compara directo con el 97,2 % anterior, porque también cambiaron los
  inventarios sintéticos.

| Pregunta | Respuesta |
|---|---|
| ¿La cocina cambia la heurística? | No. Reduce el dominio antes de buscar; la cota sigue siendo admisible. |
| ¿Por qué dura y no blanda? | Es más fácil de explicar, no cambia los pesos justificados y nunca rompe lo que pidió la persona. |
| ¿Por qué hay recetas tan sencillas? | Porque con más de 3 faltantes una receta no entra al plan con una despensa típica. |
| ¿Qué pasa si elijo una cocina y soy vegano? | Se aplican las dos. Si no hay plan, nunca se sugiere quitar la dieta. |
