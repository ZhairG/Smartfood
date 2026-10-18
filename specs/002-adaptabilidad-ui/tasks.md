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

## Fase 4: Modo de color (HU-UI-05)
- [x] T016 `ThemeMode` + `LocalIsDarkTheme`; `SmartfoodTheme(themeMode)` con claro por defecto (CA-UI-05-03, RNF-UI-05).
- [x] T017 [P] `ActionSuccessScreen` lee `LocalIsDarkTheme`; `HomeScreen` y `ActionSuccessScreen` ajustan los íconos de las barras (CA-UI-05-01).
- [x] T018 [P] Tema XML claro, `values-v31` con `windowSplashScreen*` y `splash_icon` (CA-UI-05-02).
- [x] T019 [P] `SplashScreen` y `MainActivity` usan tokens y estilos claros (CA-UI-05-04).
- [x] T020 Pruebas `ThemeModeTest` (JVM) y `ColorModeTest` (instrumentada); documentación.

## Fase 5: Ícono oficial (HU-UI-06)
- [x] T021 `tools/generate_launcher_icon.py`: capas frontal y monocromática, íconos para Android 7.x y Play Store, con verificación de la zona segura (CA-UI-06-02, 04).
- [x] T022 `mipmap-anydpi-v26` con fondo `GreenLight` y capas generadas; se eliminan las plantillas del robot (CA-UI-06-01, 03).
- [x] T023 `LauncherIconTest` instrumentada y documentación.

## Fase 6: Fidelidad con Figma (HU-UI-07)
- [x] T024 Poppins en `res/font`, licencia y `Type.kt` (CA-UI-07-01).
- [x] T025 Roles de color del esquema claro y token `GreenCheck` (CA-UI-07-02).
- [x] T026 [P] Botón, enlaces, casilla, términos, código y diálogo según Figma (CA-UI-07-05, 06, 07).
- [x] T027 [P] Checklist con estilo de Figma (CA-UI-07-04).
- [x] T028 Espaciados de Login, Registro, Recuperación y Éxito por perfil; `AdaptiveLayoutTest` con 412×917 (CA-UI-07-03).
- [x] T028b Perfil por pantalla (bajar si no cabe), separaciones flexibles (`FlexibleGap`, `fillFormViewport`) y `FormProfileFitTest` (CA-UI-07-03).
- [x] T029b Desborde pasajero entre pasos de la recuperación (perfil compacto por error) y checklist siempre encima (CA-UI-07-03, CA-UI-03-04).
- [x] T029 Comparación visual con Figma (capturas a 412×917) y documentación.
- [x] T030 Asentamiento inicial del perfil sin espera y oculto, y estado recordado por `maxHeight` (CA-UI-07-08).

## Matriz de trazabilidad

Leyenda: **U** = prueba unitaria, **I** = prueba instrumentada (verificada en un V2436 con Android 16), **M** = revisión manual.

> Desde el #105 varias pruebas recorren un flujo o una tabla de casos y citan **todos** sus CA en el nombre. La matriz nombra la clase; `python tools/ca_coverage.py --check tools/ca_coverage_baseline.txt` comprueba que ningún CA se quede sin prueba.

| Criterio | Pruebas |
|---|---|
| CA-UI-01-01 | I `AdaptiveLayoutTest` (Login, Registro, Registro con error y recuperación pasos 1, 2 y 3, en 360×720, 384×857 y 412×917) |
| CA-UI-01-02 | I `AdaptiveLayoutTest` (360×720, 384×857 y 412×917) |
| CA-UI-01-03 | U `FormMetricsTest` |
| CA-UI-01-04 | U `FormMetricsTest`, I `AdaptiveLayoutTest` (384×857) |
| CA-UI-01-05 | U `FormMetricsTest`, I `AdaptiveLayoutTest` (360×720) |
| CA-UI-01-06 | Diseño: el `verticalScroll` del panel sigue activo como respaldo |
| CA-UI-02-01 | I `FormFocusTest.ca_ui_02_01_*`, I `SmartFoodFieldsTest` (selector) |
| CA-UI-02-02 | I `FormFocusTest.ca_ui_02_02_*` |
| CA-UI-02-03 | M (acciones "Listo" en Login, Registro y pasos 1, 2 y 3) |
| CA-UI-02-04 | M (capturas en el dispositivo: el campo enfocado queda visible con el teclado abierto) |
| CA-UI-02-05 | M (capturas en el dispositivo: el encabezado se colapsa con el teclado) |
| CA-UI-02-06 | M |
| CA-UI-02-07 | M |
| CA-UI-02-08 | I `FormFocusTest.ca_ui_02_01_*` (la etiqueta forma parte del nodo editable), I `SmartFoodFieldsTest` |
| CA-UI-03-01 | U `AbovePopupPositionProviderTest`, I `FormFocusTest.ca_ui_03_01_*` |
| CA-UI-03-02 | I `FormFocusTest.ca_ui_03_02_*` |
| CA-UI-03-03 | Diseño: `PopupProperties(focusable = false)`; I `FormFocusTest.ca_ui_03_05_*` (el foco sigue en los campos) |
| CA-UI-03-04 | U `AbovePopupPositionProviderTest` (3 casos), I `FormFocusTest.ca_ui_03_04_*` (paso 3 en 384×857, ambos campos) |
| CA-UI-03-05 | I `FormFocusTest.ca_ui_03_05_*` |
| CA-UI-03-06 | I `FormFocusTest.ca_ui_03_02_*` |
| CA-UI-04-01 | Manifest `screenOrientation="portrait"`; M |
| CA-UI-04-02 | Diseño: ancho máximo de 480 dp en el formulario |
| CA-UI-05-01 | U `ThemeModeTest`, I `ColorModeTest.ca_ui_05_01_*` (tema y pantalla de éxito con el teléfono en modo oscuro; se comprueba el píxel de fondo `GreenLight`) |
| CA-UI-05-02 | M (V2436 en modo oscuro: arranque en frío capturado cuadro a cuadro; el primer cuadro es `#F0FDF4` con el logo y ningún cuadro es oscuro) |
| CA-UI-05-03 | U `ThemeModeTest`, I `ColorModeTest.ca_ui_05_03_*`; `isSystemInDarkTheme()` solo en `ThemeMode.kt` (RNF-UI-05) |
| CA-UI-05-04 | Revisión de código: `Color.kt` y los esquemas de `Theme.kt` sin cambios; el `#F0FDF4` fijo de la Splash pasa al token `GreenLight` (mismo valor) |
| CA-UI-06-01 | I `LauncherIconTest.ca_ui_06_01_*` (`AdaptiveIconDrawable` con fondo `#F0FDF4`, normal y redondo) |
| CA-UI-06-02 | `tools/generate_launcher_icon.py` (aserción por densidad: ningún píxel visible fuera del círculo de 66 dp); vista previa con máscaras de círculo, squircle y cuadrado |
| CA-UI-06-03 | I `LauncherIconTest.ca_ui_06_03_*` (ejecutada en Android 13) |
| CA-UI-06-04 | `tools/generate_launcher_icon.py` versionado; README con el comando |
| CA-UI-07-01 | M (capturas a 412×917 comparadas con los frames de Figma); revisión de código: `Poppins` en `Type.kt` |
| CA-UI-07-02 | I `ColorModeTest` (píxel de fondo `GreenLight`); M (comparación con Figma); revisión de código: solo cambian roles en `LightColorScheme` |
| CA-UI-07-03 | U `FormMetricsTest`, I `FormProfileFitTest` (incluye el cambio de paso en la recuperación), I `AdaptiveLayoutTest` (412×917); M (comparación con Figma: Login, pasos 1, 2 y 3 y Éxito con el perfil `Regular`; Registro baja a `Medium` porque no cabe) |
| CA-UI-07-04 | I `FormFocusTest.ca_ui_03_*` (el popup sigue encima); M (captura del checklist frente al nodo 572:1299) |
| CA-UI-07-05 | M (captura del paso 2 frente al nodo 585:4467) |
| CA-UI-07-06 | M (captura del diálogo frente al nodo 585:4441; desenfoque en Android 12+) |
| CA-UI-07-07 | M (captura del paso 3 con el botón deshabilitado gris) |
| CA-UI-07-08 | I `FormProfileFitTest.ca_ui_07_08_*` (2), M (Login → Registro → atrás → Registro en el V2436) |
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

**Confirmado en un segundo dispositivo (moto g32, Android 13):** con el mismo APK, el Login muestra íconos **claros**
sobre el verde y la Splash, íconos oscuros; System UI aplica `mDarkIntensity=0.0` en el Login. **El problema es
exclusivo de OriginOS 6 (vivo).**

En el moto, durante la animación de apertura de un arranque en frío lento (~2 s), los íconos se ven blancos sobre la
pantalla de arranque clara. Sin embargo, esa ventana del sistema ("Splash Screen com.smartfood.app") **sí pide**
íconos oscuros (`apr=LIGHT_STATUS_BARS`), gracias al tema claro de HU-UI-05. Es el comportamiento de System UI
mientras termina la transición de apertura, y la app no lo controla.

**Conclusión (vivo):** la app y Android coinciden (íconos claros). En la capa de vivo, `StatusBarTransitionsController` queda
con `mPendingDarkIntensity=0.0` (el valor correcto) sin aplicarlo, y mantiene `mDarkIntensity=1.0` hasta que un
evento propio de System UI, como abrir el panel de ajustes rápidos, lo fuerza. Las dos capturas con íconos blancos
coinciden con momentos en que se había abierto ese panel. Ninguna API pública de la app reproduce ese evento. **Pendiente:** verificar en otro dispositivo o en un emulador para confirmar que ocurre solo en vivo.

**2. Margen insuficiente con mensaje de error en otro dispositivo (corregido).**

En el moto g32 (1080×2400, 400 dpi, otra fuente del sistema), `AdaptiveLayoutTest` detectó que el Registro con
mensaje de error se desbordaba 4,8 dp en 384×857 y 0,8 dp en 360×720; en el vivo cabía justo. Se ampliaron los
márgenes de los perfiles Medio (separación entre secciones 16→12 dp, panel 28/16→24/12 dp) y Compacto (separación
12→10 dp, panel 20/12→16/8 dp). Resultado: **51/51 instrumentadas en verde en el moto g32**.

**3. Autoenfoque al abrir el Registro. No se reproduce.**

Ocurrió una vez, justo después de ejecutar la suite instrumentada. Se descartó la hipótesis del "modo táctil": el
teléfono sigue con `mInTouchMode=true` después de las pruebas. Tampoco se reprodujo con instalación limpia ni en
5 intentos más. Nota: `connectedDebugAndroidTest` **desinstala la app** al terminar.
