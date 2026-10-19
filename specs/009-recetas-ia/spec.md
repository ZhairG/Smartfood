# Spec 009: Recetas + IA (agente de plan de 3 días)

| Campo | Valor |
|---|---|
| Feature | `009-recetas-ia` (Módulo 4) |
| Estado | Borrador para aprobación |
| Fecha | 2026-10-18 |
| Depende de | 006-inventario, 008-perfil |
| Afecta | Pestañas Recetas y Guardadas, inventario (descuento al cocinar), tarjetas del Perfil, reglas de Firestore |
| Entrega | Viernes 23 de octubre de 2026 |

> Los códigos llevan el prefijo `RIA` (`HU-RIA-01`, `CA-RIA-01-01`). `REC` ya lo usa la spec 003.

## 1. Contexto y motivación

Es la parte más importante del proyecto para el curso de Inteligencia Artificial. El Avance 2 definió un **agente
basado en utilidad** que recomienda recetas de un catálogo propio según el inventario, los vencimientos y el
presupuesto. El profesor pidió precisar la formulación computacional: **variables de decisión, dominios,
restricciones duras y función objetivo**, y verlo como un **problema de satisfacción de restricciones con búsqueda**,
"no como un simple filtro" (retroalimentación escrita y sustentación del 2026-10).

Decisiones de Zhair (2026-10-18): el agente arma un **plan de 3 días**, el **presupuesto es total para el plan**, no
se usan restricciones de dieta en esta versión, el catálogo vive **dentro de la app** y los ingredientes se emparejan
por **nombre normalizado y sinónimos**.

## 2. Formulación del agente (CSP con optimización)

| Elemento | Definición |
|---|---|
| **Variables** | `D1`, `D2`, `D3`: la receta que se cocina cada día. |
| **Dominio** | Las recetas del catálogo que pasan las restricciones de una sola receta (consistencia de nodo). |
| **Restricciones de una receta** | tiempo total ≤ tiempo máximo elegido; sin picante si se eligió "Sin picante"; como máximo `M` ingredientes por comprar (`M` = 3). |
| **Restricciones entre recetas** | (1) `D1`, `D2` y `D3` distintas; (2) **inventario compartido**: lo que usan las tres no supera lo disponible (lo que falta se compra); (3) **presupuesto total**: lo que hay que comprar para las tres, escalado a las porciones, no supera el presupuesto máximo. |
| **Objetivo** | Maximizar `U = w₁·rescate − w₂·costo − w₃·desperdicio`: *rescate* = cantidad de ingredientes que vencen en ≤ 3 días y se usan **antes** de vencer, ponderada por urgencia `1/(días + 1)`; *costo* = compras del plan en COP; *desperdicio* = lo que vence dentro de los 3 días y el plan no usa. Pesos iniciales `w₁ = 1`, `w₂ = 0,3`, `w₃ = 0,5` (heurísticos, se justifican con prueba de sensibilidad). |
| **Búsqueda** | Backtracking que asigna `D1`, `D2`, `D3` en orden; **forward checking** quita del dominio de los días pendientes las recetas que ya no caben en el inventario o el presupuesto restante; **ramificación y poda** descarta un plan parcial cuya utilidad máxima posible no supera la del mejor plan encontrado. |
| **Por qué no es un filtro** | Las recetas compiten por el mismo inventario y presupuesto: las tres mejores recetas por separado no forman el mejor plan. Con 50 recetas hay ~118.000 planes (50·49·48). |

## 3. Historias de usuario y criterios de aceptación

### HU-RIA-01: Descubrir recetas
| Código | Criterio |
|---|---|
| CA-RIA-01-01 | **Dado** la pestaña Recetas (Figma `557:3028`), **entonces** veo la búsqueda "Buscar receta o ingrediente...", la tarjeta "¿Qué cocinamos hoy?" con "Sugerirme recetas" y los filtros "Para ti", "≤ 30 min" y "Económicas" con las recetas en tarjetas (foto, nombre, tiempo, kcal, costo y ♡). |
| CA-RIA-01-02 | **Dado** "Para ti", **entonces** las recetas se ordenan por la utilidad de cada una con mi inventario actual, y la que usa un ingrediente próximo a vencer muestra "Usa <ingrediente> próximo a vencer". |
| CA-RIA-01-03 | **Dado** "≤ 30 min" o "Económicas", **entonces** solo veo recetas de 30 minutos o menos, o las de menor costo de compra. |
| CA-RIA-01-04 | **Dado** que escribo en la búsqueda, **entonces** veo las recetas cuyo nombre o ingredientes contienen el texto, sin importar mayúsculas ni tildes. |
| CA-RIA-01-05 | **Dado** el ♡ de una receta, **cuando** lo toco, **entonces** la receta se guarda o se quita de Guardadas. |

### HU-RIA-02: Pedirle un plan a la IA
| Código | Criterio |
|---|---|
| CA-RIA-02-01 | **Dado** que toco "Sugerirme recetas", **entonces** veo "Sugerencia con IA" (Figma `633:2948`): los ingredientes de mi inventario detectados, el aviso del que vence primero ("⚠ El pollo vence mañana."), el presupuesto máximo con un control deslizante (COP $2.000 a $100.000), las porciones, "Sin picante" y el tiempo máximo, y "Generar recomendaciones". |
| CA-RIA-02-02 | **Dado** que toco "Generar recomendaciones", **entonces** veo "Analizando opciones" (Figma `666:3546`) con el arco de progreso, el texto con mi presupuesto y "Cancelar", que vuelve al contexto sin resultado. |
| CA-RIA-02-03 | **Dado** que existe al menos un plan que cumple todas las restricciones, **entonces** veo **"Tu plan de 3 días"** con Día 1, Día 2 y Día 3, cada uno con la tarjeta de su receta (foto, nombre, % del inventario usado, costo y tiempo), y la tarjeta "¿Por qué te conviene?". |
| CA-RIA-02-04 | **Dado** el plan, **entonces** es el de **mayor utilidad** entre todos los planes válidos (comprobado contra una búsqueda exhaustiva en las pruebas). |
| CA-RIA-02-05 | **Dado** el plan, **entonces** ninguna receta pasa el tiempo máximo ni es picante si elegí "Sin picante", las tres son distintas, sus compras juntas no pasan el presupuesto y ninguna necesita más de 3 ingredientes por comprar. |
| CA-RIA-02-06 | **Dado** "¿Por qué te conviene?", **entonces** explica con datos del plan: qué ingredientes por vencer usa y en qué día, cuántos ingredientes del inventario aprovecha, qué falta comprar con su costo y cuánto queda del presupuesto. |
| CA-RIA-02-07 | **Dado** que ningún plan cumple las restricciones, **entonces** veo "Sin coincidencias" (Figma `557:2515`) con el presupuesto que usé y sugerencias calculadas (por ejemplo, el presupuesto mínimo con el que sí habría plan), "Ajustar contexto" (vuelve al contexto) y "Ver recetas económicas" (Descubrir con el filtro "Económicas"). |
| CA-RIA-02-08 | **Dado** "Ajustar" en el plan, **entonces** vuelvo al contexto con los mismos datos para cambiarlos. |
| CA-RIA-02-09 | **Dado** 50 recetas en el catálogo, **entonces** el plan se calcula en 2 segundos o menos en el teléfono (meta del Avance 2). |

### HU-RIA-03: Entender cómo decide la IA
| Código | Criterio |
|---|---|
| CA-RIA-03-01 | **Dado** "Ayuda" en el contexto, **entonces** veo "¿Cómo decide la IA?" (Figma `557:2476`) con urgencia de vencimiento, aprovechamiento y presupuesto, "Controles y seguridad", el aviso "La IA puede equivocarse" y "Contactar soporte", que abre el correo del equipo. |

### HU-RIA-04: Ver una receta y cocinarla
| Código | Criterio |
|---|---|
| CA-RIA-04-01 | **Dado** una receta (Figma `557:2856`), **entonces** veo su foto, preparación, cocción, reposo y dificultad, y los ingredientes agrupados (Verduras, Cereales, Carnes…) con la cantidad para las porciones elegidas y el costo de lo que me falta; los que tengo en el inventario se marcan como "En tu inventario". |
| CA-RIA-04-02 | **Dado** el selector de porciones (− 4 porciones +), **cuando** lo cambio, **entonces** las cantidades y costos se recalculan. |
| CA-RIA-04-03 | **Dado** "Empezar a cocinar", **entonces** veo "Antes de empezar" (Figma `557:2819`, sin video, §9) con "Prepara antes de iniciar" y "Comenzar guía". |
| CA-RIA-04-04 | **Dado** la cocina guiada (Figma `557:2781`), **entonces** veo "Cocinando · paso N de T", el paso con su texto, "Anterior", "Siguiente" y "Salir"; si el paso tiene tiempo, un temporizador que puedo iniciar, pausar y reiniciar, y que avisa con sonido y vibración al llegar a 0. |
| CA-RIA-04-05 | **Dado** el último paso, **cuando** toco "Terminar", **entonces** la app pregunta "¿Cocinaste esta receta?"; si confirmo, se descuentan del inventario las cantidades usadas (se borra el producto que queda en 0 o que no tenía cantidad) y se registra lo que se rescató. |
| CA-RIA-04-06 | **Dado** que cociné una receta, **entonces** "Ahorrados este mes" del Perfil suma el valor de los ingredientes por vencer que usé este mes (CA-PER-01-02). |

### HU-RIA-05: Mis recetas guardadas
| Código | Criterio |
|---|---|
| CA-RIA-05-01 | **Dado** la pestaña Guardadas (Figma `633:2723`), **entonces** veo mis recetas favoritas con búsqueda y los filtros "Favoritas" y "Recientes" (cocinadas por última vez). |
| CA-RIA-05-02 | **Dado** el Perfil, **entonces** "Recetas guardadas" muestra cuántas favoritas tengo (CA-PER-01-02). |
| CA-RIA-05-03 | **Dado** que no tengo conexión, **entonces** Guardadas sigue mostrando mis recetas y el aviso "Sin conexión · Tus recetas guardadas siguen disponibles. Los cambios se sincronizarán después." |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-RIA-01 | Catálogo de 30 a 50 recetas colombianas y tabla de ingredientes (sinónimos, categoría, precio de referencia COP, vida útil) dentro de la app. | HU-RIA-01, 04 |
| RF-RIA-02 | Emparejamiento inventario ↔ receta por nombre normalizado y sinónimos, con conversión de unidades. | HU-RIA-01, 02, 04 |
| RF-RIA-03 | Agente CSP de plan de 3 días con la formulación de §2 y explicación del resultado. | HU-RIA-02, 03 |
| RF-RIA-04 | Cocina guiada con temporizador y descuento del inventario al terminar. | HU-RIA-04 |
| RF-RIA-05 | Favoritas e historial de cocinadas en Firestore; contadores del Perfil. | HU-RIA-04, 05 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-RIA-01 | El agente vive en `domain` (Kotlin puro) y es determinista: el mismo inventario y contexto dan el mismo plan. |
| RNF-RIA-02 | Rendimiento: ≤ 2 s con 50 recetas en el teléfono (CA-RIA-02-09). |
| RNF-RIA-03 | Evaluación (métricas del Avance 2): en 30 inventarios sintéticos, el agente supera en aprovechamiento a las dos líneas base (ordenar solo por coincidencia de ingredientes y solo por vencimiento), y la receta del Día 1 usa ≥ 80 % de los ingredientes que vencen en ≤ 3 días cuando existe una receta que los use. |
| RNF-RIA-04 | Seguridad: reglas de Firestore para `favorites` y `cookings` (solo el dueño, datos validados) probadas en `rules-test/`. |
| RNF-RIA-05 | Las fotos del catálogo son propias o de licencia libre, con su fuente anotada en `docs/licenses/`. |
| RNF-RIA-06 | Textos en `res/values/strings_recipes.xml`; accesibilidad como RNF-04. |

## 6. Fuera de alcance

- Videos reales de las recetas y "Omitir video".
- "Creadas por mí" (recetas propias) y editar recetas.
- Restricciones de dieta en el agente (decisión del 2026-10-18; las preferencias del Perfil quedan guardadas).
- Precios reales de supermercados (se usa la tabla de referencia del catálogo).
- Aprendizaje de pesos con datos de uso.

## 7. Supuestos

- Un producto del inventario sin cantidad cuenta como "suficiente para una receta" y se usa completo.
- Las cantidades se convierten entre g/kg/lb y ml/l; las unidades ("und") no se convierten a peso.
- El precio de lo que falta se calcula con la tabla del catálogo, escalado a la cantidad necesaria.

## 8. Fidelidad con Figma

Capturas en [`disenos-figma/Modulo 4 - Recetas + IA`](../../disenos-figma/README.md).

| Pantalla | Frame |
|---|---|
| Recetas: descubrir | `557:3028` |
| Sugerencia con IA (contexto) | `633:2948`, `666:3482` |
| Analizando opciones | `666:3546`, `557:2946`; arco "Progreso" `281:272` |
| Plan (antes "2 opciones para ti") | `557:2903` (ver §9) |
| Sin coincidencias | `557:2515` |
| Ayuda | `557:2476` |
| Detalle de receta | `557:2856` |
| Antes de empezar | `557:2819` |
| Cocina guiada | `557:2781` |
| Guardadas | `633:2723` |

**Interacciones del prototipo:** son instantáneas (las transiciones siguen la spec 007). "Analizando opciones" pasa
solo al resultado (`AFTER_TIMEOUT`); en la app pasa cuando el agente termina.

## 9. Desviaciones (aprobadas por Zhair el 2026-10-18)

| Desviación | Motivo |
|---|---|
| "2 opciones para ti" pasa a **"Tu plan de 3 días"** con Día 1, 2 y 3, con el mismo estilo de tarjetas. | Formulación del agente como plan de 3 días (decisión de Zhair). |
| "Antes de empezar" sin video ni "Omitir video": muestra la foto de la receta. | No hay videos; quedan fuera de alcance. |
| Guardadas sin "Creadas por mí". | Crear recetas queda fuera de alcance. |
| El cierre de la cocina guiada pregunta "¿Cocinaste esta receta?". | Necesario para descontar el inventario sin errores. |
| "Carulla - Optimizado para tu zona" se muestra como "Precios de referencia". | No hay integración con supermercados. |

## 10. Plan de entrega

Un PR para el módulo (`feature/009-recetas-ia` → `master`) con **5 Issues verticales** y **un commit por tarea** (`Tnnn`).

| Issue | Contenido | Tareas | Dificultad | Puede ir en paralelo con |
|---|---|---|---|---|
| 1. Catálogo, Descubrir y Detalle | JSON, modelos, emparejamiento, pestaña Recetas, detalle con porciones | T002-T008 | intermedia | 2 |
| 2. Agente CSP y evaluación | Solo `domain` y pruebas: formulación, búsqueda, explicación, evaluación del Avance 2 | T009-T013 | avanzada | 1 |
| 3. Flujo de la IA | Contexto, cargando, plan, sin coincidencias, ayuda | T014-T018 | intermedia | — (después de 1 y 2) |
| 4. Cocina guiada | Antes de empezar, pasos, temporizador, descuento e historial, ahorro en el Perfil | T019-T023 | intermedia | 5 |
| 5. Guardadas | Favoritas, pestaña Guardadas, contador del Perfil, reglas | T024-T026 | inicial | 4 |
