# Constitución del proyecto SmartFood

> Principios que no cambian entre features. Toda spec, plan, tarea y línea de código debe cumplirlos.
> Si una feature necesita romper un principio, primero se enmienda este documento (sección "Enmiendas").

**Equipo:** Dariany Natalia Bernal Duarte, Laura Nikoll Malagón Gómez, Santiago Ardila Vargas, Zhair David Gómez Miranda
**Cursos:** Construcción de Aplicaciones Móviles · Inteligencia Artificial — UTADEO 2026-2
**Versión:** 1.0 · Octubre 2026

---

## 1. Propósito del producto

SmartFood ayuda a hogares urbanos colombianos a reducir el desperdicio de comida. Une tres variables que las
apps existentes tratan por separado: **inventario** (qué hay), **vencimiento** (qué se daña primero) y
**presupuesto** (cuánto se puede gastar). Sobre esos datos actúa un **agente de recomendación basado en
utilidad** que ordena recetas de un catálogo curado (ver Avance 2 de IA).

## 2. Flujo de trabajo: Spec Driven Development

Ninguna funcionalidad se programa sin especificación. El orden es obligatorio:

1. **`spec.md`**: el QUÉ y el POR QUÉ. Historias de usuario, criterios de aceptación (Dado/Cuando/Entonces),
   requisitos y lo que queda fuera de alcance. No menciona tecnología.
2. **`plan.md`**: el CÓMO. Arquitectura, contratos (interfaces), modelo de datos y decisiones técnicas.
3. **`tasks.md`**: lista ordenada de tareas pequeñas, cada una ligada a un requisito o criterio.
4. **Código y pruebas**: cada criterio de aceptación tiene al menos una prueba que lo cita por su código.

Cada feature vive en `specs/NNN-nombre-corto/`. Si el código y la spec discrepan, se corrige **primero la spec**
y después el código.

### Códigos de trazabilidad

| Prefijo | Significado | Ejemplo |
|---|---|---|
| `HU-NN` | Historia de usuario | `HU-02` Inicio de sesión |
| `CA-NN-MM` | Criterio de aceptación MM de la historia NN | `CA-02-03` |
| `RF-XXX-NN` | Requisito funcional | `RF-AUTH-04` |
| `RNF-NN` | Requisito no funcional | `RNF-01` |
| `TNNN` | Tarea de implementación | `T012` |

A partir de la spec 002, los códigos llevan un prefijo corto de la spec para no repetirse (por ejemplo,
`CA-UI-01-02` en la spec 002). La spec 001 conserva sus códigos sin prefijo.

## 3. Arquitectura (no negociable)

- **Un módulo Gradle (`:app`) con paquetes por feature**: `feature/<nombre>/{domain,data,ui}` más `core/`
  para lo compartido (diseño, navegación, utilidades y DI).
- **Clean Architecture**: las dependencias apuntan hacia adentro, `ui → domain ← data`.
  - `domain` es Kotlin puro: sin Android, sin Firebase, sin Compose. Define los **modelos** y las
    **interfaces de repositorio**.
  - `data` implementa esas interfaces. Es la única capa que conoce Firebase o DataStore.
  - `ui` solo conoce `domain`.
- **MVVM + UDF**: un ViewModel por pantalla. Cada ViewModel expone `StateFlow<XUiState>` inmutable,
  recibe `onEvent(XEvent)` (sealed) y emite efectos de un solo uso (`Flow<XEffect>`), como la navegación.
- **Pantallas stateless**: reciben `state`, `onEvent` y callbacks de navegación. Nunca reciben un ViewModel.
- **Validaciones de negocio solo en los UseCases.** El ViewModel no repite reglas.
- **Errores tipados**: las fallas viajan como `Result<T>` con `AuthException(AuthError)` (o el equivalente de
  la feature). Prohibido comparar mensajes de texto de excepciones fuera del mapper de la capa `data`.
- **Coroutines**: en la capa `data`, usar `safeRunCatching` (`core/common/CoroutinesExt.kt`) en lugar de
  `runCatching`, para no tragarse una `CancellationException`.
- **DI con Hilt**: las dependencias se inyectan **no nulas**. Para probar se usan fakes de las interfaces,
  nunca parámetros nulos.

## 4. Seguridad

- **Ningún secreto en el código ni en el APK**: ni API keys de terceros ni credenciales.
  `google-services.json` es la única configuración de cliente permitida.
- Lo que no puede hacerse de forma segura en el cliente **se simula y se documenta como simulación** en la
  spec, o se mueve a un backend. Nunca se presenta como si fuera real.
- Reglas de Firestore con privilegio mínimo: cada usuario solo lee y escribe sus propios documentos.

## 5. Convenciones de código

- Idioma: código, KDoc, comentarios, commits y nombres de tests en **español**. Los identificadores siguen
  las convenciones de Kotlin.
- Cada archivo `.kt` empieza con la cabecera KDoc que exige el curso:
  ```kotlin
  /**
   * Nombre: Archivo.kt
   * Autor: SmartFood Team
   * Fecha: Mes Año
   * Descripción: …
   */
  ```
- Todo texto visible va en `res/values/strings.xml`. El mapeo de un error de dominio a su texto vive en un
  solo archivo por feature.
- Design system: colores, tipografía y espaciados en `core/designsystem/theme`. Los componentes reutilizables
  tienen el prefijo `SmartFood` y viven en `core/designsystem/components`.

## 6. Pruebas

- **Unitarias (JVM)** para cada UseCase, mapper, repositorio con lógica propia y ViewModel.
- **Instrumentadas (Compose)** para pantallas y navegación.
- El nombre del test cita el criterio de aceptación:
  `` `CA-02-03 credenciales incorrectas muestran error de credenciales` ``.
- Fakes escritos a mano y compartidos en `app/src/sharedTest/.../testutil/` (los usan las pruebas unitarias y las instrumentadas). No se usa una librería de mocks.

## 7. Definition of Done

Una tarea está terminada cuando se cumple todo esto:

- [ ] Cumple los criterios de aceptación citados en `tasks.md`.
- [ ] `./gradlew :app:testDebugUnitTest` pasa.
- [ ] `./gradlew :app:assembleDebug` compila sin errores.
- [ ] No introduce secretos (`git grep` limpio).
- [ ] La spec, el plan o las tareas se actualizaron si la implementación cambió algo.

## Enmiendas

| Fecha | Cambio | Motivo |
|---|---|---|
| 2026-10-04 | Versión 1.0 | Adopción de SDD para el módulo 1 |
| 2026-10-04 | Códigos con prefijo por spec | Evitar choques de códigos entre la spec 001 y la 002 |
