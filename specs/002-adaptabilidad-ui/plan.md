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
  - **y:** `anchor.top − gap − alto`, **siempre encima** del ancla. Si eso queda por encima de `minTopPx` (la barra
    de estado), se ajusta a `minTopPx` y cubre la parte superior del ancla (la etiqueta), pero nunca pasa debajo
    (CA-UI-03-04). Con el perfil correcto (sección 8) el checklist cabe encima en los pasos y pantallas actuales.
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

## 7. Ícono oficial (HU-UI-06)

`tools/generate_launcher_icon.py` (Python + Pillow) genera todo el ícono a partir de
`res/drawable-nodpi/logo_smartfood_circle.png`:

| Salida | Tamaño | Uso |
|---|---|---|
| `mipmap-*/ic_launcher_foreground.webp` | lienzo de 108 dp (108–432 px) | Capa frontal del ícono adaptativo |
| `mipmap-*/ic_launcher_monochrome.webp` | 108 dp | Silueta para íconos temáticos (Android 13+) |
| `mipmap-*/ic_launcher.webp`, `ic_launcher_round.webp` | 48 dp (48–192 px) | Android 7.x |
| `ic_launcher-playstore.png` | 512×512 | Ficha de Google Play |

- La escala hace que **todo píxel visible** quede dentro del círculo de 66 dp (zona segura); el script lo verifica.
- Fondo del ícono adaptativo: `@color/smartfood_green_light` (`GreenLight`).
- WebP sin pérdida: los píxeles del logo solo se reescalan (LANCZOS), sin compresión con pérdida.

## 8. Fidelidad con Figma (HU-UI-07)

- **Tipografía:** Poppins Regular, Medium, SemiBold y Bold en `res/font` (licencia OFL en `docs/licenses/`).
  `theme/Type.kt` ya tenía los tamaños, pesos y altos de línea de Figma; solo faltaba la familia.
- **Roles de color del esquema claro**, sin cambiar el valor de ningún token:

| Rol | Antes | Ahora (Figma "_Claro") |
|---|---|---|
| `surface` (panel) | `SurfaceLight` | `GreenLight` |
| `onSurface` (etiquetas, texto escrito, checklist) | `GreenDark` | `Black` |
| `onSurfaceVariant` (subtítulos, placeholders, textos de enlaces) | `GreyNeutral` | `GreyLight` |
| `outline` (borde de campo) | `GreyDivider` | `GreyNeutral35` |

- **Token nuevo:** `GreenCheck` (`#16A34A`), la base sólida de `GreenMain50` y `GreenMain20`, para la casilla y el check del checklist.
- **Componentes:**
  - Botón: SemiBold; deshabilitado en `GreyDark`; cargando en naranja.
  - Enlaces: SemiBold, con tamaño por pantalla.
  - Código: casillas rectangulares (`FormMetrics.otpBoxWidth/Height/Spacing`).
  - Checklist: estilo de Figma, ancho propio de 283 dp en `AnchoredPopup`.
  - Diálogo sin conexión: desenfoca el contenido con `Modifier.blur` (en Android 11 o anterior no tiene efecto y queda la atenuación del `Dialog`). La tarjeta ocupa el ancho de la pantalla a 6 dp de los bordes (`usePlatformDefaultWidth = false`, máximo 480 dp).
- **Distribución vertical (CA-UI-07-03):**
  - **Perfil por pantalla.** `formMetricsForWindow` da el perfil *inicial* según el alto útil (`Regular` desde
    720 dp, `Medium` desde 620 dp). Si con el teclado cerrado el formulario se desborda, `AuthScreenScaffold` baja al
    perfil siguiente (`FormMetrics.smaller()`). Solo baja, nunca sube, así que no oscila. El desborde solo cuenta
    cuando el scroll ya se midió (`viewportSize > 0`): antes del primer layout `maxValue` vale `Int.MAX_VALUE` y
    bajaba de perfil sin motivo. Además, el desborde debe durar `OVERFLOW_SETTLE_MS` (400 ms): los pasajeros no
    cuentan. En el fundido entre pasos de la recuperación, el paso saliente conservaba su alto un instante y el perfil
    terminaba en el compacto (logo en fila) en el V2436. Por eso el `AnimatedContent` de los pasos tampoco anima el
    tamaño (`using(null)`).
  - **Separaciones flexibles.** `FlexibleGap(minHeight, weight)` es una separación con un mínimo fijo más una parte
    del alto sobrante. El contenido del scroll recibe como alto mínimo el del viewport, así que las separaciones
    crecen cuando sobra alto y vuelven a su mínimo cuando falta o con el teclado abierto. No hay medidas por pantalla.
  - Los pasos de recuperación viven dentro de un `AnimatedContent`, que no transmite el alto mínimo. Por eso cada
    paso usa `Modifier.fillFormViewport(LocalFormViewport.current)`, que lee el alto del viewport en la fase de layout.
  - **Encabezado de Figma en `Regular`:**
    - 53 dp bajo la marca, de modo que el panel empieza en y = 296 como en el frame.
    - 24 dp al pie del panel, porque el resto lo reparten las separaciones flexibles.
    - Con indicador de pasos, el indicador ocupa el margen superior y el logo queda a la misma altura que en el Login.

## 9. Decisiones (ADR)

| # | Decisión | Alternativa descartada | Motivo |
|---|---|---|---|
| ADR-UI-01 | El colapso del encabezado se deriva del inset del teclado en la fase de layout | `AnimatedVisibility` disparado por `isImeVisible` | La animación propia arranca después del teclado y dura otro tanto, lo que produce un retraso visible. El inset ya viene animado por el sistema. |
| ADR-UI-02 | Llevar a la vista con el mecanismo nativo de foco y el padding del teclado | `animateScrollTo(maxValue)` en cada campo | El scroll manual compite con el del sistema y se ejecuta un frame después (salto y retraso). |
| ADR-UI-03 | Tres perfiles de medidas según el alto | Un solo diseño compacto | Conserva Figma en tablets y una versión intermedia en teléfonos altos. |
| ADR-UI-04 | Etiqueta dentro del `decorationBox` | `FocusRequester` en un `Text` clicable | Menos código; la accesibilidad y el foco quedan correctos por construcción. |
| ADR-UI-05 | Orientación vertical bloqueada en el manifest | Diseño de dos paneles en horizontal | Coherente con RNF-05 de la spec 001 y con el plazo. Las tablets usan el ancho máximo existente. |
| ADR-UI-06 | `ThemeMode` (claro, oscuro o sistema) con claro por defecto | Forzar claro y borrar el modo oscuro | Mismo resultado visual hoy. El esquema oscuro ya diseñado queda listo para un selector futuro sin tocar pantallas. |
| ADR-UI-07 | Pantalla de arranque del sistema con atributos del tema (`windowSplashScreen*`) | Librería `androidx.core:core-splashscreen` | Cero dependencias nuevas. La Splash propia de Compose ya maneja la sesión y el tiempo mínimo. |
| ADR-UI-09 | Ajustar a Figma cambiando los **roles** de color del esquema claro, no los valores de los tokens | Colores fijos en cada componente | Un solo lugar (`Theme.kt`) y la paleta intacta; el esquema oscuro no se toca. |
| ADR-UI-10 | Poppins empaquetada en el APK | Fuentes descargables (Google Play Services) | Se ve igual en cualquier teléfono, sin conexión y sin depender de Play Services (~630 KB). |
| ADR-UI-11 | Separaciones flexibles (`FlexibleGap`) con un mínimo por perfil | Medidas fijas de Figma por pantalla, o `Arrangement.SpaceBetween` | Con medidas fijas, el formulario no cabría en pantallas bajas y quedaría un hueco en las altas. `SpaceBetween` no respeta las proporciones de Figma ni un mínimo. Las separaciones flexibles se adaptan a cualquier alto y al teclado sin código por pantalla. |
| ADR-UI-08 | Ícono adaptativo raster generado desde el PNG | VectorDrawable del logo | El logo tiene 23.403 colores y degradados: un vector idéntico necesitaría decenas de miles de trazados (varios MB, dibujo lento) y uno trazado altera los degradados. El raster conserva el diseño original. |

## 10. Pruebas

- **JVM:** `formMetricsFor` (umbrales) y `AbovePopupPositionProvider` (arriba, abajo y límites).
- **Instrumentadas:**
  - Con `DeviceConfigurationOverride.ForcedSize`: en 360×720 y 384×857, cada pantalla tiene
    `VerticalScrollAxisRange.maxValue == 0` (no hay scroll posible) y el botón se ve. También en 412×917 (Figma).
  - `FormProfileFitTest`: un formulario que cabe conserva `Regular` y uno que no cabe baja hasta `Compact`.
  - De foco: tocar la etiqueta, "Siguiente", aparición del popup con su borde inferior por encima del campo, y
    desaparición del popup al cambiar de campo.
