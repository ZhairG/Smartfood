# Tareas 002: Adaptabilidad de la UI, foco y teclado

> Derivadas de [`plan.md`](./plan.md). `[P]` = se puede hacer en paralelo con la tarea anterior.

## Fase 0: Especificación
- [x] T001 `spec.md`, `plan.md` y `tasks.md`; rama `feature/002-ui-adaptable`.

## Fase 1: Design system
- [x] T002 `FormMetrics` + `formMetricsFor` + `LocalFormMetrics` (RNF-UI-03, CA-UI-01-03..05).
- [x] T003 [P] `ClearFocusOnKeyboardDismiss` en `util/KeyboardExt.kt` (CA-UI-02-06).
- [x] T004 [P] `AbovePopupPositionProvider` y `AnchoredPopup` (CA-UI-03-01, 03, 04; RNF-UI-01).
- [x] T005 `SmartFoodTextField`: etiqueta dentro del campo, `onFocusChanged` directo y medidas (CA-UI-02-01, 02, 08; RNF-UI-04).
- [x] T006 [P] Marca, botón, OTP (`onDone`), indicador de pasos y pantalla de éxito con `FormMetrics` (CA-UI-01-02).

## Fase 2: Scaffold y pantallas
- [x] T007 `AuthScreenScaffold` desde cero: métricas, encabezado colapsable y padding del teclado (CA-UI-02-04, 05, 07; RNF-UI-02).
- [x] T008 `PasswordFieldWithChecklist` y checklist compacto (CA-UI-03-01..05).
- [x] T009 `LoginScreen` y `RegisterScreen` compactas (CA-UI-01-01).
- [x] T010 `PasswordRecoveryScreen` sin scroll programático y con el checklist en popup (CA-UI-01-01, CA-UI-03-02, 06).
- [x] T011 [P] Orientación vertical en el manifest (CA-UI-04-01).

## Fase 3: Pruebas y cierre
- [x] T012 Pruebas JVM de métricas y del posicionamiento del popup.
- [x] T013 Pruebas instrumentadas de "sin scroll" en 360×720 y 384×857.
- [x] T014 Pruebas instrumentadas de foco y popup; ajuste de las pruebas existentes.
- [x] T015 Documentación (`AGENTS.md`, `README.md`, nota en la spec 001) y matriz de trazabilidad.

## Matriz de trazabilidad

Leyenda: **U** = prueba unitaria, **I** = prueba instrumentada (verificada en un V2436 con Android 16), **M** = revisión manual.

| Criterio | Pruebas |
|---|---|
| CA-UI-01-01 | I `AdaptiveLayoutTest` (Login, Registro, Registro con error y recuperación pasos 1, 2 y 3, en 360×720 y 384×857) |
| CA-UI-01-02 | I `AdaptiveLayoutTest.ca_ui_01_02_*` (360×720 y 384×857) |
| CA-UI-01-03 | U `FormMetricsTest` |
| CA-UI-01-04 | U `FormMetricsTest`, I `AdaptiveLayoutTest` (384×857) |
| CA-UI-01-05 | U `FormMetricsTest`, I `AdaptiveLayoutTest` (360×720) |
| CA-UI-01-06 | Diseño: el `verticalScroll` del panel sigue activo como respaldo |
| CA-UI-02-01 | I `FormFocusTest.ca_ui_02_01_*` |
| CA-UI-02-02 | I `FormFocusTest.ca_ui_02_02_*` |
| CA-UI-02-03 | M (acciones "Listo" en Login, Registro y pasos 1, 2 y 3) |
| CA-UI-02-04 | M (capturas en el dispositivo: el campo enfocado queda visible con el teclado abierto) |
| CA-UI-02-05 | M (capturas en el dispositivo: el encabezado se colapsa con el teclado) |
| CA-UI-02-06 | M |
| CA-UI-02-07 | M |
| CA-UI-02-08 | I `FormFocusTest.ca_ui_02_01_*` (la etiqueta forma parte del nodo editable) |
| CA-UI-03-01 | U `AbovePopupPositionProviderTest`, I `FormFocusTest.ca_ui_03_01_*` |
| CA-UI-03-02 | I `FormFocusTest.ca_ui_03_02_*` |
| CA-UI-03-03 | Diseño: `PopupProperties(focusable = false)`; I `FormFocusTest.ca_ui_03_05_*` (el foco sigue en los campos) |
| CA-UI-03-04 | U `AbovePopupPositionProviderTest` (3 casos) |
| CA-UI-03-05 | I `FormFocusTest.ca_ui_03_05_*` |
| CA-UI-03-06 | I `FormFocusTest.ca_ui_03_02_*` |
| CA-UI-04-01 | Manifest `screenOrientation="portrait"`; M |
| CA-UI-04-02 | Diseño: ancho máximo de 480 dp en el formulario |
| RNF-UI-01..04 | Revisión de código: sin offsets fijos ni `scrollTo`/`animateScrollTo`, medidas en `FormMetrics`, foco con `onFocusChanged` |

### Hallazgos durante la verificación

**1. Íconos de la barra de estado oscuros en vivo (OriginOS 6). Causa: el sistema del fabricante. Ya existía antes de esta spec.**

En un vivo V2436 con OriginOS 6, el reloj y los íconos de la barra de estado se ven oscuros sobre el encabezado
verde del Login y, en la mayoría de los casos, también del Registro. Resultados de la investigación (2026-10-04):

| Prueba | Resultado |
|---|---|
| Flags de la ventana en Login y Registro | Idénticos: sin `APPEARANCE_LIGHT_STATUS_BARS`. La app pide íconos claros. |
| `LightBarController` de System UI (capa estándar de Android) | `isLight=false`, es decir, íconos claros. Pero `StatusBarTransitionsController.mDarkIntensity=1.0` (oscuros). |
| Versión anterior (`refactor/sdd-auth`) | Mismo resultado, así que no es una regresión. |
| Barra de navegación oscura en pantallas oscuras | Sin cambio. |
| Splash pidiendo íconos claros | La Splash los muestra **blancos**; al entrar al Login vuelven a oscuros. |
| Reaplicar la apariencia con un cambio real del flag tras 800 ms | Sin cambio. |
| Esperar 60 s o abrir y cerrar el teclado | Sin cambio. |
| Ocultar y mostrar la barra de estado desde la app (en el mismo frame y con 300 ms de pausa) | Sin cambio. |
| **Abrir y cerrar el panel de ajustes rápidos** (`cmd statusbar expand-settings` / `collapse`) | **Se corrige:** `mDarkIntensity` pasa de 1.0 a 0.0 y los íconos se ven blancos. Lo reportó el usuario y quedó verificado. |

**Conclusión:** la app y Android coinciden (íconos claros). En la capa de vivo, `StatusBarTransitionsController` queda
con `mPendingDarkIntensity=0.0` (el valor correcto) sin aplicarlo, y mantiene `mDarkIntensity=1.0` hasta que un
evento propio de System UI, como abrir el panel de ajustes rápidos, lo fuerza. Las dos capturas con íconos blancos
coinciden con momentos en que se había abierto ese panel. Ninguna API pública de la app reproduce ese evento. **Pendiente:** verificar en otro dispositivo o en un emulador para confirmar que ocurre solo en vivo.

**2. Autoenfoque al abrir el Registro. No se reproduce.**

Ocurrió una vez, justo después de ejecutar la suite instrumentada. Se descartó la hipótesis del "modo táctil": el
teléfono sigue con `mInTouchMode=true` después de las pruebas. Tampoco se reprodujo con instalación limpia ni en
5 intentos más. Nota: `connectedDebugAndroidTest` **desinstala la app** al terminar.
