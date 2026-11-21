# Módulo 5: Figma vs app (spec 010, T024)

Comparación de cada pantalla de Guardadas con su frame del Figma, en el mismo estado, para cerrar T024 de la
[spec 010](../../../specs/010-recetas-guardadas/tasks.md). Se llena en la ronda final de los pendientes, sobre `master`
con todos los PR fusionados (punto 9 de [`docs/pendientes.md`](../../../docs/pendientes.md)).

- **Figma:** las capturas de la carpeta de arriba (2026-10-31). Si el frame cambió, se vuelve a capturar con el MCP de
  Figma (`get_screenshot`).
- **App:** capturas reales del V2436 por adb (`adb exec-out screencap -p > archivo.png`), en claro y oscuro. Se nombran
  `NN-app-claro.png` y `NN-app-oscuro.png`, con el número de la tabla.
- **Resultado:** «Igual», «Desviación aprobada (spec 010 §9)» o «Hallazgo», con una nota. Un hallazgo no se corrige
  sin una nueva ronda de modo plan.

**Base de la comparación (2026-11-20):** el APK debug de `integracion/ronda-final` (commit `76ddd2b`, con los PR #139 a #148 y la T026) en el V2436 (Android 16, 720×1608). `master` todavía no tenía los PR fusionados. Las capturas se tomaron por adb y los textos se escribieron con `adb shell input`, que no escribe tildes («Sofreir»).

| # | Pantalla | Nodo | Figma | App clara | App oscura | Resultado |
|---|---|---|---|---|---|---|
| 01 | Guardadas: Favoritas, con "Sin conexión" | 633-2723 | `01 Guardadas - favoritas (633-2723).png` | `01-app-claro.png` | `01-app-oscuro.png` | Igual. Con la red de SmartFood bloqueada y un ♡ quitado (T026). La receta del catálogo es «Pasta con tomate y albahaca», porque el catálogo no tiene «Pasta verde rápida». En oscuro se lee bien. |
| 02 | Guardadas después de crear | 666-3804 | `02 Guardadas - tras crear (666-3804).png` | `02-app-claro.png` | `02-app-oscuro.png` | Desviación aprobada (spec 010 §9: la receta propia muestra el libro sobre verde suave, sin foto). En claro había red y no aparece «Sin conexión»; en oscuro, sin red, sí aparece, como en el Figma. En oscuro, la píldora del ♡ de la receta propia se confunde con la tarjeta, aunque el ♡ se ve. |
| 03 | Guardadas después de editar | 666-3892 | `03 Guardadas - tras editar (666-3892).png` | `03-app-claro.png` | `03-app-oscuro.png` | Desviación aprobada (la misma de la 02). La receta editada sube al primer lugar (CA-SAV-02-03). En oscuro se lee bien. |
| 04 | Nueva receta | 557-2693 | `04 Crear receta (557-2693).png` | `04-app-claro.png` | `04-app-oscuro.png` | Igual. Tomate en «200 g»: el catálogo lo mide en gramos, en lugar de «1 und». **Hallazgo menor:** el ícono de la etiqueta de Categoría está derecho y en el Figma va inclinado. En oscuro se lee bien. |
| 05 | "¡Receta guardada!" | 557-2551 | `05 Receta guardada (557-2551).png` | `05-app-claro.png` | `05-app-oscuro.png` | Igual. **Hallazgo menor:** el círculo del check no tiene la sombra del Figma. Sin conexión, el éxito aparece después de la espera de 3 s. En oscuro se lee bien. |
| 06 | Editar receta | 666-3647 | `06 Editar receta (666-3647).png` | `06-app-claro.png` | `06-app-oscuro.png` | Desviación aprobada (§9: la tarjeta de ingredientes en «Editar receta»). **Hallazgo menor:** el campo vacío y enfocado no muestra la ayuda «0 min» del Figma. Mismo ícono de etiqueta de la 04. En oscuro se lee bien. |
| 07 | Editar receta con error (0 min) | 557-2654 | `07 Editar receta - con error (557-2654).png` | `07-app-claro.png` | `07-app-oscuro.png` | Desviación aprobada (§9: el rojo del rol `error`). **Hallazgo menor:** el ⚠ del aviso es negro y en el Figma es amarillo. **Hallazgo (oscuro, accesibilidad):** el texto rojo del aviso y el mensaje bajo el campo tienen poco contraste sobre el fondo oscuro (se estima entre 2,6:1 y 3,4:1, frente a 4,5:1). |
| 08 | Editar receta corregida | 666-3753 | `08 Editar receta 2 (666-3753).png` | `08-app-claro.png` | `08-app-oscuro.png` | Desviación aprobada (la de la 06). El aviso y el rojo se quitan al corregir el campo. En oscuro se lee bien. |
| 09 | "¡Receta guardada!" (actualizada) | 666-3717 | `09 Receta guardada editada (666-3717).png` | `09-app-claro.png` | `09-app-oscuro.png` | Igual. Mismo hallazgo menor de la sombra del check (05). En oscuro se lee bien. |
| 10 | Eliminar receta (confirmación) | 557-2620 | `10 Confirmar eliminacion (557-2620).png` | `10-app-claro.png` | `10-app-oscuro.png` | Desviación aprobada (§9: el rojo del rol `error`). Mismo hallazgo menor de la sombra del círculo. En oscuro, la papelera roja sobre el círculo rojo oscuro tiene un contraste justo, pero se distingue. |
| 11 | Receta eliminada (deshacer · 8 s) y vacío | 557-2585 | `11 Receta eliminada (557-2585).png` | `11-app-claro.png` | `11-app-oscuro.png` | La tarjeta «Receta eliminada · DESHACER» es igual (en la captura, a 8 s y 7 s). Estado distinto: el Figma muestra el vacío «Aún no creas recetas» porque no quedan recetas propias, y en el teléfono quedan otras recetas de Zhair, que no se borraron. Ese vacío lo cubre `SavedRecipesScreenTest` (CA-SAV-02-07). En oscuro se lee bien. |

## Checklist manual (lo confirma Zhair en el teléfono)

Los pasos se hicieron por adb a pedido de Zhair, que los fue siguiendo en el teléfono.

| Paso de `tasks.md` | Confirmado |
|---|---|
| 1. ♡ en Recetas, detalle y Guardadas; la misma favorita en otro teléfono | Confirmado (2026-11-20) en Recetas, en el detalle («Crema de ahuyama») y en Guardadas. **No se verificó en otro teléfono:** Zhair no tenía uno a mano. |
| 2. Chips, búsqueda con y sin tildes, "Ordenar" por fecha y por nombre | Confirmado (2026-11-20). «pure», sin tilde, y «AHUYAMA» encuentran «Puré de ahuyama». «Nombre A-Z» y «Más recientes» cambian el orden. |
| 3. Crear con dos ingredientes; "¡Receta guardada!", "Ver receta" y "Crear otra" | Confirmado (2026-11-20). Tomate y cebolla; «Ver receta» abre el detalle y «Crear otra» abre el formulario vacío. |
| 4. Editar con 0 min (aviso y rojo), corregir a 30 min; "Descartar cambios" no guarda | Confirmado (2026-11-20). Pantallas 07 y 08. Con 45 min y «Descartar cambios», la receta siguió en 20 min. |
| 5. Eliminar y deshacer antes de 8 s; eliminar otra y esperar los 8 s | Confirmado (2026-11-20). Sin red, la tarjeta aparece después de la espera de 3 s. |
| 6. Modo avión: ♡ y receta nueva; tarjeta "Sin conexión" que desaparece con la red | Confirmado (2026-11-20), con la red de SmartFood bloqueada en lugar del modo avión. Se probaron el ♡, una receta nueva, editar y eliminar; con la red, la tarjeta desaparece (incluye la T026). |
| 7. El Perfil muestra el número correcto después de cada paso | Confirmado (2026-11-20). «Recetas guardadas» cuenta recetas distintas: 4 y luego 2. El ♡ de una receta propia no la cuenta dos veces. |
