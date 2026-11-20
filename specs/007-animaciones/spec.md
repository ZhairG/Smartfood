# Spec 007: Animaciones, transiciones y efectos

| Campo | Valor |
|---|---|
| Feature | `007-animaciones` |
| Estado | Implementada, pruebas y medición en la ronda final |
| Fecha | 2026-10-18 (borrador); 2026-11-19 (extendida y aprobada) |
| Depende de | 001-autenticacion, 002-adaptabilidad-ui, 006-inventario, 008-perfil, 009-recetas-ia, 010-recetas-guardadas, 013-modo-oscuro |
| Afecta | Navegación, design system (campos, diálogos y hojas), formularios de autenticación, inventario, Perfil, Recetas + IA, cocina guiada y Guardadas |
| Entrega | Un PR desde `feature/007-animaciones` (sale de `feature/013-modo-oscuro`) |

> Los códigos de esta spec llevan el prefijo `ANI` (`HU-ANI-01`, `CA-ANI-01-01`, `RNF-ANI-01`).

## 1. Contexto y motivación

Quien usa la app dice que la navegación y casi todas las animaciones "se ven muy rápidas y no van con fluidez",
incluidos los campos de texto y el teclado. Antes de escribir esta spec se midió la app en un vivo V2436 (Android 16)
con `adb screenrecord`, las marcas de tiempo reales de cada cuadro y `dumpsys gfxinfo framestats`. También se midieron
Beep, Rappi y Nequi, y se leyeron las 856 interacciones del prototipo de Figma.

**Actualización del 2026-11-19.** El prototipo tiene hoy 727 interacciones. Las de autenticación e inventario siguen
iguales. Perfil, Recetas, IA y Guardadas, que llegaron después del borrador, navegan **sin transición** (316
navegaciones instantáneas); la barra inferior usa disolvencia de 100 ms, ease-out. Las specs 008 a 010 ya dicen que sus
transiciones "siguen la spec 007", así que esta spec las extiende a esos módulos (HU-ANI-06, §8).

| Animación | SmartFood hoy | Figma (prototipo) | Beep / Rappi / Nequi |
|---|---|---|---|
| Entrar a otra pantalla | Empuje de 600 ms, FastOutSlowIn | **Empuje a la izquierda, 300 ms, ease-in-out** | 270-370 ms |
| Volver | Empuje de 600 ms | **Empuje a la derecha, 300 ms** | 200-370 ms |
| Pestañas | Cambio instantáneo | **Disolvencia de 100 ms, ease-out** | — |
| Teclado | El encabezado de marca desaparece de golpe (50-100 ms) mientras el teclado sube en ~300 ms | — | El contenido no salta |
| Foco de un campo | El borde cambia de color al instante | Smart Animate de 150 ms, ease-out (ojo y checklist) | — |
| Hoja de categoría | ~180 ms al abrir y ~150 ms al cerrar (valores por defecto) | **Disolvencia de 300 ms** | Diálogos de 290-350 ms |
| Diálogos y avisos | Valores por defecto, sin tokens comunes | Disolvencia de 300 ms (algunos con *back*) | 300-440 ms |
| Resultados y filtros de búsqueda | Sin animación | **Smart Animate de 300 ms, ease-in-out** | — |

**Hallazgo principal.** En el build **debug**, el primer cuadro de cada pantalla nueva tarda 170-260 ms y luego hay un
hueco de ~250 ms sin cuadros, así que se pierde la primera mitad de la animación y la pantalla aparece "de golpe". En
el build **release** (R8) la misma transición dibuja ~35 cuadros seguidos. Por eso la fluidez se evalúa en release
(RNF-ANI-01), y la duración se corrige según el Figma.

## 2. Actores

- **Usuario de SmartFood:** navega, llena formularios y usa el inventario.
- **Equipo de desarrollo:** necesita un solo lugar para las duraciones y las curvas.

## 3. Historias de usuario y criterios de aceptación

### HU-ANI-01: Movimiento coherente
**Como** usuario **quiero** que todas las animaciones tengan el mismo ritmo **para** que la app se sienta fluida y
predecible.

| Código | Criterio |
|---|---|
| CA-ANI-01-01 | **Dado** cualquier animación de la app, **entonces** su duración y su curva salen de los tokens de movimiento del design system. Ninguna pantalla declara una duración o una curva sueltas. |
| CA-ANI-01-02 | **Dado** que el teléfono tiene "Quitar animaciones" (escala de animación 0), **cuando** navego o abro un diálogo, **entonces** el cambio es inmediato y la app queda en el mismo estado final que con animaciones. |
| CA-ANI-01-03 | **Dado** el build release en un teléfono de gama media (V2436), **cuando** ocurre cualquier transición de esta spec, **entonces** el movimiento empieza en el primer o segundo cuadro y se ve continuo (RNF-ANI-01). |

### HU-ANI-02: Navegación entre pantallas
**Como** usuario **quiero** que al entrar o volver de una pantalla el cambio se vea como en el diseño **para**
entender a dónde voy.

| Código | Criterio |
|---|---|
| CA-ANI-02-01 | **Dado** una pantalla, **cuando** entro a otra (Registro, Recuperación, documentos legales, editor, búsqueda, cámara), **entonces** la nueva entra desde la derecha y la anterior sale hacia la izquierda en **300 ms** con curva ease-in-out (Figma). |
| CA-ANI-02-02 | **Dado** una pantalla interna, **cuando** vuelvo (flecha, enlace o "atrás" del sistema), **entonces** la anterior entra desde la izquierda y la actual sale hacia la derecha en **300 ms**. |
| CA-ANI-02-03 | **Dado** la barra inferior, **cuando** cambio entre Inventario, Recetas, Guardadas y Perfil, **entonces** el contenido cambia con una **disolvencia de 100 ms**, ease-out, sin empuje. Hoy Guardadas hace un empuje porque no está en la lista de pestañas; se corrige. |
| CA-ANI-02-04 | **Dado** la Splash, **cuando** termina, **entonces** el Login o la lista entran con el empuje de CA-ANI-02-01. |
| CA-ANI-02-05 | **Dado** una transición en curso, **cuando** toco otro destino o "atrás", **entonces** la app no se bloquea ni queda una pantalla a medio camino. |

### HU-ANI-03: Campos de texto y teclado
**Como** usuario **quiero** que el foco de los campos y la aparición del teclado se vean suaves **para** no perder de
vista lo que estoy escribiendo.

| Código | Criterio |
|---|---|
| CA-ANI-03-01 | **Dado** un campo, **cuando** recibe o pierde el foco, **entonces** el color del borde y del ícono cambia en **150 ms**, ease-out. Lo mismo vale para el estado de error. |
| CA-ANI-03-02 | **Dado** un formulario de autenticación, **cuando** se abre el teclado, **entonces** el encabezado de marca se colapsa **al mismo ritmo que sube el teclado**, sin desaparecer de golpe, y el panel acompaña ese movimiento. |
| CA-ANI-03-03 | **Dado** el teclado abierto, **cuando** se cierra, **entonces** el encabezado vuelve a aparecer al ritmo del teclado. |
| CA-ANI-03-04 | **Dado** el campo de contraseña, **cuando** recibe el foco, **entonces** el checklist aparece con disolvencia de **150 ms**, siempre encima del campo (spec 002, CA-UI-03-04). Al mostrar u ocultar la contraseña, el ícono del ojo cambia en 150 ms. |
| CA-ANI-03-05 | **Dado** el editor de producto o la búsqueda, **cuando** se abre el teclado, **entonces** el campo enfocado queda visible mientras el teclado sube, sin un salto ni un scroll posterior (spec 002, CA-UI-02-04). |

### HU-ANI-04: Diálogos, hojas y avisos
**Como** usuario **quiero** que los diálogos, las hojas y los avisos aparezcan y se vayan con suavidad **para**
notarlos sin que me distraigan.

| Código | Criterio |
|---|---|
| CA-ANI-04-01 | **Dado** una hoja inferior (categoría o unidad del editor, "Agregar foto", ingrediente de la receta propia o "Tema de la app"), **cuando** se abre, **entonces** el velo y la hoja aparecen en **300 ms**, ease-in-out, y se van con la misma duración al guardar, cancelar, tocar fuera, arrastrar hacia abajo o usar "atrás". |
| CA-ANI-04-02 | **Dado** un diálogo (confirmar, aviso, resultado o carga), **cuando** aparece o se cierra, **entonces** el velo y el diálogo se disuelven en **300 ms**, y el diálogo crece levemente desde el centro al aparecer. |
| CA-ANI-04-03 | **Dado** un error en línea (formulario o editor), **cuando** aparece o desaparece, **entonces** se expande y se disuelve en **150 ms**, sin mover de golpe los campos de abajo. |
| CA-ANI-04-04 | **Dado** un aviso de resultado ("Producto guardado" y "Guardado en tu teléfono" sobre la lista, "Guardado" del Perfil, barra de deshacer de Guardadas), **entonces** aparece y se va con disolvencia de **300 ms**. |
| CA-ANI-04-05 | **Dado** el aviso de voz, **cuando** se abre, cambia de estado (escuchando, procesando, error) o se cierra, **entonces** cada cambio dura **300 ms**, ease-in-out (Figma "Grabadora"). |

### HU-ANI-05: Listas, búsqueda y botones
**Como** usuario **quiero** que la lista y los botones respondan con movimiento **para** saber que la app hizo lo que
pedí.

| Código | Criterio |
|---|---|
| CA-ANI-05-01 | **Dado** la lista cargando, **cuando** llegan los productos, **entonces** el esqueleto da paso al contenido con una disolvencia de **300 ms**. |
| CA-ANI-05-02 | **Dado** la lista o los resultados de búsqueda, **cuando** un producto aparece, desaparece o cambia de sección, **entonces** las filas se reacomodan con movimiento de **300 ms**, ease-in-out, sin saltos. |
| CA-ANI-05-03 | **Dado** un grupo de chips (búsqueda del inventario, Recetas, Guardadas, Preferencias o el contexto de la IA), **cuando** elijo uno, **entonces** su color cambia en **300 ms**, ease-in-out (Figma), y los resultados de la búsqueda cambian con Smart Animate de **300 ms**. |
| CA-ANI-05-04 | **Dado** un botón con estado de carga, **cuando** pasa a cargando o vuelve, **entonces** el cambio dura **300 ms**, ease-out (Figma "BotonCargando"). |
| CA-ANI-05-05 | **Dado** el indicador de pasos de la recuperación, **cuando** cambia de paso, **entonces** su ancho y su color usan los tokens de 300 ms. |

### HU-ANI-06: Perfil, Recetas, IA y Guardadas
**Como** usuario **quiero** que los módulos nuevos se muevan igual que el resto **para** que la app se sienta una sola.

| Código | Criterio |
|---|---|
| CA-ANI-06-01 | **Dado** Perfil, Recetas, IA, cocina guiada, recetas propias o Guardadas, **cuando** entro a una pantalla o vuelvo, **entonces** se usa el empuje de 300 ms de HU-ANI-02 (Desviación §8: el Figma es instantáneo). |
| CA-ANI-06-02 | **Dado** el flujo de la IA, **cuando** pasa de contexto a "Analizando opciones" y de ahí al plan o a "Sin coincidencias", **entonces** el contenido cambia con disolvencia de **300 ms**, sin empuje (es una sola ruta). |
| CA-ANI-06-03 | **Dado** la cocina guiada, **cuando** paso al siguiente paso o vuelvo al anterior, **entonces** el paso cambia con disolvencia de **300 ms** y la barra de progreso avanza con la misma duración. |
| CA-ANI-06-04 | **Dado** las rejillas de Recetas y Guardadas, **cuando** cambia el filtro, la búsqueda o el orden, **entonces** las tarjetas se reacomodan con **300 ms**, ease-in-out, sin saltos. |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-ANI-01 | Tokens de movimiento (duraciones y curvas) en el design system, tomados del Figma y de Material 3 donde el Figma no define nada. | HU-ANI-01 |
| RF-ANI-02 | Transiciones de navegación de 300 ms declaradas una sola vez, incluidas las pestañas. | HU-ANI-02 |
| RF-ANI-03 | Campos y encabezado de autenticación que siguen el foco y la animación del teclado. | HU-ANI-03 |
| RF-ANI-04 | Diálogos, hojas, errores en línea, avisos y aviso de voz con entrada y salida animadas. | HU-ANI-04 |
| RF-ANI-05 | Lista, búsqueda, botones e indicador de pasos con cambios animados. | HU-ANI-05 |
| RF-ANI-06 | Los módulos de Perfil, Recetas + IA y Guardadas usan los mismos tokens y transiciones. | HU-ANI-06 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-ANI-01 | **Fluidez medida en release.** En el V2436, con el build release, cada transición de la spec tiene: el primer movimiento visible a ≤ 50 ms del toque, ningún hueco entre cuadros mayor de 50 ms durante la animación y como máximo 5 % de cuadros lentos según `gfxinfo`. El build debug no se usa para juzgar la fluidez. |
| RNF-ANI-02 | Funciona desde Android 7.0 (API 24). En versiones sin animación del teclado (antes de Android 11), el encabezado cambia en un solo paso, pero sin dejar de mostrar el campo enfocado. |
| RNF-ANI-03 | Accesibilidad: se respeta la escala de animación del sistema (CA-ANI-01-02) y ninguna animación es la única señal de un cambio: los avisos siguen anunciándose con `liveRegion` (RNF-04). |
| RNF-ANI-04 | Se mantienen las reglas de la spec 002: sin scroll programático, el popup siempre encima del campo y los formularios caben sin scroll desde 360×720 dp. |

**Enmienda.** Esta spec reemplaza la duración de RNF-06 de la spec 001 (600 ms) por la del Figma (300 ms).

**Pruebas.** Según las reglas de después de la entrega (PR #138), las pruebas y la medición de RNF-ANI-01 se escriben
ahora y se corren en la ronda final de los 10 puntos.

## 6. Fuera de alcance

- La animación del gesto "atrás" predictivo de Android 14+.
- Transiciones de elementos compartidos entre pantallas (por ejemplo, la foto de la lista al editor).
- Animaciones nuevas que el Figma no muestra, como celebraciones o Lottie.
- El estado "hover" del prototipo (solo aplica a escritorio).

## 7. Supuestos

- El prototipo de Figma usa "Ease in and out", "Ease out" y "Ease in" de Figma, que corresponden a las curvas cúbicas
  CSS `ease-in-out` (0.42, 0, 0.58, 1), `ease-out` (0, 0, 0.58, 1) y `ease-in` (0.42, 0, 1, 1).
- Las interacciones instantáneas del prototipo son atajos para presentar el flujo y no piden quitar animaciones.

## 8. Fidelidad con Figma y desviaciones (aprobadas por Zhair el 2026-11-19)

- **Referencia:** las reacciones del prototipo (`node.reactions`) de la página `SmartFood`, leídas con el MCP de Figma
  el 2026-11-19 (727 interacciones; §1).
- **Módulos nuevos:** Perfil, Recetas, IA y Guardadas son instantáneos en el prototipo. Usan el empuje de 300 ms y la
  disolvencia de pestañas de 100 ms para que la app se mueva igual en todos los módulos (HU-ANI-06).
- **Hojas:** el prototipo pide disolvencia de 300 ms; `ModalBottomSheet` de Material usa su propio resorte, así que se
  reemplaza por `SmartFoodBottomSheet` (plan §5).

- **Teclado:** el Figma no muestra el teclado. El encabezado sigue la animación del sistema (CA-ANI-03-02), como en
  las apps de referencia.
- **Diálogos con *back*:** algunos diálogos del prototipo usan "Ease in and out back" (rebote leve). Se aplica solo a la
  escala del diálogo (CA-ANI-04-02), nunca al velo.

## 9. Plan de entrega

Un PR (`feature/007-animaciones` → `master`) con un **commit por tarea** (`tasks.md`). La rama sale de
`feature/013-modo-oscuro` porque las dos tocan diálogos, hojas y barras; hasta fusionar el #144, el PR muestra también
sus commits.
