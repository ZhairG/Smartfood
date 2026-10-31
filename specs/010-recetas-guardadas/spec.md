# Spec 010: Recetas guardadas

| Campo | Valor |
|---|---|
| Feature | `010-recetas-guardadas` (Módulo 5) |
| Estado | En planeación (Issues #117, #130 y #131) |
| Fecha | 2026-10-31 |
| Depende de | 006-inventario (patrón sin conexión), 008-perfil (contador), 009-recetas-ia (catálogo, tarjetas y detalle) |
| Afecta | Pestaña Guardadas, tarjetas y detalle de Recetas, contador del Perfil, `firestore.rules` |
| Entrega | Por definir (Zhair) |

> Los códigos de esta spec llevan el prefijo `SAV` (`HU-SAV-01`, `CA-SAV-01-01`, `RF-SAV-01`, `RNF-SAV-01`).

## 1. Contexto y motivación

La pestaña Guardadas está gris desde la spec 006 y las tarjetas de Recetas no tienen ♡ (desviación de la spec 009, §9).
El Perfil muestra "Recetas guardadas = 0" con un texto fijo (spec 008, §9).

El Figma de este módulo define dos cosas:
- guardar como **favoritas** las recetas del catálogo (y las propias);
- **crear, editar y eliminar recetas propias** ("Creadas por mí"), con validación, confirmación y "Deshacer".

Zhair decidió cubrir todo el Figma (2026-10-31). Esta spec reemplaza el alcance anterior del #117, que era solo
favoritas.

## 2. Actores

- **Usuario con sesión:** quiere tener a mano las recetas que le gustan y guardar las suyas para no perderlas.

## 3. Historias de usuario y criterios de aceptación

### HU-SAV-01: Marcar recetas favoritas
**Como** usuario **quiero** marcar una receta con ♡ **para** encontrarla después en Guardadas.

| Código | Criterio |
|---|---|
| CA-SAV-01-01 | **Dado** una tarjeta de receta en la pestaña Recetas o en Guardadas, **entonces** veo el ♡ en la esquina inferior derecha de la foto, como en el Figma `633:2723`: relleno si es favorita, con borde si no lo es. |
| CA-SAV-01-02 | **Dado** el detalle de una receta (Figma `557:2856`), **entonces** veo el mismo ♡ y refleja si es favorita. |
| CA-SAV-01-03 | **Dado** una receta que no es favorita, **cuando** toco su ♡, **entonces** se marca al instante y aparece en Guardadas → "Favoritas"; **cuando** lo toco otra vez, se quita de Favoritas. |
| CA-SAV-01-04 | **Dado** que no hay conexión, **cuando** toco el ♡, **entonces** el cambio se ve al instante y se sincroniza cuando vuelve la conexión (como el inventario, CA-INV-10-01). |
| CA-SAV-01-05 | **Dado** que marqué favoritas, **cuando** abro la app en otro teléfono con la misma cuenta, **entonces** veo las mismas favoritas. |

### HU-SAV-02: Ver mis recetas guardadas
**Como** usuario **quiero** una pestaña con mis favoritas y mis recetas **para** buscarlas y ordenarlas.

| Código | Criterio |
|---|---|
| CA-SAV-02-01 | **Dado** la barra inferior, **entonces** la pestaña "Guardadas" está habilitada; **cuando** la toco, se abre la lista (Figma `633:2723`) con "Guardadas" seleccionada. |
| CA-SAV-02-02 | **Dado** la lista, **entonces** veo la barra "Guardadas" con "Ordenar", el buscador "Buscar receta guardado...", los chips "Favoritas", "Creadas por mí" y "Recientes" ("Favoritas" elegido al entrar), las tarjetas en dos columnas (foto, ♡, nombre, "N porciones · M min") y el botón flotante "+". |
| CA-SAV-02-03 | **Dado** el chip "Favoritas", **entonces** se ven las recetas marcadas con ♡; **dado** "Creadas por mí", las recetas que creé; **dado** "Recientes", las favoritas y las creadas juntas, de la más reciente a la más antigua, por la fecha en que las guardé o las cambié. |
| CA-SAV-02-04 | **Dado** que escribo en el buscador, **entonces** se filtran las tarjetas del chip elegido por nombre, sin importar mayúsculas ni tildes (`SearchInventoryUseCase.normalize`); si no hay coincidencias, veo "No encontramos recetas con ese nombre". |
| CA-SAV-02-05 | **Dado** que toco "Ordenar", **entonces** se abre un menú con "Más recientes" (por defecto) y "Nombre A-Z"; la elección ordena el chip elegido y se conserva mientras la app esté abierta. *(Desviación §9.)* |
| CA-SAV-02-06 | **Dado** una tarjeta del catálogo, **cuando** la toco, **entonces** se abre su detalle (spec 009); **dado** una tarjeta creada por mí, **cuando** la toco, **entonces** se abre "Editar receta" (Figma `666:3647`). |
| CA-SAV-02-07 | **Dado** que no tengo favoritas ni recetas creadas, **entonces** no se ven el buscador ni los chips, y veo el ícono de libro, "Aún no creas recetas", "Guarda una recomendación de IA o crea tu primera receta desde cero." y el botón "Crear receta" (Figma `557:2585`). |
| CA-SAV-02-08 | **Dado** un chip sin recetas (por ejemplo, "Creadas por mí" sin recetas creadas), **entonces** veo un texto corto que lo explica en lugar de la cuadrícula. |
| CA-SAV-02-09 | **Dado** que tengo cambios que esperan sincronizarse, **entonces** debajo de las tarjetas veo la tarjeta "Sin conexión · Tus recetas guardadas siguen disponibles. Los cambios se sincronizarán después." (Figma `633:2723`), que desaparece al sincronizar. |
| CA-SAV-02-10 | **Dado** que toco el "+" o "Crear receta", **entonces** se abre "Nueva receta" (Figma `557:2693`). |

### HU-SAV-03: Crear una receta propia
**Como** usuario **quiero** escribir mis propias recetas **para** tenerlas junto a las del catálogo.

| Código | Criterio |
|---|---|
| CA-SAV-03-01 | **Dado** "Nueva receta" (Figma `557:2693`), **entonces** veo la barra con la flecha y "Guardar", y los campos "Nombre de la receta *", "Categoría *" (con flecha), la tarjeta "Ingredientes" con "+ Añadir ingrediente", "Tiempo de cocción *" y "Preparación *". Debajo, la nota "Los campos con * son obligatorios. Revisa antes de guardar." y el botón naranja "Guardar receta". |
| CA-SAV-03-02 | **Dado** "Categoría", **cuando** la toco, **entonces** se abre una hoja con la lista fija de categorías (§7) y la elegida se muestra en la fila. |
| CA-SAV-03-03 | **Dado** "+ Añadir ingrediente", **cuando** lo toco, **entonces** se abre una hoja para buscar un ingrediente del catálogo (`ingredientes.json`) y escribir la cantidad en su unidad (g, ml o und). El ingrediente se agrega como fila (ícono, nombre y chip verde con la cantidad, como "Tomate · 1 und"). Tocar una fila permite cambiar la cantidad o quitarla. *(Desviación §9.)* |
| CA-SAV-03-04 | **Dado** "Guardar receta" o "Guardar", **cuando** todos los campos son válidos, **entonces** la receta se guarda y se abre "¡Receta guardada!" (Figma `557:2551`): "“<nombre>” ya está en Creadas por mí.", con "Ver receta" y "Crear otra". |
| CA-SAV-03-05 | **Dado** los campos, **entonces** se validan así: <ul><li>el nombre es obligatorio y tiene de 2 a 60 caracteres, sin espacios al inicio ni al final;</li><li>la categoría es obligatoria;</li><li>el tiempo de cocción es un número entero de 1 a 240 minutos;</li><li>la preparación es obligatoria y tiene como máximo 2.000 caracteres;</li><li>los ingredientes son opcionales, como máximo 20 y sin repetirse, cada uno con cantidad mayor que 0.</li></ul> |
| CA-SAV-03-06 | **Dado** un campo inválido, **cuando** toco guardar, **entonces** no se guarda: arriba aparece el aviso "⚠ No pudimos guardar los cambios" con el motivo del primer campo inválido, y el campo se marca en rojo con su mensaje (como el Figma `557:2654`; por ejemplo, "Ingresa un valor entre 1 y 240 minutos."). |
| CA-SAV-03-07 | **Dado** "¡Receta guardada!", **cuando** toco "Ver receta", **entonces** se abre el detalle de mi receta (*desviación §9*); **cuando** toco "Crear otra", se abre "Nueva receta" vacía; **cuando** toco la flecha, vuelvo a Guardadas. |
| CA-SAV-03-08 | **Dado** que guardo sin conexión, **entonces** se abre "¡Receta guardada!" igual y la receta aparece en la lista marcada como pendiente; se sincroniza al volver la conexión. |
| CA-SAV-03-09 | **Dado** una receta recién creada, **entonces** también queda marcada como favorita (aparece en "Favoritas" y en "Creadas por mí", como el Figma `666:3804`); puedo quitarle el ♡. |

### HU-SAV-04: Editar una receta propia
**Como** usuario **quiero** corregir mis recetas **para** mantenerlas al día.

| Código | Criterio |
|---|---|
| CA-SAV-04-01 | **Dado** "Editar receta" (Figma `666:3647`), **entonces** veo la barra con la flecha y "Eliminar", los campos con los datos de la receta (incluida la tarjeta de ingredientes, *desviación §9*), "Guardar receta" y "Descartar cambios" (botón blanco con borde). |
| CA-SAV-04-02 | **Dado** cambios válidos, **cuando** toco "Guardar receta", **entonces** se guardan y se abre "¡Receta guardada!" con "“<nombre>” ha sido actualizada." (Figma `666:3717`). |
| CA-SAV-04-03 | **Dado** un campo inválido, **cuando** toco "Guardar receta", **entonces** pasa lo mismo que en CA-SAV-03-06 (Figma `557:2654`); al corregirlo, el aviso y el rojo desaparecen (Figma `666:3753`). |
| CA-SAV-04-04 | **Dado** "Descartar cambios", la flecha o "atrás", **entonces** vuelvo a Guardadas sin guardar y sin preguntar (como CA-INV-03-04). |
| CA-SAV-04-05 | **Dado** el detalle de mi receta, **entonces** veo el nombre, la categoría, "2 porciones · M min", los ingredientes y la preparación, el ♡ y la acción "Editar"; no tiene "Cocinar" ni precios. |

### HU-SAV-05: Eliminar una receta propia
**Como** usuario **quiero** borrar una receta que ya no uso, y deshacerlo si me equivoco.

| Código | Criterio |
|---|---|
| CA-SAV-05-01 | **Dado** "Editar receta", **cuando** toco "Eliminar", **entonces** se abre "Eliminar receta" (Figma `557:2620`): el ícono de papelera en un círculo rojo claro, "¿Eliminar “<nombre>”?", "Esta acción quitará la receta de tus creaciones. No afecta los ingredientes del inventario.", "Cancelar" (blanco con borde) y "Sí, eliminar" (rojo). |
| CA-SAV-05-02 | **Dado** la confirmación, **cuando** toco "Cancelar" o la flecha, **entonces** vuelvo a "Editar receta" sin cambios. |
| CA-SAV-05-03 | **Dado** la confirmación, **cuando** toco "Sí, eliminar", **entonces** la receta (y su ♡) se elimina y vuelvo a Guardadas con el aviso "Receta eliminada · “<nombre>” se eliminó." y "DESHACER · 8 s" (Figma `557:2585`), con la cuenta regresiva de 8 a 1. |
| CA-SAV-05-04 | **Dado** el aviso, **cuando** toco "DESHACER" antes de que termine la cuenta, **entonces** la receta vuelve con los mismos datos, su fecha de creación y su ♡, y el aviso se cierra. |
| CA-SAV-05-05 | **Dado** el aviso, **cuando** pasan los 8 s, **entonces** se cierra solo y la eliminación queda definitiva. |
| CA-SAV-05-06 | **Dado** que era mi única receta guardada, **entonces** detrás del aviso se ve el estado vacío (CA-SAV-02-07), como en el Figma `557:2585`. |

### HU-SAV-06: Contador del Perfil
**Como** usuario **quiero** ver cuántas recetas tengo guardadas en mi Perfil.

| Código | Criterio |
|---|---|
| CA-SAV-06-01 | **Dado** el Perfil, **entonces** la tarjeta "Recetas guardadas" muestra cuántas recetas distintas tengo entre favoritas y creadas por mí, sin contar dos veces una receta creada que además es favorita. |
| CA-SAV-06-02 | **Dado** que marco, quito, creo o elimino una receta, **cuando** vuelvo al Perfil, **entonces** el número ya está actualizado (también sin conexión). |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-SAV-01 | Favoritas en `users/{uid}/favorites/{recipeId}`, con `source` (`catalog` u `own`) y `savedAt`. | HU-SAV-01 |
| RF-SAV-02 | Recetas propias en `users/{uid}/recipes/{recipeId}`, con `name`, `nameNormalized`, `category`, `ingredients`, `cookMinutes`, `preparation`, `createdAt` y `updatedAt`. | HU-SAV-03 a 05 |
| RF-SAV-03 | Lista Guardadas con chips, búsqueda, orden, estado vacío y aviso de cambios pendientes. | HU-SAV-02 |
| RF-SAV-04 | Eliminar con confirmación y "Deshacer" durante 8 s. | HU-SAV-05 |
| RF-SAV-05 | El contador "Recetas guardadas" del Perfil sale de los datos reales. | HU-SAV-06 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-SAV-01 | Seguridad: las reglas de Firestore solo dejan al dueño leer y escribir sus favoritas y sus recetas, con claves conocidas, tipos y límites de §3 (RNF-02). Se prueban en `rules-test/`. |
| RNF-SAV-02 | Sin conexión: las escrituras van a la cola de Firestore sin transacciones y esperan al servidor como mucho 3 s (`WriteOutcome`), como el inventario (spec 006, §13). |
| RNF-SAV-03 | Los textos van en `res/values/strings_saved.xml` (RNF-03). |
| RNF-SAV-04 | Accesibilidad: el ♡ se anuncia como "Agregar a favoritas" o "Quitar de favoritas" con su estado; el aviso de eliminación usa `liveRegion` y "Deshacer" tiene acción para TalkBack (RNF-04). |
| RNF-SAV-05 | Fidelidad con Figma (constitución §1): íconos, colores, tipografía y medidas de los frames de §8, con los tokens existentes (sin colores nuevos). |
| RNF-SAV-06 | Funciona desde Android 7.0 (API 24). R8 no cambia: Firestore se escribe con `Map`. |

## 6. Fuera de alcance

- Cocinar una receta propia con la cocina guiada, o que el agente de IA la use en un plan.
- Fotos en las recetas propias.
- Porciones editables en las recetas propias.
- Compartir recetas con otros usuarios.

## 7. Supuestos

- **Categorías (lista fija):** Arepas, Arroces, Carnes, Pollo, Pescados y mariscos, Pastas, Sopas, Ensaladas, Desayunos,
  Postres y Otras.
- **Porciones:** las recetas propias son de **2 porciones**, porque el formulario del Figma no tiene ese campo y la
  tarjeta muestra "2 porciones".
- **Ingredientes:** son opcionales, porque el Figma no les pone `*`.
- **Fechas de "Recientes" y "Más recientes":**
  - una favorita usa `savedAt`;
  - una receta creada usa `updatedAt`;
  - una receta que es las dos cosas usa la fecha más nueva.
- **Deshacer:** la receta se borra al tocar "Sí, eliminar" y "Deshacer" la vuelve a escribir con el mismo id. Así
  funciona igual sin conexión y no depende de que la app siga abierta los 8 s.
- **Favorita huérfana:** si una favorita apunta a una receta que ya no existe (un id que salió del catálogo), no se
  muestra ni se cuenta.

## 8. Fidelidad con Figma

Capturas en [`disenos-figma/Modulo 5 - Recetas guardadas`](../../disenos-figma/README.md).

| Pantalla | Frame | Criterios |
|---|---|---|
| Guardadas (Favoritas, con aviso "Sin conexión") | `633:2723` | HU-SAV-02, CA-SAV-01-01 |
| Guardadas después de crear | `666:3804` | CA-SAV-03-09 |
| Guardadas después de editar | `666:3892` | CA-SAV-04-02 |
| Nueva receta | `557:2693` | HU-SAV-03 |
| ¡Receta guardada! | `557:2551` | CA-SAV-03-04, 03-07 |
| Editar receta | `666:3647` | CA-SAV-04-01 |
| Editar receta con error | `557:2654` | CA-SAV-03-06, 04-03 |
| Editar receta corregida | `666:3753` | CA-SAV-04-03 |
| ¡Receta guardada! (editada) | `666:3717` | CA-SAV-04-02 |
| Eliminar receta | `557:2620` | CA-SAV-05-01, 05-02 |
| Receta eliminada + vacío | `557:2585` | CA-SAV-05-03 a 05-06, CA-SAV-02-07 |

**Interacciones del prototipo** (leídas con `node.reactions` el 2026-10-31): todas son `ON_CLICK → NAVIGATE`
**instantáneas**, sin animación. En la app las pantallas internas usan la transición de empuje de 600 ms de
`NavTransitions` (spec 007), como el resto de los módulos, y las pestañas cambian sin transición.

Flujo:
- la tarjeta abre Editar;
- "+" abre Nueva receta;
- Guardar va a ¡Receta guardada!;
- "Eliminar" abre la confirmación;
- "Sí, eliminar" vuelve a Guardadas con el aviso;
- "Deshacer" vuelve a la lista.

## 9. Desviaciones (aprobadas por Zhair el 2026-10-31)

| Desviación | Motivo |
|---|---|
| "Ver receta" abre el detalle de la receta propia (solo lectura, sin "Cocinar"; CA-SAV-04-05). En el prototipo vuelve a la lista. | Que el botón haga lo que dice. |
| "Ordenar" abre un menú con "Más recientes" y "Nombre A-Z". | El Figma no define el menú. |
| Una receta propia no tiene foto: su tarjeta muestra el ícono de libro del Figma sobre `GreenSoft`. En el Figma `666:3804` la receta creada tiene foto. | Fotos fuera de alcance (§6). |
| La hoja "Añadir ingrediente" (buscar en el catálogo y escribir la cantidad) no está en el Figma; usa el estilo de la hoja de categoría del inventario (`OptionSheet`). | El Figma no define cómo se agrega un ingrediente. |
| "Editar receta" muestra la tarjeta de ingredientes. En el Figma `666:3647` no aparece. | Si no, no se podrían corregir los ingredientes. |
| El aviso de error "No pudimos guardar los cambios" también aparece en "Nueva receta". | Mismo formulario y mismas reglas. |
| Las tarjetas de Recetas y el detalle muestran el ♡. Esto anula la desviación de la spec 009 §9 que lo quitaba. | El módulo de favoritas ya existe. |

## 10. Plan de entrega

Un PR para todo el módulo (`feature/010-recetas-guardadas` → `master`), con **3 Issues verticales** y un **commit por
tarea** (`Tnnn`). Cada Issue tiene su domain, data, estructura, UI y pruebas.

Los Issues se trabajan **en orden** sobre la misma rama, porque comparten `Routes.kt`, `RecipesNavGraph.kt`,
`strings_saved.xml` y las reglas. Cada persona hace `git pull` antes de empezar y antes de cada push (AGENTS.md §1).

| Issue | Contenido | Tareas | Dificultad |
|---|---|---|---|
| 1. Favoritas y lista Guardadas (#117) | Repositorio y reglas de favoritas, ♡ en tarjetas y detalle, pestaña Guardadas, lista con chips, búsqueda, orden, vacío y "Sin conexión" | T002-T008 | intermedia |
| 2. Crear y editar receta propia (#130) | Modelo, repositorio y reglas de recetas, validación, editor (crear, editar, error, descartar), hojas de categoría e ingrediente, "¡Receta guardada!" y detalle propio | T009-T016 | avanzada |
| 3. Eliminar con deshacer y contador (#131) | Confirmación, aviso con cuenta regresiva y "Deshacer", contador del Perfil | T017-T021 | intermedia |

Cierre (T022-T025) en el mismo PR: suite instrumentada completa, `rules-test`, despliegue de reglas, checklist manual
y matriz.
