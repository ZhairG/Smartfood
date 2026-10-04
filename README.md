# SmartFood

Aplicación Android para reducir el desperdicio de comida en los hogares. Une **inventario**, **fechas de vencimiento** y
**presupuesto**, y sobre ellos actúa un **agente de recomendación de recetas basado en utilidad**.

**Equipo:** Dariany Natalia Bernal Duarte · Laura Nikoll Malagón Gómez · Santiago Ardila Vargas · Zhair David Gómez Miranda
**Cursos:** Construcción de Aplicaciones Móviles · Inteligencia Artificial — UTADEO 2026-2

---

## Estado

| Módulo | Spec | Estado |
|---|---|---|
| 001 Autenticación | [`specs/001-autenticacion`](specs/001-autenticacion/spec.md) | ✅ Implementado (v1) |
| 002 Inventario | — | Pendiente |
| 003 Catálogo de recetas | — | Pendiente |
| 004 Agente recomendador | — | Pendiente |

## Metodología: Spec Driven Development

El proyecto sigue **SDD**: ninguna funcionalidad se programa sin especificación.

```
specs/
├── constitution.md              Principios del proyecto (arquitectura, convenciones, Definition of Done)
└── 001-autenticacion/
    ├── spec.md                  QUÉ y POR QUÉ: historias de usuario y criterios Dado/Cuando/Entonces
    ├── plan.md                  CÓMO: arquitectura, contratos, modelo de datos y decisiones (ADR)
    └── tasks.md                 Tareas y matriz de trazabilidad criterio → prueba
```

Cada criterio de aceptación (p. ej. `CA-02-03`) tiene al menos una prueba que lo cita en su nombre.

## Arquitectura

**Clean Architecture + MVVM con UDF**, organizada **por features**:

```
com.smartfood.app/
├── core/
│   ├── common/          safeRunCatching (coroutines seguras)
│   ├── designsystem/    theme/ (tokens de Figma), components/ (SmartFood*), util/
│   ├── di/              CoreDataModule: FirebaseAuth, Firestore, DataStore
│   └── navigation/      AppNavHost, Routes type-safe, NavTransitions, ObserveEffects
└── feature/
    ├── splash/          Decide el destino inicial según la sesión
    ├── home/            Saludo y cierre de sesión (punto de entrada de las próximas features)
    └── auth/
        ├── domain/      model/ (User, AuthError), repository/ (interfaces), usecase/
        ├── data/        repository/ (Firebase), mapper/, local/ (DataStore), demo/, di/
        └── ui/          login/, register/, recovery/, components/, AuthErrorText, AuthNavGraph
```

- `ui → domain ← data`: el dominio es Kotlin puro y define las interfaces; la capa de datos las implementa.
- Un ViewModel por pantalla con `StateFlow<UiState>`, eventos `onEvent(Event)` y efectos de un solo uso para navegar.
- Errores tipados (`AuthError`) en lugar de comparar textos de excepciones.

## Tecnologías

Kotlin · Jetpack Compose (Material 3) · Hilt · Navigation Compose type-safe · Coroutines/Flow ·
Firebase Authentication · Cloud Firestore · DataStore · JUnit4 · Compose UI Test

## Puesta en marcha

1. Abrir el proyecto en Android Studio y sincronizar Gradle.
2. En la consola de Firebase (`smartfood-f62a1`):
   - **Authentication** → habilitar *Correo/contraseña*.
   - **Firestore Database** → crear la base de datos.
   - Publicar las reglas: `firebase deploy --only firestore:rules`.
3. Ejecutar la configuración `app`.

## Datos de prueba: recuperación de contraseña (simulada)

En la v1, la recuperación de contraseña es una **simulación** de interfaz: no envía correos ni cambia la contraseña real
(ver [ADR-01](specs/001-autenticacion/plan.md#8-decisiones-adr)).

| Dato | Valor |
|---|---|
| Correos con cuenta | `demo@smartfood.com`, `prueba@smartfood.com` |
| Código de verificación | `1234` |

## Comandos

```bash
./gradlew :app:assembleDebug              # Compilar APK debug
./gradlew :app:testDebugUnitTest          # Pruebas unitarias
./gradlew :app:connectedDebugAndroidTest  # Pruebas instrumentadas (requiere emulador)
./gradlew :app:lintDebug                  # Android Lint
```
