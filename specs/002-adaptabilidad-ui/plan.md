# Plan técnico 002: Adaptabilidad de la UI, foco y teclado

> El CÓMO de [`spec.md`](./spec.md). Cumple la [constitución](../constitution.md).

## 1. Presupuesto de alto (Registro, la pantalla más alta)

| Bloque | Figma | Medio | Compacto |
|---|---|---|---|
| Encabezado (logo + nombre + márgenes) | 238 | 150 | 72 (en fila) |
| Panel: margen + título + subtítulo | 172 | 128 | 106 |
| 3 campos (etiqueta + caja) | 259 | 252 | 226 |
| Términos + botón + enlace + margen final | 236 | 166 | 162 |
| **Contenido** | **~905** | **~696** | **~566** |
| **Perfil (alto útil)** | **≥ 1000 dp** | **760–1000 dp** | **< 760 dp** |

El perfil se elige con el **alto útil**: el alto del contenedor menos las barras de estado y de navegación
(`formMetricsForWindow`). Un teléfono de 360×720 dp deja unos 660 dp útiles: perfil compacto, con ~90 dp de margen
para una línea de error. Uno de 384×857 dp deja unos 800 dp: perfil medio.

## 2. `FormMetrics` (design system)

`core/designsystem/theme/FormMetrics.kt`: única fuente de las medidas que cambian con el alto (RNF-UI-03).

```kotlin
data class FormMetrics(
    val inlineBrand: Boolean, val logoSize: Dp, val wordmarkFontSize: TextUnit, val wordmarkLineHeight: TextUnit,
    val headerPaddingTop: Dp, val headerPaddingBottom: Dp,
    val panelPaddingTop: Dp, val panelPaddingBottom: Dp, val formHorizontalPadding: Dp,
    val titleFontSize: TextUnit, val titleLineHeight: TextUnit,
    val labelFontSize: TextUnit, val labelSpacing: Dp, val fieldSpacing: Dp, val sectionSpacing: Dp,
    val inputHeight: Dp, val buttonHeight: Dp, val otpBoxSize: Dp, val successIconSize: Dp,
    val stepIndicatorPadding: Dp,
)
fun formMetricsFor(availableHeight: Dp): FormMetrics   // alto útil: < 760 Compact · < 1000 Medium · resto Regular
@Composable fun formMetricsForWindow(maxHeight: Dp): FormMetrics  // descuenta WindowInsets.systemBars
val LocalFormMetrics = staticCompositionLocalOf { FormMetrics.Regular }
```

El valor por defecto es `Regular` (Figma), así que las vistas previas y las pantallas que no lo proveen (como Home)
no cambian.

## 3. `AuthScreenScaffold` desde cero

```
BoxWithConstraints ── provee LocalFormMetrics = formMetricsForWindow(maxHeight)
└─ Column (fillMaxSize, statusBars, clearFocusOnTap)
   ├─ topHeaderContent        (indicador de pasos: nunca se colapsa)
   ├─ CollapsibleBrandHeader  (alto = completo − inset del teclado, alfa proporcional)
   └─ Panel (weight 1, esquinas redondeadas, padding inferior = max(barra de navegación, teclado))
      ├─ headerContent        (título y subtítulo)
      └─ Column(verticalScroll) ── formContent
```

- **Encabezado colapsable (CA-UI-02-05, ADR-UI-01):** un modificador `layout` lee `WindowInsets.ime` **en la
  fase de layout**. Así cada frame de la animación del teclado solo vuelve a medir, sin recomponer. El
  encabezado se encoge píxel a píxel con el teclado.
- **El campo enfocado queda visible (CA-UI-02-04, ADR-UI-02):** el panel aplica el inset del teclado como
  padding, fuera del `verticalScroll`. El viewport se reduce mientras sube el teclado, y el contenedor de scroll
  de Compose mantiene visible al hijo enfocado en cada frame. Esto es nativo y no hay ningún `animateScrollTo`
  (RNF-UI-02).
- **Al cerrar el teclado (CA-UI-02-06):** `ClearFocusOnKeyboardDismiss()` (en `util/KeyboardExt.kt`) detecta la
  transición de visible a oculto y quita el foco. Como con el teclado cerrado todo cabe, no hay nada que
  desplazar.
- **Tocar fuera (CA-UI-02-07):** se reutiliza `clearFocusOnTap` (`util/ModifierExt.kt`).

## 4. Campos de texto (`SmartFoodTextField`)

- La **etiqueta pasa dentro del `decorationBox`** del `BasicTextField`:
  - Tocarla toca el propio campo, así que recibe el foco sin un `FocusRequester` (CA-UI-02-01).
  - Su texto queda en la semántica del campo, así que TalkBack lo anuncia (CA-UI-02-08).
- El foco se notifica con `Modifier.onFocusChanged`, en el mismo frame y sin `LaunchedEffect` (RNF-UI-04).
- El alto y los espacios salen de `LocalFormMetrics`.
- Acciones del teclado:
  - "Siguiente" usa la acción por defecto de Compose, que mueve el foco al siguiente campo (CA-UI-02-02).
  - "Listo" quita el foco (con eso se oculta el teclado) y envía el formulario (CA-UI-02-03). En el OTP se agrega
    un parámetro `onDone`.

## 5. Popup anclado (checklist)

`core/designsystem/components/AnchoredPopup.kt`:

- `AbovePopupPositionProvider(gapPx, minTopPx)` es pura y se prueba en la JVM:
  - **x:** centrado sobre el ancla, ajustado para no salirse de la ventana.
  - **y:** `anchor.top − gap − alto`. Si eso queda por encima de `minTopPx` (la barra de estado), el popup va a
    `anchor.bottom + gap` (CA-UI-03-04).
- `AnchoredPopup(visible, popupContent) { ancla }`:
  - Mide el ancho del ancla para que el popup tenga el mismo ancho.
  - Usa `PopupProperties(focusable = false)`, así que no roba el foco y el teclado sigue abierto (CA-UI-03-03).

`feature/auth/ui/components/PasswordFieldWithChecklist.kt` combina `SmartFoodPasswordField` y `AnchoredPopup`.
El popup es visible mientras el campo tiene el foco (CA-UI-03-01, 02 y 05). `PasswordRequirementChecklist` pasa a
un estilo compacto: unos 115 dp con 4 reglas.

## 6. Modo de color (HU-UI-05)

```
ThemeMode { LIGHT, DARK, SYSTEM }   Default = LIGHT
   └─ ThemeMode.isDark()            único lugar con isSystemInDarkTheme()
        └─ SmartfoodTheme(themeMode) ── provee LocalIsDarkTheme + LightColorScheme/DarkColorScheme (sin cambios)
             ├─ ActionSuccessScreen lee LocalIsDarkTheme (ya no el sistema)
             └─ HomeScreen / ActionSuccessScreen ajustan los íconos de las barras según LocalIsDarkTheme
```

- **Antes de Compose:** el tema XML pasa a `Theme.Material.Light.NoActionBar` con `windowBackground = GreenLight`
  (`@color/smartfood_green_light`, el mismo valor que el token) e íconos oscuros en las barras.
- **Android 12+:** `values-v31/themes.xml` define `windowSplashScreenBackground = GreenLight` y
  `windowSplashScreenAnimatedIcon = @drawable/splash_icon`: un `layer-list` de 240 dp con el logo a 128×135 dp, el
  mismo tamaño que la Splash de Compose. Así la pantalla del sistema y la Splash se ven iguales.
- No existe `values-night/`, así que el tema XML es claro en ambos modos del sistema.

## 7. Decisiones (ADR)

| # | Decisión | Alternativa descartada | Motivo |
|---|---|---|---|
| ADR-UI-01 | El colapso del encabezado se deriva del inset del teclado en la fase de layout | `AnimatedVisibility` disparado por `isImeVisible` | La animación propia arranca después del teclado y dura otro tanto, lo que produce un retraso visible. El inset ya viene animado por el sistema. |
| ADR-UI-02 | Llevar a la vista con el mecanismo nativo de foco y el padding del teclado | `animateScrollTo(maxValue)` en cada campo | El scroll manual compite con el del sistema y se ejecuta un frame después (salto y retraso). |
| ADR-UI-03 | Tres perfiles de medidas según el alto | Un solo diseño compacto | Conserva Figma en tablets y una versión intermedia en teléfonos altos. |
| ADR-UI-04 | Etiqueta dentro del `decorationBox` | `FocusRequester` en un `Text` clicable | Menos código; la accesibilidad y el foco quedan correctos por construcción. |
| ADR-UI-05 | Orientación vertical bloqueada en el manifest | Diseño de dos paneles en horizontal | Coherente con RNF-05 de la spec 001 y con el plazo. Las tablets usan el ancho máximo existente. |
| ADR-UI-06 | `ThemeMode` (claro, oscuro o sistema) con claro por defecto | Forzar claro y borrar el modo oscuro | Mismo resultado visual hoy. El esquema oscuro ya diseñado queda listo para un selector futuro sin tocar pantallas. |
| ADR-UI-07 | Pantalla de arranque del sistema con atributos del tema (`windowSplashScreen*`) | Librería `androidx.core:core-splashscreen` | Cero dependencias nuevas. La Splash propia de Compose ya maneja la sesión y el tiempo mínimo. |

## 8. Pruebas

- **JVM:** `formMetricsFor` (umbrales) y `AbovePopupPositionProvider` (arriba, abajo y límites).
- **Instrumentadas:**
  - Con `DeviceConfigurationOverride.ForcedSize`: en 360×720 y 384×857, cada pantalla tiene
    `VerticalScrollAxisRange.maxValue == 0` (no hay scroll posible) y el botón se ve.
  - De foco: tocar la etiqueta, "Siguiente", aparición del popup con su borde inferior por encima del campo, y
    desaparición del popup al cambiar de campo.
