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

| # | Pantalla | Nodo | Figma | App clara | App oscura | Resultado |
|---|---|---|---|---|---|---|
| 01 | Guardadas: Favoritas, con "Sin conexión" | 633-2723 | `01 Guardadas - favoritas (633-2723).png` | pendiente | pendiente | pendiente |
| 02 | Guardadas después de crear | 666-3804 | `02 Guardadas - tras crear (666-3804).png` | pendiente | pendiente | pendiente |
| 03 | Guardadas después de editar | 666-3892 | `03 Guardadas - tras editar (666-3892).png` | pendiente | pendiente | pendiente |
| 04 | Nueva receta | 557-2693 | `04 Crear receta (557-2693).png` | pendiente | pendiente | pendiente |
| 05 | "¡Receta guardada!" | 557-2551 | `05 Receta guardada (557-2551).png` | pendiente | pendiente | pendiente |
| 06 | Editar receta | 666-3647 | `06 Editar receta (666-3647).png` | pendiente | pendiente | pendiente |
| 07 | Editar receta con error (0 min) | 557-2654 | `07 Editar receta - con error (557-2654).png` | pendiente | pendiente | pendiente |
| 08 | Editar receta corregida | 666-3753 | `08 Editar receta 2 (666-3753).png` | pendiente | pendiente | pendiente |
| 09 | "¡Receta guardada!" (actualizada) | 666-3717 | `09 Receta guardada editada (666-3717).png` | pendiente | pendiente | pendiente |
| 10 | Eliminar receta (confirmación) | 557-2620 | `10 Confirmar eliminacion (557-2620).png` | pendiente | pendiente | pendiente |
| 11 | Receta eliminada (deshacer · 8 s) y vacío | 557-2585 | `11 Receta eliminada (557-2585).png` | pendiente | pendiente | pendiente |

## Checklist manual (lo confirma Zhair en el teléfono)

| Paso de `tasks.md` | Confirmado |
|---|---|
| 1. ♡ en Recetas, detalle y Guardadas; la misma favorita en otro teléfono | pendiente |
| 2. Chips, búsqueda con y sin tildes, "Ordenar" por fecha y por nombre | pendiente |
| 3. Crear con dos ingredientes; "¡Receta guardada!", "Ver receta" y "Crear otra" | pendiente |
| 4. Editar con 0 min (aviso y rojo), corregir a 30 min; "Descartar cambios" no guarda | pendiente |
| 5. Eliminar y deshacer antes de 8 s; eliminar otra y esperar los 8 s | pendiente |
| 6. Modo avión: ♡ y receta nueva; tarjeta "Sin conexión" que desaparece con la red | pendiente |
| 7. El Perfil muestra el número correcto después de cada paso | pendiente |
