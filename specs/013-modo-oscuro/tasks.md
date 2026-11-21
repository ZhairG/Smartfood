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
  `error`/`onError` del esquema oscuro en `Theme.kt` y sus pares en `SmartFoodColorsContrastTest`, incluido el aviso
  teñido de rojo del editor de recetas propias. `RecipeActionButton` recibe `contentColor` para que "Sí, eliminar"
  use `onError` (con texto blanco fijo bajaba a 2,5:1 sobre el rojo nuevo; medido en el V2436). Hallazgo de la T024
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

- [x] En oscuro, las 16 pantallas capturadas el 2026-11-19 (Perfil y sus 4 subpantallas, hoja de tema, Inventario,
  búsqueda, editor, Recetas, detalle, IA, sin coincidencias, Guardadas, editor de receta propia, "Antes de empezar" y
  un paso de cocina) se leen sin manchas claras. Comprobado el 2026-11-21 en el V2436 con `integracion/ronda-final`
  (antes y después en `capturas/oscuro-antes-despues-*.png`). "Sin coincidencias" salió como "Tu plan de 3 días",
  porque la cocina relajada (spec 011) ya arma un plan con la Cocina Favorita Mexicana; las recetas y los productos
  son otros porque cambiaron los datos.
- [x] En claro, las 5 pantallas que tienen captura de antes (Perfil, hoja de tema, Inventario, Recetas y Guardadas) son
  iguales salvo por los datos (`capturas/claro-antes-despues.png`, 2026-11-21). Las demás no tienen captura de antes
  en claro; los roles claros los cubre `SmartFoodColorsContrastTest.ca_osc_01_03`.
- [x] Las hojas en oscuro no dejan la barra de navegación clara: comprobado el 2026-11-19 con la hoja de categoría del
  editor (misma `OptionSheet` que "Tema de la app"); "Cancelar" naranja con texto blanco y "Guardar" verde con texto
  oscuro.
- [x] Editor de producto en oscuro: la basura de la foto es roja con ícono blanco y la cámara verde con ícono oscuro.
- [x] Rojo de error en oscuro (T009), comprobado el 2026-11-21 en el V2436 (antes y después en
  [`capturas/`](./capturas/)): en la pantalla 07 de Guardadas, el texto del aviso pasa de 2,9:1 a 4,8:1 y el mensaje bajo
  el campo de 3,5:1 a 6,8:1; "Sí, eliminar" (pantalla 10) es rojo claro con texto rojo oscuro, 6,5:1.
- [x] La basura de la foto y "Eliminar" del editor de producto en oscuro, con el rojo nuevo: rojo claro con ícono y
  texto rojo oscuro (2026-11-21, `capturas/oscuro-antes-despues-2.png`).
- [x] Búsqueda del inventario en oscuro: buscador, chips y etiquetas oscuros y legibles.
- [ ] Barra de estado blanca al abrir en frío (CA-OSC-04-03): en el V2436 solo se cumple al navegar o al recrearse la
  actividad (limitación del teléfono, plan §2.3); falta probarlo en otro dispositivo o emulador.
- [x] Autenticación en oscuro contra los frames del Figma (`51:11` Inicio de sesión, `51:34` Crear cuenta y `51:84`
  ¿Olvidaste tu contraseña?), el 2026-11-19 con el APK anterior a esta spec: encabezado, panel, campos, título, botón y
  enlaces coinciden. Las diferencias son de texto (modo sin facturación, spec 012) o de ancho (spec 002).
- [ ] Checklist de contraseña en oscuro contra `572:1194` (tarjeta oscura con texto blanco), **a simple vista**: el
  V2436 bloquea las capturas y las grabaciones mientras el teclado está sobre un campo de contraseña.
