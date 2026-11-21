# Tareas 013: Modo oscuro legible

> Rama `feature/013-modo-oscuro`, un commit por tarea. Contratos en [`plan.md`](./plan.md). Según las reglas de
> después de la entrega, las pruebas se escriben ahora y se corren en la ronda final de los 10 puntos.

- [x] T001 La spec, el plan, las tareas y sus referencias: enmienda de CA-UI-05-04 (spec 002), `AGENTS.md`,
  `docs/pendientes.md` y `tools/ca_coverage.py`.
- [x] T002 **Roles del tema** (RF-OSC-01, RF-OSC-03, RNF-OSC-01): `SmartFoodColors.kt`, `Theme.kt` (esquema oscuro y
  `LocalSmartFoodColors`).
- [x] T003 **Design system y barras** (CA-OSC-02-01, 02-04, 03-03, 03-07, HU-OSC-04): `TopBar`, `Buttons`, `Dialogs`,
  `SystemBarEffect` y las tres hojas.
- [x] T004 **Inventario** (CA-OSC-02-03, 03-01, 03-02, 03-03, 03-06): barra, etiqueta de categoría, búsqueda, chips, aviso
  de voz y hoja de foto.
- [x] T005 **Perfil y autenticación** (CA-OSC-02-04, 02-05, 03-03, 03-05): encabezado, íconos de las filas, avatar,
  chips de Preferencias y checklist de contraseña.
- [x] T006 **Recetas, IA y cocina guiada** (CA-OSC-02-01 a 02-03, 03-01, 03-04, 03-07): encabezado, tarjeta "¿Qué
  cocinamos hoy?", buscador, chips, detalle, flujo de la IA, ayuda y temporizador.
- [x] T007 **Guardadas y recetas propias** (CA-OSC-02-01, 03-04): "Ordenar", tarjetas, editor, detalle y "¡Receta
  guardada!".
- [x] T008 **Pruebas** (HU-OSC-01, CA-OSC-02-01, 02-03, 03-01): `SmartFoodColorsContrastTest` y casos nuevos en
  `ColorModeTest`.
- [x] T009 **Rojo de error legible** (CA-OSC-01-01, CA-OSC-01-03, §9): `ErrorRedDark` y `OnErrorDark` en `Color.kt`,
  `error`/`onError` del esquema oscuro en `Theme.kt` y sus pares en `SmartFoodColorsContrastTest`. Hallazgo de la T024
  de la spec 010 (pantalla 07).

## Matriz de trazabilidad

| CA | Prueba |
|---|---|
| CA-OSC-01-01, 01-02 | U `SmartFoodColorsContrastTest` |
| CA-OSC-01-03 | U `SmartFoodColorsContrastTest` |
| CA-OSC-02-01 | I `ColorModeTest.ca_osc_02_01_*` |
| CA-OSC-02-02, 02-04, 02-05 | U `SmartFoodColorsContrastTest` (roles), M |
| CA-OSC-02-03 | I `ColorModeTest.ca_osc_02_03_*`, U `SmartFoodColorsContrastTest` |
| CA-OSC-03-01 | I `ColorModeTest.ca_osc_03_01_*` |
| CA-OSC-03-02 a 03-07 | U `SmartFoodColorsContrastTest` (roles), M |
| CA-OSC-04-01, 04-02, 04-03 | M |

U: unitaria. I: instrumentada. M: captura por adb en el V2436.

### Checklist de verificación manual (ronda final)

- [ ] En oscuro, las 16 pantallas capturadas el 2026-11-19 (Perfil y sus 4 subpantallas, hoja de tema, Inventario,
  búsqueda, editor, Recetas, detalle, IA, sin coincidencias, Guardadas, editor de receta propia, "Antes de empezar" y
  un paso de cocina) se leen sin manchas claras.
- [ ] En claro, las mismas pantallas son idénticas a las capturas de antes.
- [x] Las hojas en oscuro no dejan la barra de navegación clara: comprobado el 2026-11-19 con la hoja de categoría del
  editor (misma `OptionSheet` que "Tema de la app"); "Cancelar" naranja con texto blanco y "Guardar" verde con texto
  oscuro.
- [x] Editor de producto en oscuro: la basura de la foto es roja con ícono blanco y la cámara verde con ícono oscuro.
- [ ] Rojo de error en oscuro (T009): el aviso y el mensaje de la pantalla 07 de Guardadas se leen, y la basura de la foto
  y el botón "Eliminar" son rojo claro con contenido rojo oscuro.
- [x] Búsqueda del inventario en oscuro: buscador, chips y etiquetas oscuros y legibles.
- [ ] Barra de estado blanca al abrir en frío (CA-OSC-04-03): en el V2436 solo se cumple al navegar o al recrearse la
  actividad (limitación del teléfono, plan §2.3); falta probarlo en otro dispositivo o emulador.
- [x] Autenticación en oscuro contra los frames del Figma (`51:11` Inicio de sesión, `51:34` Crear cuenta y `51:84`
  ¿Olvidaste tu contraseña?), el 2026-11-19 con el APK anterior a esta spec: encabezado, panel, campos, título, botón y
  enlaces coinciden. Las diferencias son de texto (modo sin facturación, spec 012) o de ancho (spec 002).
- [ ] Checklist de contraseña en oscuro contra `572:1194` (tarjeta oscura con texto blanco), **a simple vista**: el
  V2436 bloquea las capturas y las grabaciones mientras el teclado está sobre un campo de contraseña.
