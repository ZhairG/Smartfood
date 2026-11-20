# Spec 013: Modo oscuro legible

| Campo | Valor |
|---|---|
| Feature | `013-modo-oscuro` |
| Estado | Implementada, pruebas en la ronda final |
| Fecha | 2026-11-19 |
| Depende de | 002-adaptabilidad-ui (HU-UI-05, `ThemeMode`), 008-perfil (HU-PER-06, "Tema de la app") |
| Afecta | `core/designsystem/theme`, componentes `SmartFood*`, pantallas de todos los módulos |
| Entrega | Un PR desde `feature/013-modo-oscuro` |

> Los códigos de esta spec llevan el prefijo `OSC` (`HU-OSC-01`, `CA-OSC-01-01`, `RF-OSC-01`, `RNF-OSC-01`).

## 1. Contexto y motivación

Desde la spec 008 (HU-PER-06) la persona puede elegir "Oscuro" en "Tema de la app". El esquema oscuro de la spec 002 se
usó tal cual y nadie lo revisó pantalla por pantalla. Es el punto 6 de [`docs/pendientes.md`](../../docs/pendientes.md).

El 2026-11-19 se capturaron con adb 16 pantallas en el teléfono V2436, en claro y en oscuro. Se encontró lo siguiente:

- **`primary` brillante con texto blanco.** `DarkColorScheme` usa `primary = GreenMain` (#4ADE80) con `onPrimary = White`,
  un contraste de 1,6:1. Fallan los encabezados, la tarjeta "¿Qué cocinamos hoy?", los chips seleccionados, los botones
  flotantes, la píldora de precios y el "Guardar" de las barras.
- **Contenedores claros en oscuro.** `primaryContainer = GreenLight` deja claros los íconos del Perfil, las etiquetas
  de la búsqueda y el círculo de los diálogos.
- **Tokens claros fijos.** Unos 25 archivos usan `White`, `SurfaceLight`, `GreenSoft`, `YellowSoft` o `GreyDivider`. Por
  eso los buscadores siguen blancos y los chips sin seleccionar de la búsqueda del inventario quedan blancos con texto
  blanco.
- **Checklist de contraseña.** La tarjeta es blanca y el texto usa `onSurface`, así que en oscuro no se lee.
- **Barras del sistema.** La barra de navegación siempre pide íconos oscuros, y con una hoja abierta queda clara.

Figma solo tiene 6 frames oscuros, todos de autenticación: `742:1933`, `744:2075`, `744:2197`, `744:2330`, `750:2955` y
`744:2549`. Muestran el panel del formulario y ninguno tiene encabezado. Lo demás lo decidió Zhair (§9).

## 2. Actores

- **Usuario con el tema oscuro:** eligió "Oscuro", o "Según el teléfono" con el teléfono en oscuro.

## 3. Historias de usuario y criterios de aceptación

### HU-OSC-01: Contraste suficiente
**Como** usuario del tema oscuro **quiero** leer todos los textos e íconos **para** usar la app sin esfuerzo.

| Código | Criterio |
|---|---|
| CA-OSC-01-01 | **Dado** el tema oscuro, **entonces** cada texto sobre su relleno tiene un contraste de al menos 4,5:1 (WCAG AA) en los pares de roles del tema. |
| CA-OSC-01-02 | **Dado** el tema oscuro, **entonces** los íconos y bordes que dan información tienen un contraste de al menos 3:1. |
| CA-OSC-01-03 | **Dado** el tema claro, **entonces** todos los roles valen lo mismo que antes de esta spec: el modo claro no cambia ni un píxel. |

### HU-OSC-02: Encabezados y rellenos verdes
**Como** usuario del tema oscuro **quiero** encabezados y botones verdes legibles **para** reconocer la marca sin
deslumbrarme.

| Código | Criterio |
|---|---|
| CA-OSC-02-01 | **Dado** el tema oscuro, **entonces** los encabezados de las pestañas y de las pantallas internas son `GreenDark` (#14532D) con texto e íconos blancos, como en claro. |
| CA-OSC-02-02 | **Dado** el tema oscuro, **entonces** la tarjeta "¿Qué cocinamos hoy?" y la píldora de precios del detalle de receta usan el mismo verde del encabezado con texto blanco. |
| CA-OSC-02-03 | **Dado** el tema oscuro, **entonces** los botones flotantes, los chips seleccionados y los botones de micrófono y cámara son `GreenMain` (#4ADE80) con el texto o ícono en `GreenDark2` (#10271C). |
| CA-OSC-02-04 | **Dado** el tema oscuro, **entonces** los botones y chips marcados en `GreenCheck` (Guardar de las hojas y los diálogos, chips de Preferencias) siguen la regla de CA-OSC-02-03. |
| CA-OSC-02-05 | **Dado** el Perfil en oscuro, **entonces** el círculo de las iniciales, su borde y el correo se ven sobre el encabezado como en claro. |

### HU-OSC-03: Superficies que siguen el tema
**Como** usuario del tema oscuro **quiero** que buscadores, chips, etiquetas y tarjetas sean oscuros **para** no ver
manchas claras.

| Código | Criterio |
|---|---|
| CA-OSC-03-01 | **Dado** el tema oscuro, **entonces** los buscadores de Recetas, Guardadas y la búsqueda del inventario usan `InputDark`. |
| CA-OSC-03-02 | **Dado** la búsqueda del inventario en oscuro, **entonces** los chips sin seleccionar son oscuros con texto blanco y borde `BorderDark`. |
| CA-OSC-03-03 | **Dado** el tema oscuro, **entonces** los cuadros de los íconos del Perfil, las etiquetas de categoría y el círculo de los diálogos usan `GreenDark` con el ícono o texto en `GreenMain`. |
| CA-OSC-03-04 | **Dado** el tema oscuro, **entonces** los fondos `GreenSoft` y `YellowSoft` (fotos vacías, tarjetas de la IA, temporizador y "¡Receta guardada!") pasan a `InputDark`. |
| CA-OSC-03-05 | **Dado** el checklist de contraseña en oscuro, **entonces** la tarjeta es `InputDark` con texto blanco. |
| CA-OSC-03-06 | **Dado** el aviso de voz en oscuro, **entonces** el velo es negro al 35 % y no blanco. |
| CA-OSC-03-07 | **Dado** el tema oscuro, **entonces** los bordes y divisores grises usan `BorderDark`. |

### HU-OSC-04: Barras del sistema
**Como** usuario del tema oscuro **quiero** que las barras del sistema combinen con la app **para** no ver franjas
claras.

| Código | Criterio |
|---|---|
| CA-OSC-04-01 | **Dado** el tema oscuro, **entonces** la barra de navegación del sistema tiene íconos claros sobre la app. |
| CA-OSC-04-02 | **Dado** una hoja inferior abierta en oscuro ("Tema de la app", categoría, unidad, foto, ingrediente), **entonces** la barra de navegación no queda clara. |
| CA-OSC-04-03 | **Dado** que abro la app en frío con la sesión iniciada, en claro o en oscuro, **entonces** la barra de estado tiene íconos blancos sobre el encabezado verde de Inventario, Recetas, Guardadas y las pantallas internas. *Agregado el 2026-11-19 (aprobado por Zhair): antes de esta spec, esas pantallas heredaban los íconos oscuros de la Splash. En el V2436 (vivo) se cumple cuando la actividad se recrea, pero no tras un arranque en frío, por una limitación del teléfono que también tiene `master` (plan §2.3).* |

## 4. Requisitos funcionales

| Código | Requisito |
|---|---|
| RF-OSC-01 | `core/designsystem/theme/SmartFoodColors.kt` define los roles propios de la app (encabezado, relleno marcado, superficie suave, divisor, verde suave, resaltado, etiqueta, ícono verde y velo) para claro y oscuro. `SmartfoodTheme` los provee con `LocalSmartFoodColors`. |
| RF-OSC-02 | Las pantallas leen esos roles o los de Material. No usan tokens fijos de un solo modo, salvo los que el Figma pide iguales en los dos (naranja, rojo de error, `GreenCheck` en textos e íconos sobre fondo). |
| RF-OSC-03 | En `DarkColorScheme` cambian tres roles: `onPrimary = GreenDark2`, `primaryContainer = GreenDark` y `onPrimaryContainer = GreenMain`. Los tokens de `Color.kt` no cambian. |
| RF-OSC-04 | El modo sigue decidiéndose solo con `ThemeMode` (CA-UI-05-03). Las pantallas no consultan `LocalIsDarkTheme` para elegir colores; lo hacen los roles. La excepción es `ActionSuccessScreen`, que ya copia sus frames claro y oscuro del Figma (`GreenLight` y `GreenDark2`). |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-OSC-01 | El modo claro queda idéntico: cada rol claro es el token que la pantalla usaba antes. |
| RNF-OSC-02 | Funciona desde Android 7.0 (API 24). No hay reflexión ni cambios de R8. |
| RNF-OSC-03 | La Splash sigue en `GreenLight` en los dos modos para empalmar con la pantalla de arranque del sistema (CA-UI-05-02). |

## 6. Fuera de alcance

- Cambiar el modo por defecto: sigue siendo claro (CA-UI-05-01).
- Rediseñar las pantallas o cambiar colores del modo claro.
- Íconos de color fijo dentro de imágenes (`ic_figma_*` con color propio) y las fotos de las recetas.
- Las animaciones del cambio de tema (spec 007).

## 7. Supuestos

- `GreenCheck` sobre los fondos oscuros llega a unos 5:1, así que sus textos e íconos (pestaña activa, "PASO 1", números
  del Perfil) se quedan igual.
- Los botones naranja con texto blanco, los grises deshabilitados y el botón "Cancelar" gris con texto negro de los
  avisos son iguales en los dos modos, como en los frames oscuros de autenticación.

## 8. Fidelidad con Figma

- **Autenticación:** los 6 frames oscuros (§1) son la referencia: panel `SurfaceDark`, campos `InputDark`, borde
  `BorderDark` y botón naranja. Esta spec no los cambia; solo corrige el checklist, que esos frames no muestran abierto.
- **Demás módulos:** no tienen frame oscuro. Se aplican las decisiones de §9.
- **Interacciones:** el cambio de tema no tiene transición en el prototipo; se aplica al instante.

## 9. Desviaciones (aprobadas por Zhair el 2026-11-19)

| Desviación | Motivo |
|---|---|
| Inventario, Perfil, Recetas, IA, cocina guiada y Guardadas tienen modo oscuro sin frame en el Figma. | El Figma solo diseña la autenticación en oscuro; la fila "Tema de la app" ya existe (spec 008). |
| El encabezado en oscuro es `GreenDark` (#14532D) con texto blanco, igual que en claro. | Con `GreenMain` y texto blanco el contraste es de 1,6:1. |
| Los rellenos de acción (botón flotante, chips seleccionados, micrófono, cámara, "Guardar" verde) son `GreenMain` con contenido `GreenDark2`. | Con `GreenDark` se confundían con el fondo (1,9:1); así se distinguen del encabezado y llegan a ~9:1. |
| Las superficies suaves (`SurfaceLight`, `GreenSoft`, `YellowSoft`) pasan a `InputDark`. | Son tarjetas claras sobre fondo claro; en oscuro deben ser tarjetas oscuras. |
| El velo del aviso de voz es negro al 35 % en oscuro. | El velo blanco del Figma `476:4507` aclara toda la pantalla oscura. |

## 10. Plan de entrega

Un PR (`feature/013-modo-oscuro` → `master`) con un **commit por tarea**: T001 la spec, T002 los roles, T003 el design
system, T004 a T007 las pantallas por módulo y T008 las pruebas. El detalle está en [`tasks.md`](./tasks.md).
