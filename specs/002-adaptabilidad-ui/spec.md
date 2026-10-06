# Spec 002: Adaptabilidad de la UI, foco y teclado

| Campo | Valor |
|---|---|
| Feature | `002-adaptabilidad-ui` |
| Estado | Implementada (v1) |
| Fecha | 2026-10-04 |
| Depende de | 001-autenticacion |
| Afecta | Login, Registro, Recuperación de contraseña, pantallas de éxito y design system |

> Los códigos de esta spec llevan el prefijo `UI` (`HU-UI-01`, `CA-UI-01-01`) para no chocar con los de la 001.
>
> **Alto útil:** el alto de la pantalla sin las barras de estado y de navegación (entre 50 y 80 dp según el
> teléfono).

## 1. Contexto y motivación

Las pantallas de autenticación se diseñaron en Figma para un alto de unos 900 dp. En los teléfonos reales hay que
hacer scroll para llegar al botón principal:

| Pantalla | Alto necesario | Teléfono de 360×800 dp |
|---|---|---|
| Registro | ~935 dp | Necesita scroll |
| Recuperación, paso 3 | ~940 dp | Necesita scroll |
| Login | ~830 dp | Al límite |

Además, el manejo actual del teclado produce **saltos y retrasos**:
- Al cerrar el teclado, la pantalla vuelve de golpe arriba.
- Cada campo dispara su propio scroll animado al recibir el foco.
- La lista de requisitos de la contraseña depende de un desplazamiento fijo para colocarse.

Esta spec define cómo deben comportarse las pantallas para que el usuario complete cualquier formulario **sin
hacer scroll** y con un teclado que **responde al instante**.

## 2. Historias de usuario y criterios de aceptación

### HU-UI-01: Formularios sin scroll
**Como** usuario **quiero** ver todo el formulario y su botón sin desplazarme **para** completarlo más rápido.

| Código | Criterio |
|---|---|
| CA-UI-01-01 | **Dado** un teléfono de al menos 360×720 dp con el teclado cerrado, **entonces** Login, Registro y cada paso de la recuperación caben completos en pantalla, botón principal incluido, sin necesidad de scroll. |
| CA-UI-01-02 | **Dado** el mismo tamaño, **entonces** las pantallas de éxito ("¡Cuenta creada con éxito!" y "¡Contraseña actualizada!") caben completas. |
| CA-UI-01-03 | **Dado** un alto útil de 1000 dp o más (tablets), **entonces** se conservan las medidas exactas de Figma. |
| CA-UI-01-04 | **Dado** un alto útil de 760 a 1000 dp (por ejemplo, un teléfono de 384×857 dp), **entonces** se usa una versión intermedia: el logo y los espaciados se reducen y se mantiene el encabezado en columna. |
| CA-UI-01-05 | **Dado** un alto útil menor de 760 dp (por ejemplo, un teléfono de 360×720 dp), **entonces** se usa una versión compacta: logo y nombre en una sola fila, campos y botones más bajos. |
| CA-UI-01-06 | **Dado** un teléfono de menos de 720 dp de alto, **entonces** el formulario sigue siendo usable con scroll como respaldo. |

### HU-UI-02: Teclado y foco sin retrasos
**Como** usuario **quiero** que el teclado y los campos respondan de inmediato **para** escribir sin perder de vista
lo que estoy llenando.

| Código | Criterio |
|---|---|
| CA-UI-02-01 | **Dado** un formulario, **cuando** toco la etiqueta de un campo (p. ej. "Contraseña"), **entonces** ese campo recibe el foco. |
| CA-UI-02-02 | **Dado** un campo enfocado con la acción "Siguiente", **cuando** la pulso en el teclado, **entonces** el foco pasa al siguiente campo. |
| CA-UI-02-03 | **Dado** el último campo del formulario, **cuando** pulso "Listo", **entonces** se oculta el teclado y se envía el formulario. |
| CA-UI-02-04 | **Dado** que se abre el teclado, **entonces** el campo enfocado queda visible de inmediato, sin una animación de scroll posterior. |
| CA-UI-02-05 | **Dado** que se abre el teclado, **entonces** el encabezado de la marca se encoge al mismo ritmo que sube el teclado y reaparece al mismo ritmo cuando baja. El indicador de pasos de la recuperación no se oculta. |
| CA-UI-02-06 | **Dado** un campo enfocado, **cuando** cierro el teclado (botón atrás o gesto), **entonces** el campo pierde el foco y la pantalla queda en su lugar, sin saltos. |
| CA-UI-02-07 | **Dado** un campo enfocado, **cuando** toco una zona sin campos, **entonces** se oculta el teclado y el campo pierde el foco. |
| CA-UI-02-08 | Los lectores de pantalla anuncian cada campo con su etiqueta. |

### HU-UI-03: Requisitos de la contraseña en un popup
**Como** usuario **quiero** ver los requisitos de la contraseña mientras la escribo, sin que ocupen espacio fijo en
el formulario.

| Código | Criterio |
|---|---|
| CA-UI-03-01 | **Dado** el Registro, **cuando** enfoco el campo de contraseña, **entonces** aparece un popup con los requisitos **encima** del campo, sin taparlo. |
| CA-UI-03-02 | **Dado** el paso 3 de la recuperación, **cuando** enfoco "Nueva Contraseña" o "Confirmar Contraseña", **entonces** aparece el popup encima del campo enfocado, incluyendo la regla "¿Coinciden?". |
| CA-UI-03-03 | **Dado** el popup visible, **cuando** escribo, **entonces** cada requisito se marca en tiempo real y el teclado sigue abierto (el popup no roba el foco). |
| CA-UI-03-04 | El popup se muestra **siempre encima** del campo, nunca debajo. **Dado** que no cabe completo encima, **entonces** se ajusta al borde superior útil (debajo de la barra de estado) y cubre como máximo la parte superior del campo (su etiqueta), siempre dentro de la pantalla. |
| CA-UI-03-05 | **Dado** el popup visible, **cuando** el campo pierde el foco, **entonces** el popup desaparece. |
| CA-UI-03-06 | El paso 3 de la recuperación ya no muestra la lista de requisitos fija dentro del formulario. |

### HU-UI-04: Orientación
| Código | Criterio |
|---|---|
| CA-UI-04-01 | **Dado** un teléfono, **entonces** la app se muestra siempre en vertical. |
| CA-UI-04-02 | **Dado** una tablet (desde Android 16 el sistema puede ignorar el bloqueo de orientación en pantallas grandes), **entonces** el formulario se centra con un ancho máximo y sigue siendo usable en cualquier orientación. |

### HU-UI-05: Modo de color claro por defecto
**Como** usuario **quiero** que la app se vea siempre clara y sin destellos oscuros **para** tener una experiencia
consistente con el diseño, aunque mi teléfono esté en modo oscuro.

| Código | Criterio |
|---|---|
| CA-UI-05-01 | **Dado** un teléfono en modo oscuro, **entonces** toda la app se ve en modo claro, incluidas las pantallas de éxito ("¡Cuenta creada con éxito!" y "¡Contraseña actualizada!"). |
| CA-UI-05-02 | **Dado** un arranque en frío, **entonces** antes de la Splash no aparece ningún fondo oscuro. En Android 12+, la pantalla del sistema usa el mismo fondo (`GreenLight`) y el mismo logo que la Splash; en versiones anteriores, la ventana se pinta con ese fondo. |
| CA-UI-05-03 | El modo de color se decide en **un solo lugar** (`ThemeMode`, por defecto CLARO). Las pantallas y componentes no consultan el modo del sistema directamente, de modo que un selector futuro (claro, oscuro o sistema) no exige tocarlos. |
| CA-UI-05-04 | Los colores de cada modo y su asignación por pantalla no cambian: la paleta y los esquemas claro y oscuro quedan iguales. |

### HU-UI-06: Ícono oficial
**Como** usuario **quiero** reconocer la app por su logo en el launcher **para** encontrarla rápido.

| Código | Criterio |
|---|---|
| CA-UI-06-01 | **Dado** el launcher, **entonces** el ícono muestra el logo de SmartFood (bombillo con zanahoria y tomate) sobre fondo `GreenLight`, tanto en Android 8+ (ícono adaptativo) como en Android 7.x. |
| CA-UI-06-02 | **Dado** cualquier forma de máscara del launcher (círculo, cuadrado o gota), **entonces** el logo se ve completo, porque cabe dentro de la zona segura de 66 dp del ícono adaptativo. |
| CA-UI-06-03 | **Dado** Android 13+ con íconos temáticos activados, **entonces** el ícono usa la silueta del logo. |
| CA-UI-06-04 | El ícono se genera de forma reproducible desde el PNG original del logo; no se edita a mano. |

### HU-UI-07: Fidelidad con el diseño de Figma
**Como** equipo **queremos** que la app se vea como el diseño aprobado en Figma (frames "_Claro" del archivo
`z1AoQvluQcmx98gjP4vHF6`), **sin perder** la adaptabilidad de esta spec.

| Código | Criterio |
|---|---|
| CA-UI-07-01 | La tipografía es **Poppins**: Bold en títulos y en la marca, SemiBold en botones y enlaces, y Medium en etiquetas, campos y textos. |
| CA-UI-07-02 | Los colores siguen los frames "_Claro": panel `#F0FDF4`, etiquetas y texto escrito en negro, subtítulos y placeholders en `#9AA39D`, borde de campo `#6F7C74` al 35 % y casilla `#16A34A`. Nodos `585:4420`, `585:4447`, `585:4458`, `585:4467`, `585:4494`, `585:4564` y `700:1913`. |
| CA-UI-07-03 | Cada pantalla usa el perfil de medidas más grande que le cabe: con alto suficiente, las medidas replican Figma; en pantallas más bajas se reducen espaciados y tamaños de forma proporcional, sin cambiar jerarquía, orden ni colores, y se mantiene el cero scroll de HU-UI-01. El alto sobrante se reparte entre los bloques como en Figma (el botón y los enlaces bajan), en lugar de quedar vacío al pie del panel. |
| CA-UI-07-04 | El checklist de contraseña tiene el estilo de Figma: tarjeta blanca de 283 dp con borde y sombra, ítems de 15 sp en negro, check verde si cumple y X naranja si no. Se muestra **encima** del campo (HU-UI-03). |
| CA-UI-07-05 | Las casillas del código de verificación son rectangulares, con la proporción de Figma (50×70 dp). |
| CA-UI-07-06 | Con el diálogo "Sin conexión a Internet" abierto, el fondo se desenfoca (Android 12+) y se atenúa en versiones anteriores. |
| CA-UI-07-07 | El botón principal deshabilitado es gris (`#555555`); mientras carga, sigue naranja con el texto de carga. |
| CA-UI-07-08 | **Dado** que entro o vuelvo a una pantalla, **entonces** se ve directamente con su perfil definitivo: no aparece primero una versión más grande que luego se encoge, y los campos no se pueden usar antes de que el perfil sea el definitivo. |

## 3. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-UI-01 | No se usan desplazamientos fijos ("números mágicos") para colocar elementos flotantes: el popup se posiciona respecto al campo real. |
| RNF-UI-02 | No hay scroll programático (`scrollTo`, `animateScrollTo`) en las pantallas. Llevar el campo enfocado a la vista es responsabilidad del sistema de foco. |
| RNF-UI-03 | Las medidas compactas y regulares viven en un único lugar del design system. |
| RNF-UI-04 | El cambio de foco no espera a un frame adicional: los indicadores de foco y el popup reaccionan en el mismo frame. |
| RNF-UI-05 | `isSystemInDarkTheme()` solo se usa dentro de `ThemeMode.kt`. |

## 4. Fuera de alcance

- Diseño de dos paneles en horizontal.
- Mostrar los errores de cada campo como globos flotantes (siguen debajo del formulario).
- Splash y Home, que ya caben sin scroll.
