# Plan técnico 005: Términos y condiciones y Política de privacidad

> El CÓMO de [`spec.md`](./spec.md). Cumple la [constitución](../constitution.md).

## 1. Arquitectura

```
feature/legal/
├── domain/model/LegalDocument.kt   LegalDocumentType { TERMS, PRIVACY } y LegalVersion.CURRENT (Kotlin puro)
└── ui/
    ├── LegalContent.kt             único lugar que mapea cada tipo a sus @StringRes (título y secciones)
    ├── LegalDocumentScreen.kt      pantalla stateless: barra con "atrás", aviso de vista previa y secciones
    └── LegalNavGraph.kt            NavGraphBuilder.legalDestinations(): rutas TermsOfService y PrivacyPolicy

res/values/strings_legal.xml        textos de la plantilla (RNF-LEG-01)
```

- **Sin ViewModel ni repositorio:** la pantalla no tiene estado ni eventos propios; solo muestra recursos según el
  tipo, igual que `ActionSuccessScreen`. Si el texto llega más adelante desde un servidor, se agregan un
  `LegalDocumentRepository` en `domain` y un ViewModel, sin cambiar la firma de la pantalla.
- **Las rutas viven en la raíz** de `AppNavHost` (no dentro de `AuthGraph`), para que Home o un perfil puedan abrirlas
  después. Usan las transiciones de `NavTransitions` (CA-LEG-01-01 y 02).
- `LegalVersion.CURRENT` está en `domain` porque `RegisterUseCase` (dominio de `auth`) la necesita. Un dominio puede
  depender de otro: los dos son Kotlin puro.

## 2. Contratos

| Elemento | Firma |
|---|---|
| Pantalla | `LegalDocumentScreen(type: LegalDocumentType, onBack: () -> Unit, modifier)` |
| Contenido | `legalContent(type): LegalContent(@StringRes title, List<LegalSection(@StringRes title, @StringRes body)>)` |
| Casilla | `SmartFoodTermsCheckbox(checked, onCheckedChange, onTermsClick, onPrivacyClick, modifier)` |
| Registro | `RegisterScreen(state, onEvent, onNavigateToLogin, onOpenTerms, onOpenPrivacy, modifier)` |
| Perfil | `UserRepository.createProfile(user: User, acceptedTermsVersion: String)` |

## 3. Casilla con enlaces

- Los enlaces son `LinkAnnotation.Clickable` (`withLink`) con `TextLinkStyles` en `secondary` y SemiBold, como en
  Figma. El toque en un enlace lo consume el enlace: la fila `toggleable` no cambia (CA-LEG-01-01, 02 y 04).
- La fila declara `customActions` "Abrir Términos y condiciones" y "Abrir Política de privacidad", porque al fusionar
  la semántica de la fila el lector de pantalla no alcanza los enlaces internos (CA-LEG-01-07).

## 4. Pantalla del documento

- `Scaffold` con `CenterAlignedTopAppBar` (título del documento y flecha "Volver" con `contentDescription`).
- Primer elemento: píldora de vista previa en `secondaryContainer`/`onSecondaryContainer` con "Vista previa: el
  contenido definitivo está en redacción" y la versión (CA-LEG-01-05). Solo se muestra si `LegalVersion.IS_PREVIEW`.
- `LazyColumn` con el texto introductorio y las secciones. Cada título lleva `semantics { heading() }` (CA-LEG-01-06).
- Fondo `surface` (el panel claro). En este design system `background` es el verde del encabezado de marca, y el
  título y los encabezados en `primary` no se verían sobre él. Texto en Poppins (`bodyLarge`/`labelLarge`): los
  estilos `bodyMedium`, `bodySmall` y `titleMedium` del tema no la usan.
- Colores del `MaterialTheme` (claro y oscuro salen del tema; no se usa `isSystemInDarkTheme`). `SystemBarEffect`
  ajusta los íconos de la barra de estado según `LocalIsDarkTheme`.
- Ancho máximo del texto de 600 dp y márgenes de `MaterialTheme.spacing`.

## 5. Constancia de aceptación (HU-LEG-02)

```
users/{uid}
  ├── fullName, email, createdAt   (spec 001)
  ├── termsVersion    : string     versión aceptada (LegalVersion.CURRENT)
  └── termsAcceptedAt : timestamp  FieldValue.serverTimestamp()
```

Reglas: el `create` de `users/{uid}` exige además
`request.resource.data.termsVersion is string && request.resource.data.termsAcceptedAt == request.time`
(CA-LEG-02-02). El `update` sigue permitido al dueño, como en la 001.

## 6. Decisiones (ADR)

| # | Decisión | Alternativas descartadas | Motivo |
|---|---|---|---|
| ADR-LEG-01 | Pantalla completa con su propia ruta | Hoja inferior | Los textos legales son largos; la ruta se reutiliza desde otras features. |
| ADR-LEG-02 | Texto en `strings_legal.xml` | Markdown en `res/raw` con una librería | Sin dependencias nuevas; cumple RNF-03; el texto se reemplaza sin tocar código. |
| ADR-LEG-03 | Una sola versión para los dos documentos | Una versión por documento | Se aceptan juntos con una sola casilla. |
| ADR-LEG-04 | Fecha del servidor validada por las reglas | Fecha del dispositivo | La hora del teléfono se puede cambiar; `request.time` no. |

## 7. Estrategia de pruebas

- **Unitarias**: `RegisterUseCaseTest` (CA-LEG-02-01).
- **Instrumentadas**: `RegisterScreenTest` (enlaces y casilla), `LegalDocumentScreenTest` (aviso, encabezados,
  volver, modo oscuro), `AppNavigationTest` (ida y vuelta conserva el formulario) y `AdaptiveLayoutTest` (el
  registro sigue cabiendo).
- **Manual**: reglas con el emulador de Firestore (CA-LEG-02-02).
