# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code), Gemini or any other AI agent when working with code in this repository. Las reglas de este archivo aplican igual a cualquier modelo o entorno.

## Proyecto

App Android (Kotlin + Jetpack Compose, Material 3) de un solo módulo `:app`, paquete `com.smartfood.app`. Proyecto universitario (UTADEO 2026-2) para reducir el desperdicio de comida en hogares: inventario + vencimientos + presupuesto, con un agente de recomendación de recetas. Hoy están implementados la autenticación (módulo 1), el **inventario** (spec 006), con uso sin conexión, el **perfil** (spec 008) y **recetas + IA** (spec 009). El próximo módulo son las recetas guardadas (#117, spec 010).

`compileSdk`/`targetSdk` 37, `minSdk` 24, Java 11. Todas las dependencias van en `gradle/libs.versions.toml`.

## Flujo de trabajo: Spec Driven Development (obligatorio)

Antes de escribir código, leer **`specs/constitution.md`** (principios no negociables) y la spec de la feature.

- Specs actuales: `001-autenticacion`, `002-adaptabilidad-ui`, `003-recuperacion-real`, `004-correo-registro`, `005-documentos-legales`, `006-inventario`, `008-perfil` y `009-recetas-ia` (implementadas). A partir de la 002, los códigos llevan prefijo (`CA-UI-01-01`, `CA-REC-01-01`, `CA-MAIL-01-01`, `CA-LEG-01-01`, `CA-INV-01-01`, `CA-PER-01-01`, `CA-RIA-01-01`).
- Las capturas de referencia de cada módulo están en `disenos-figma/` (índice en su `README.md`); la fuente de verdad sigue siendo el Figma.
- Cada feature vive en `specs/NNN-nombre/` con `spec.md` (QUÉ: historias, criterios Dado/Cuando/Entonces), `plan.md` (CÓMO: contratos, decisiones) y `tasks.md` (tareas + matriz CA → test).
- Una feature nueva empieza creando su `spec.md`; el código viene después. Si el código y la spec discrepan, se corrige primero la spec.
- Códigos de trazabilidad: `HU-NN`, `CA-NN-MM`, `RF-XXX-NN`, `RNF-NN`. Los nombres de los tests citan el CA que cubren.
- Al revisar comentarios `TODO`, implementarlos aplicando MVVM según la constitución.
- **Plantilla de las specs nuevas:** además de historias, criterios, requisitos, fuera de alcance y supuestos, cada `spec.md` incluye:
  - **Fidelidad con Figma:** frames de referencia, interacciones del prototipo (transiciones con duración y curva) y **Desviaciones** aprobadas.
  - **Plan de entrega:** las features verticales (domain + data + estructura + ui), los commits y los PR previstos (ver "Recomendaciones de Zhair").

## Modo plan con preguntas (obligatorio en toda modificación)

Ninguna modificación de archivos (código, specs, recursos, configuración o documentación) se hace sin pasar antes por el modo plan.

1. **Entrar en modo plan.** Usar el modo plan del entorno (Claude Code: `Shift+Tab` hasta "plan mode"; Gemini CLI: `--approval-mode plan` o `/plan` si la versión lo ofrece; Cursor/Copilot: modo *Ask*/*Plan*). Si el entorno no tiene modo plan, la IA se comporta como si lo tuviera: **solo lee y pregunta, no edita** hasta recibir aprobación explícita.
2. **Investigar sin modificar:** leer la constitución, la spec afectada y el código relacionado.
3. **Hacer siempre preguntas al usuario antes del plan final.** Como mínimo una pregunta por cada categoría que aplique, con opciones concretas y la opción recomendada marcada:
   - **Auditoría:** qué spec, HU y CA cubre el cambio; si el código y la spec discrepan, cuál manda; qué pruebas se agregan o cambian.
   - **Toma de decisiones:** alternativas de implementación con sus ventajas y desventajas (librería, capa donde vive la lógica, alcance del cambio).
   - **Diseño:** frame o nodo de Figma de referencia, estados (vacío, carga, error), modo claro/oscuro, perfiles `Compact`/`Medium`/`Regular`, accesibilidad.
   - **Riesgos:** datos de Firebase, reglas de Firestore, secretos (RNF-01), R8/ProGuard, compatibilidad con `minSdk` 24.
4. **Presentar el plan:** archivos a crear o modificar, pasos, pruebas que citan su CA y cómo se verifica.
5. **Esperar la aprobación explícita** del usuario. Un "sí" a las preguntas no es aprobación del plan. Si durante la implementación aparece una decisión nueva, se detiene y se pregunta otra vez.

## Implementar diseños de Figma (MCP)

El diseño de referencia es el archivo **[SmartFood en Figma](https://www.figma.com/design/z1AoQvluQcmx98gjP4vHF6/SmartFood)** (`fileKey`: `z1AoQvluQcmx98gjP4vHF6`, frames "_Claro"). Toda pantalla o componente nuevo se implementa leyendo ese archivo a través del **MCP de Figma**, no desde capturas ni de memoria.

- Al implementar, pedir al usuario el enlace del frame (`...?node-id=123-456`; en las llamadas el nodo se escribe `123:456`) y obtener el contexto de diseño y una captura del nodo con el MCP.
- El código que entrega Figma es **referencia**, no código final: se adapta a los tokens de `core/designsystem/theme`, a los componentes `SmartFood*`, a `LocalFormMetrics` y al resto de reglas de este archivo. No se copian colores ni medidas fijas.
- Si el diseño pide un color o una medida que no existe, se pregunta en el modo plan (categoría *Diseño*) antes de crear nada.

### Fidelidad total con Figma (exigencia del curso)

El profesor evalúa que la app funcione **exactamente como el prototipo**, como lo exigiría un cliente real que lo aprobó. Es un principio de la [constitución](specs/constitution.md#1-propósito-del-producto).

- **Todo cuenta:** íconos (los del Figma, no sustitutos de Material), colores, tipografía, espaciados, componentes, estados (vacío, carga, error) y **animaciones y transiciones**.
- **Interacciones del prototipo:** además del frame, se leen sus reacciones (transición, dirección, duración y curva) con el MCP. Con `use_figma` en modo solo lectura, `node.reactions` de los nodos del flujo; el resultado se agrupa porque el archivo tiene cientos de interacciones.
- **Desviaciones:** toda diferencia con el Figma (por limitación técnica o porque el Figma no define el caso) va en la sección "Desviaciones" de la spec y la aprueba Zhair antes de programarla.
- **Verificación:** el PR de una pantalla incluye la captura del Figma junto a la de la app en el mismo estado.

### Si el MCP de Figma no está configurado

Antes de implementar, la IA comprueba si tiene herramientas de Figma disponibles. **Si no las tiene, no implementa el diseño**: identifica el entorno y el modelo en que está corriendo y muestra **solo** los pasos de ese entorno, tal como aparecen abajo. Si no puede identificarlo, pregunta cuál es.

Hay dos servidores. Usar el **remoto** salvo que el entorno no permita iniciar sesión con OAuth:

| Servidor | URL | Requisito |
|---|---|---|
| Remoto (recomendado) | `https://mcp.figma.com/mcp` | Cuenta de Figma con acceso al archivo. Las cuentas gratuitas tienen un límite bajo de llamadas al mes. |
| Escritorio | `http://127.0.0.1:3845/mcp` | App de escritorio de Figma abierta, Dev Mode, *Enable desktop MCP server*. Requiere un asiento Dev o Full de un plan pago. |

**Gemini CLI**
1. Ejecutar `gemini mcp add --transport http figma https://mcp.figma.com/mcp` (o agregar en `~/.gemini/settings.json`: `"mcpServers": { "figma": { "httpUrl": "https://mcp.figma.com/mcp" } }`).
2. Abrir `gemini` y ejecutar `/mcp auth figma`; autorizar en el navegador con la cuenta de Figma.
3. Comprobar con `/mcp` que `figma` aparece conectado con sus herramientas.

**Gemini en Android Studio**
1. *Settings → Tools → AI → MCP Servers* → activar *Enable MCP Servers*.
2. En el `mcp.json` que abre, agregar `"mcpServers": { "figma": { "httpUrl": "https://mcp.figma.com/mcp" } }`. Si la versión no permite iniciar sesión, usar el servidor de escritorio: `"httpUrl": "http://127.0.0.1:3845/mcp"` con la app de Figma abierta.
3. Aplicar y comprobar en el chat del agente (modo *Agent*) que aparecen las herramientas de Figma.

**Claude Code**
1. Ejecutar `claude mcp add --transport http figma https://mcp.figma.com/mcp`.
2. En la sesión, ejecutar `/mcp`, elegir `figma` → *Authenticate* y autorizar en el navegador.

**VS Code (GitHub Copilot, modo Agent)**
1. Crear `.vscode/mcp.json` con `{ "servers": { "figma": { "type": "http", "url": "https://mcp.figma.com/mcp" } } }`.
2. Pulsar *Start* sobre el servidor en ese archivo y autorizar en el navegador.
3. En el chat, modo *Agent* → icono de herramientas → comprobar que figuran las de Figma.

**Cursor**
1. *Settings → MCP → Add new MCP server*, o en `~/.cursor/mcp.json`: `{ "mcpServers": { "figma": { "url": "https://mcp.figma.com/mcp" } } }`.
2. Pulsar *Connect* / *Login* junto a `figma` y autorizar en el navegador.

**Otro entorno:** buscar en su configuración la sección "MCP servers", agregar un servidor HTTP con la URL remota y autenticarse. Si el entorno no admite MCP, decirlo y pedir al usuario que exporte el frame (captura + medidas de Dev Mode) mientras tanto.

Después de configurarlo, reiniciar la sesión del agente y pedirle que lea un frame del archivo para confirmar la conexión.

## Flujo de trabajo en GitHub (ramas y pull requests)

Repositorio `ZhairG/Smartfood`, rama principal `master`. Todo cambio llega a `master` por pull request. GitHub no permite proteger `master` en este plan, así que estas reglas no las impone la plataforma: se cumplen por acuerdo del equipo.

### 0. Issues, asignación y plan

- Zhair es el planeador: escribe o aprueba la spec, divide su `tasks.md` en **Issues por feature completa** (domain, data, estructura y ui juntos, no un Issue por capa) (etiquetas `spec-NNN`, capa y dificultad `inicial`/`intermedia`/`avanzada`, dentro del milestone de la spec) y los asigna. Nadie empieza un Issue que no tenga asignado.
- El tablero de GitHub Projects sigue el orden *Por hacer → Plan en revisión → En progreso → PR en revisión → Hecho*. Cada persona tiene **un solo Issue en progreso** a la vez.
- Antes de programar, el plan del modo plan (preguntas respondidas incluidas) se publica **como comentario en el Issue** y el Issue pasa a *Plan en revisión*. Natalia puede revisar primero los planes de dificultad `inicial` o `intermedia`; **la aprobación final es de Zhair**, en el mismo Issue. Sin esa aprobación no se escribe código.
- Los archivos compartidos (`libs.versions.toml`, `app/build.gradle.kts`, `CoreDataModule`, `AppNavHost`, `strings.xml`, `firestore.rules`) solo los modifica el Issue que los menciona. Si un plan necesita tocarlos y su Issue no los menciona, se pregunta en el Issue.

### 1. Ramas

- Siempre se parte de `master` actualizado: `git switch master && git pull`, luego `git switch -c <rama>`.
- **Una rama por módulo** cuando la spec lo indica en su "Plan de entrega" (`feature/008-perfil`, `feature/009-recetas-ia`): todos los Issues del módulo trabajan en esa rama compartida y el módulo llega a `master` en un solo PR. Si la spec no lo indica, **una rama por Issue**: `feature/NNN-<issue>-<tema>` (`feature/006-42-busqueda`). Para lo demás, `fix/<tema>`, `docs/<tema>` o `refactor/<tema>`.
- **Nunca** se trabaja ni se hace push directo a `master`.
- **Ramas compartidas (varias personas en la misma rama):**
  - Antes de empezar y antes de cada `git push`: `git pull` en la rama del módulo.
  - Si `git push` se rechaza ("rejected", "non-fast-forward", "fetch first"), es porque otra persona subió commits: hacer `git pull`, resolver los conflictos si aparecen, volver a correr las pruebas y repetir el `git push`.
  - **Prohibido** `git push --force`, `--force-with-lease`, `git reset --hard`, `git rebase` sobre commits ya subidos y borrar la rama compartida: borran el trabajo de los demás. Si un agente propone cualquiera de estos comandos para "arreglar" un rechazo, se detiene y se pregunta en el Issue.
  - Si un conflicto toca código de otra persona y no está claro qué conservar, se pregunta en el Issue antes de resolverlo.

### 2. Commits

- Convencionales y en español: `feat`, `fix`, `test`, `docs`, `refactor` o `chore`, con ámbito y la spec o el CA que cubren. Ejemplos: `feat(inventario): agregar producto con fecha de vencimiento (spec 006, CA-INV-01-01)` y `fix(ui): … (spec 002, CA-UI-07-08)`.
- **Un commit por tarea (`Tnnn`):** cada tarea de `tasks.md` va en su propio commit, con su código y sus pruebas, y el mensaje cita la tarea y su CA (por ejemplo, `feat(perfil): pantalla Editar información (spec 008, T007, CA-PER-02-01)`). No se hacen commits por archivo ni de "avance" sin terminar la tarea. Las correcciones de una tarea se integran en su commit mientras no se haya hecho push; después, van en commits nuevos. Antes de confirmar, revisar `git status` y `git diff --cached`. Nunca se suben `app/google-services.json`, keystores ni otros secretos (RNF-01).

### 3. Antes de abrir el PR (todo obligatorio)

1. `./gradlew :app:testDebugUnitTest` en verde (y `npm --prefix functions test` si cambió `functions/`, y las pruebas de `rules-test/` con los emuladores si cambiaron `firestore.rules` o `storage.rules`).
2. `./gradlew :app:assembleDebug` compila y `./gradlew :app:lintDebug` no agrega errores nuevos.
3. Si el cambio toca `ui/`, `core/designsystem` o `core/navigation`: las pruebas instrumentadas **de las pantallas y componentes que toca** (y las de navegación si cambia la navegación) en verde en emulador o dispositivo, con `-Pandroid.testInstrumentationRunnerArguments.class=…`. Las pruebas unitarias no dibujan Compose, así que un componente que se cae al mostrarse pasa el punto 1 sin fallar (Issue #60). La suite completa (`connectedDebugAndroidTest`) se corre en el Issue de cierre de cada spec; el PR dice qué clases se corrieron. `AppNavigationTest` y `StatusBarAdaptabilityTest` necesitan el dispositivo sin sesión iniciada.
4. `spec.md` y `tasks.md` de la feature reflejan el cambio, y la matriz CA → test está al día.
5. La app se probó en emulador o dispositivo.

### 4. El pull request

- Base `master`. El título sigue el formato de los commits. **Pocos PR:** se agrupan en uno los Issues relacionados cuando Zhair lo acepta. La descripción empieza con un `Closes #<issue>` por cada Issue, para que se cierren al fusionar.
- La descripción incluye:
  - **Resumen:** qué cambia y por qué.
  - **Trazabilidad:** spec, HU y CA cubiertos.
  - **Pruebas:** comandos ejecutados y su resultado, y dispositivo o emulador usado.
  - **Capturas:** antes y después, si cambia la UI.
- Si el PR queda incompleto, se abre como *draft*.

### 5. Revisión y fusión

- **Solo Zhair aprueba y fusiona** los PR hacia `master`, siempre con **Merge commit**, que conserva cada commit con su CA. No se usa squash ni rebase.
- Si la revisión pide cambios, se hacen commits nuevos en la misma rama y se hace push. No se reescribe el historial publicado.

### 6. Después de fusionar

`git switch master && git pull`, y borrar la rama local con `git branch -d <rama>`. Si la rama ya se fusionó, el trabajo siguiente va en una rama nueva.

### Reglas para la IA

- Al empezar, pide el número del Issue y lo lee (`gh issue view <n>` o el enlace). Trabaja solo dentro de su alcance y, al terminar el modo plan, entrega el plan listo para pegarlo como comentario en el Issue.
- Crea ramas, commits, push y PR **solo cuando el integrante lo pide**. Antes muestra qué archivos va a incluir.
- Nunca fusiona PR, nunca hace push a `master` y nunca usa `--force`, `--no-verify`, `reset --hard` ni comandos que descarten cambios ajenos.
- Solo incluye en el commit los cambios de la tarea. Lo que el integrante tenga sin confirmar (otros archivos, `.idea/`) se queda fuera, salvo que pida lo contrario.
- Si alguno de los puntos del paso 3 falla, lo informa con la salida y no abre el PR hasta resolverlo, o lo abre como *draft* si el integrante así lo pide.

### Checklist del Issue

Cada agente mantiene al día el checklist del Issue en el que trabaja, sin que nadie se lo pida. En cuanto se cumple un paso, cambia `- [ ]` por `- [x]` **solo en la línea de ese paso** y no toca el resto del texto.

- **Cómo:** `gh issue view <n> --json body --jq .body > <archivo temporal>`, editar solo la casilla y `gh issue edit <n> --body-file <archivo temporal>`. Siempre con `--body-file`, para evitar problemas de comillas en PowerShell y Git Bash.
- **El archivo temporal va en UTF-8.** En Git Bash o PowerShell 7 (`pwsh`) sirve `>`. En **Windows PowerShell 5.1** no se usa `>`, porque guarda con otra codificación y daña "§", "→" y las tildes (pasó en el #32 y el #33): se usa `gh issue view <n> --json body --jq .body | Out-File -Encoding utf8 <archivo temporal>`. Lo mismo vale para los comentarios y PR que se publican con `--body-file`.
- **Comprobar después de editar:** volver a leer el cuerpo y confirmar que no aparecen secuencias como "┬", "Ô" o "├". Si aparecen, se corrigen antes de seguir.
- **Cuándo se marca cada casilla:**

| Casilla | Se marca cuando… |
|---|---|
| Asignado por Zhair | el Issue tiene a la persona asignada (`gh issue view`). |
| Plan del modo plan publicado | el agente publicó el plan como comentario en el Issue. |
| Plan aprobado por Zhair | Zhair aprueba en el Issue o lo dice explícitamente en la sesión. Nunca se da por supuesto. |
| Cada tarea `Tnnn` | la tarea quedó hecha y confirmada en la rama del Issue. |
| Pruebas unitarias en verde… | `testDebugUnitTest`, `assembleDebug` y `lintDebug` pasaron, y las instrumentadas específicas si el cambio toca la UI (la suite completa en el cierre de la spec; puntos 1 a 3 del paso 3). |
| `spec.md` y `tasks.md` al día | la tarea está `[x]` en `tasks.md` y la matriz CA → test está actualizada. |
| Probado en emulador o dispositivo | **el integrante confirma** que probó el flujo. El agente puede instalar el APK (`./gradlew :app:installDebug`) y revisar que la app abre sin errores en Logcat, pero eso solo no basta. |
| PR hacia `master` con `Closes #n` | el PR quedó creado. Si es *draft*, también se marca, y en el PR se indica qué falta. |

- **Nunca** se marca una casilla cuyo paso no se cumplió. Si un paso falla, la casilla queda vacía y el agente lo informa con la salida.
- Al terminar, el agente dice qué casillas marcó, cuáles quedan pendientes y por qué.

## Issues y planes para modelos más débiles

Todos los integrantes, salvo Zhair, trabajan con **OpenCode y DeepSeek v4.1 flash (gratis)**. Las specs, los planes y los Issues se escriben para que ese modelo los siga sin adivinar:

- **Archivos exactos:** cada tarea dice qué archivos crea o cambia, con su ruta completa, y cuáles **no** debe tocar.
- **Pasos numerados y pequeños**, en el orden de las capas: `domain` → `data` → DI → navegación → `ui` → pruebas.
- **Código que se reutiliza:** se nombra el componente o la clase existente que sirve de modelo (por ejemplo, "copiar el patrón de `ProductEditorViewModel`") en lugar de describirlo.
- **Contratos escritos:** firmas de interfaces, modelos y casos de uso en el plan, para no inventarlas.
- **Comandos de verificación** al final de cada Issue (unitarias, compilación, instrumentadas específicas) y qué resultado esperar.
- **Criterios citados:** cada paso dice qué CA cumple; si algo no está en la spec, el agente se detiene y pregunta en el Issue.
- Ante una duda de diseño o de alcance, el agente **no decide solo**: pregunta a Zhair de forma clara y breve.

## Recomendaciones de Zhair

Zhair (planeador) da recomendaciones durante el trabajo. **Toda recomendación nueva se agrega a esta lista**, sin que nadie lo pida, y las specs nuevas las aplican.

| Fecha | Recomendación | Cómo se aplica |
|---|---|---|
| 2026-10-17 | Terminar cada Issue más rápido sin saltarse pasos. | Una compilación por ronda (unitarias, assemble, androidTest y lint juntos) y las preguntas del modo plan en una sola ronda. |
| 2026-10-17 | Pruebas instrumentadas específicas. | En cada Issue solo las clases de las pantallas y componentes que toca, más las de navegación si cambia; la suite completa, en el cierre de la spec. |
| 2026-10-18 | Pocos PR, porque el profesor ve demasiados cambios en GitHub. | Ver §4 PR: varios Issues por PR (un PR por módulo). |
| 2026-10-18 | Asociar en un solo Issue el domain, data, estructura y ui de cada feature. | Issues por feature vertical (§0). |
| 2026-10-18 | Un commit por tarea (`Tnnn`), no uno por Issue. | Ver §2 Commits; reemplaza el "un commit por feature" de la fila anterior. |
| 2026-10-18 | La app debe ser 100 % fiel al Figma: íconos, animaciones, colores y componentes. | Ver "Fidelidad total con Figma" y la constitución §1. |
| 2026-10-18 | Escribir los Issues y planes para que los siga DeepSeek (OpenCode) del resto del equipo. | Ver "Issues y planes para modelos más débiles". |
| 2026-10-18 | Guardar las capturas de Figma por módulo en la raíz del proyecto. | `disenos-figma/Modulo N - Nombre/`, con `README.md` de índice (pantalla, `node-id`). |
| 2026-10-18 | Que nadie borre trabajo ajeno en las ramas compartidas del módulo. | Ver §1 "Ramas compartidas": `git pull` ante un rechazo y nunca `--force`. |
| 2026-10-18 | Medir la fluidez de las animaciones en el build release. | En debug, Compose pierde ~300 ms al iniciar cada pantalla; las animaciones se juzgan con el APK release en el teléfono (medición del 18/10/2026). |

## Comandos

En Windows usar `gradlew.bat` (o `./gradlew` desde Git Bash).

```bash
./gradlew :app:assembleDebug                 # Compilar APK debug
./gradlew :app:testDebugUnitTest             # Pruebas unitarias (JVM)
./gradlew :app:testDebugUnitTest --tests "com.smartfood.app.feature.auth.ui.login.LoginViewModelTest"   # Una clase
./gradlew :app:testDebugUnitTest --tests "*LoginViewModelTest.CA-02-03*"                                # Un test por su CA
./gradlew :app:connectedDebugAndroidTest     # Pruebas instrumentadas (requiere emulador/dispositivo)
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartfood.app.feature.auth.ui.login.LoginScreenTest
./gradlew :app:lintDebug                     # Android Lint (no hay ktlint/detekt configurados)
firebase deploy --only firestore:rules       # Publicar firestore.rules
npm --prefix functions test                  # Compilar y probar las Cloud Functions (node:test)
firebase emulators:exec --only firestore,storage "npm --prefix rules-test test"   # Probar firestore.rules y storage.rules (antes: npm --prefix rules-test ci)
firebase emulators:start --only auth,firestore,functions   # Emulator Suite; la app con -PuseFirebaseEmulator=true
firebase deploy --only functions             # Desplegar las funciones (requiere los secretos SMTP_USER y SMTP_PASS)
```

El build `release` usa R8 en modo completo; los cambios con reflexión/serialización deben revisarse contra `app/proguard-rules.pro`.

## Arquitectura

Paquetes **por feature** (`feature/<nombre>/{domain,data,ui}`) más `core/` compartido. Dependencias: `ui → domain ← data`.

- **`domain`** es Kotlin puro: modelos (`User`, `AuthError` sealed, `AuthField`) e **interfaces** de repositorio (`AuthRepository`, `UserRepository`, `PasswordRecoveryRepository`, `SessionPreferences`). Las validaciones de negocio viven **solo** en los UseCases; los ViewModels no las repiten.
- **Errores tipados:** todo devuelve `Result<T>`; un fallo lleva `AuthException(AuthError)` y se lee con `Throwable.toAuthError()`. `data/mapper/FirebaseErrorMapper` es el único lugar que conoce excepciones de Firebase. `ui/AuthErrorText.kt` es el único lugar que traduce `AuthError` a `strings.xml`. Para agregar un error: nuevo caso en `AuthError` → mapper (si viene de Firebase) → `AuthErrorText` → `strings.xml`.
- **`data`**: `FirebaseAuthRepository`, `FirestoreUserRepository` (perfil en `users/{uid}`, escrito con `Map` para no depender de reflexión) y `DataStoreSessionPreferences` ("Recuérdame"). Usar `safeRunCatching` (`core/common`) + `.mapFirebaseError()`, nunca `runCatching` (se traga `CancellationException`).
- **Recuperación de contraseña real (spec 003):** `data/repository/RemotePasswordRecoveryRepository` llama a 3 funciones callable de `functions/` (TypeScript, región `southamerica-east1`): `requestPasswordResetCode`, `verifyPasswordResetCode` y `resetPasswordWithCode`.
  - Los datos viajan como `Map`. Los errores llegan con `details.reason` y `FirebaseErrorMapper.mapFunctionsError` los clasifica.
  - Responde igual exista o no la cuenta: no hay error de "cuenta no encontrada" en la recuperación.
  - En el servidor, la lógica vive en `functions/src/recoveryService.ts`, sin Firebase y probada con fakes. Firebase y Gmail solo aparecen en `adapters.ts`. Los secretos `SMTP_USER` y `SMTP_PASS` están en Secret Manager.
  - La colección `passwordResets` es solo del servidor; las reglas niegan todo acceso a los clientes.
  - **Modo demo:** con `-PrecoveryDemo=true` (solo debug), `AuthDataModule` enlaza `data/demo/DemoPasswordRecoveryRepository` (correos `demo@smartfood.com` y `prueba@smartfood.com`; código `1234`).
  - Con `-PuseFirebaseEmulator=true` (solo debug), `CoreDataModule` conecta Auth, Firestore y Functions a la Emulator Suite. En el emulador, el código se lee en el log.
- **Orquestación del registro** en `RegisterUseCase`: crear cuenta → perfil Firestore (si falla, rollback con `deleteCurrentAccount`) → `logout`. La app **no** pide el correo de verificación de Firebase.
- **Correo de bienvenida (spec 004):** la función `sendWelcomeEmail` (`onDocumentCreated("users/{uid}")`, sin reintentos) envía un único correo con el nombre y el enlace de verificación (`generateEmailVerificationLink`).
  - La lógica vive en `functions/src/registrationService.ts` (`WelcomeService`, probado con fakes) y las plantillas en `emailTemplates.ts` (puras, con `escapeHtml`).
  - El destinatario sale siempre de Auth, nunca del perfil. `welcomeEmails/{uid}` (solo servidor) garantiza un correo por cuenta; si el envío falla, se libera.
  - En el emulador, el enlace se lee en el log (`[EMULADOR] Correo de bienvenida…`).
- **UI (MVVM + UDF):** un ViewModel por pantalla. Cada pantalla tiene un `XContract.kt` con `XUiState` (los campos en rojo se **derivan** de `error`), `XEvent` y `XEffect`. Los efectos de un solo uso (navegación) salen por `Channel` → `Flow` y se recolectan con `ObserveEffects`. Patrón Route/Screen: los `*NavGraph.kt` obtienen el ViewModel con `hiltViewModel()` y llaman a la `XScreen` **stateless**, que es la que se prueba.
- **Documentos legales (spec 005, `feature/legal`):** `LegalDocumentScreen` (stateless, sin ViewModel) muestra los Términos o la Política según `LegalDocumentType`.
  - El texto vive en `res/values/strings_legal.xml` y `ui/LegalContent.kt` es el único mapeo tipo → recursos. Hoy es una plantilla: al publicar el texto definitivo se cambian `LegalVersion.CURRENT` e `IS_PREVIEW`.
  - Las rutas `TermsOfService` y `PrivacyPolicy` se registran en la raíz de `AppNavHost` con `legalDestinations()`.
  - `SmartFoodTermsCheckbox` resuelve el toque por carácter (enlace → abre el documento; resto → alterna) y ofrece los enlaces como `customActions` para TalkBack.
  - Constancia: `UserRepository.createProfile(user, acceptedTermsVersion)` guarda `termsVersion` y `termsAcceptedAt` (hora del servidor); las reglas lo exigen al crear `users/{uid}`.
- **Inventario (spec 006, `feature/inventory`):** lista por estado de vencimiento, búsqueda por texto y voz (`SpeechToText` sobre `SpeechRecognizer`), editor con foto (cámara o galería) y aviso diario (`ExpiryReminderWorker`).
  - **Uso sin conexión (§13):** las escrituras van a la cola de Firestore sin transacciones y esperan al servidor como mucho 3 s; devuelven `WriteOutcome` (`SYNCED` o `PENDING`). `observeItems()` marca `isPendingSync` con los metadatos.
  - **Fotos:** los casos de uso no las suben: las encolan con `PhotoSync` (`data/sync/WorkManagerPhotoSync`). `PendingPhotoStore` las guarda en `filesDir/pending/{itemId}/{uuid}.jpg` y `PhotoUploadWorker`/`PhotoDeleteWorker` (`@HiltWorker`) las suben o borran con red y reintentos. En el editor, una foto que espera subirse es `PhotoUiState.Pending`: se ve desde el teléfono, se conserva al guardar y nunca se borra al salir.
  - **Resultados del editor:** se muestran sobre la lista con `EditorResultHost` (`savedStateHandle` de la pantalla anterior), incluido "Guardado en tu teléfono" (`EditorResult.SavedOnPhone`).
  - Textos en `res/values/strings_inventory.xml`; reglas en `firestore.rules` (`users/{uid}/inventory`) y `storage.rules`, probadas en `rules-test/`.
- **Recetas + IA (spec 009, `feature/recipes`):** catálogo propio, agente que arma un plan de 3 días, flujo de la IA y cocina guiada.
  - **Catálogo:** `assets/recetas.json` (50 recetas) e `ingredientes.json` (con `"etiquetas"` para las dietas, empaque y precio por tienda), leídos por `AssetsRecipeCatalog`. Fotos `drawable-nodpi/receta_*.webp` con su fuente en `docs/licenses/fotos-recetas.md`; `res/raw/keep.xml` evita que el build release las quite. Los precios se actualizan con `tools/precios/` (ver su `README.md`); la fuente de verdad es `docs/precios/precios.json`.
  - **Agente (`domain/agent`):** `MealPlanner` (CSP con backtracking, *forward checking* y ramificación y poda), `RecipeEvaluator` (compras por empaques completos, básicos de despensa sin costo), `PlanExplainer` (razones de "¿Por qué te conviene?") e `IngredientMatcher` (inventario ↔ ingredientes). **No se modifican sin revisar `MealPlannerTest` contra fuerza bruta.** La evaluación del Avance 2 está en `docs/ia/evaluacion-agente.md` (`AgentEvaluationTest`, `MealPlannerPerformanceTest`, `RealCatalog`).
  - **Flujo de la IA (`ui/ai`):** una sola ruta (`Route.AiPlan`) con `AiPlanViewModel` y `AiUiState.step` (`Context`, `Loading`, `Plan`, `NoPlan`), más `Route.AiHelp`. `GetAiContextUseCase` arma el contexto (inventario emparejado, restricciones del Perfil como `DietRule`, tienda) y `GeneratePlanUseCase` corre el agente en `Dispatchers.Default`. `AiReasonText.kt` es el único lugar que convierte razones y sugerencias en texto.
  - **Cocina guiada (`ui/cooking`):** "Antes de empezar" y los pasos con temporizador; `TimerAlarm` repite `res/raw/timer_alarm.ogg` (generado con `tools/timer_sound.py`, fuente en `docs/licenses/sonidos.md`) con volumen de alarma. `FinishCookingUseCase` descuenta el inventario y guarda `users/{uid}/cookings` (espera al servidor 3 s, como el inventario); `GetMonthlySavingsUseCase` alimenta "Ahorrados este mes" del Perfil.
  - Botones de recetas: `RecipeActionButton` (naranja) y `OutlineActionButton` (blanco con borde). Textos en `res/values/strings_recipes.xml`; desviaciones aprobadas del Figma en la spec §9.
- **Navegación (`core/navigation`):** `AppNavHost` arranca en `Splash`; `SplashViewModel` usa `ResolveSessionUseCase` para ir a `MainGraph` (Home) o a `AuthGraph` (Login). `authGraph()` y `mainGraph()` son extensiones de `NavGraphBuilder` en cada feature. Las transiciones push de 600 ms se declaran una sola vez (`NavTransitions`). `navigateToMainGraph()` y `navigateToLoginClearingBackStack()` limpian toda la pila.
- **DI:** `core/di/CoreDataModule` provee `FirebaseAuth`, `FirebaseFirestore`, `FirebaseFunctions` y el `DataStore`, siempre **no nulos**; `feature/auth/data/di/AuthDataModule` enlaza las interfaces.
- **Design system (`core/designsystem`):** `theme/` (tokens), `components/` (prefijo `SmartFood`) y `util/` (modificadores, `SystemBarEffect`, `ClearFocusOnKeyboardDismiss`).
- **Adaptabilidad (spec 002):** las medidas de los formularios salen de `theme/FormMetrics.kt`.
  - `AuthScreenScaffold` toma el perfil inicial (`Compact`, `Medium` o `Regular`) con `formMetricsForWindow(maxHeight)`, que usa el alto útil sin barras.
  - Si con el teclado cerrado el formulario no cabe durante 400 ms, baja al perfil siguiente con `smaller()`. Los desbordes pasajeros, como el fundido entre pasos, no cuentan. En las pruebas, avanzar `mainClock` antes de comprobar el perfil. Lo provee con `LocalFormMetrics`, y los componentes leen `LocalFormMetrics.current`. No se usan medidas fijas nuevas.
  - El alto sobrante se reparte con `FlexibleGap(minHeight, weight)`. Dentro de un `AnimatedContent`, el contenido usa `Modifier.fillFormViewport(LocalFormViewport.current)`.
- **Modo de color (spec 002, HU-UI-05):**
  - El modo se decide **solo** con `ThemeMode` (`theme/ThemeMode.kt`, por defecto `LIGHT`): `SmartfoodTheme(themeMode)` provee `LocalIsDarkTheme`.
  - Los componentes leen `LocalIsDarkTheme.current`; **prohibido `isSystemInDarkTheme()` fuera de `ThemeMode.kt`**.
  - La paleta y los esquemas claro y oscuro no se tocan.
  - El tema XML es claro (`Base.Theme.Smartfood`, ventana `GreenLight`). `values-v31` hace que la pantalla de arranque de Android 12+ sea igual a la Splash (`splash_icon`).
- **Fidelidad con Figma (spec 002, HU-UI-07):** referencia en los frames "_Claro" del archivo Figma `z1AoQvluQcmx98gjP4vHF6`.
  - Tipografía Poppins empaquetada en `res/font` (licencia en `docs/licenses/OFL-Poppins.txt`).
  - Para ajustar un color a Figma se cambia el **rol** en `LightColorScheme` (`Theme.kt`), nunca el valor de un token. Los únicos tokens agregados son `GreenCheck`, `GreenSoft` (`#DCFCE7`, círculo de las iniciales del Perfil, spec 008; aprobado por Zhair el 2026-10-18) y `YellowSoft` (`#FFF7D6`, tarjeta del temporizador de la cocina guiada, spec 009; aprobado por Zhair el 2026-10-21).
- **Ícono (spec 002, HU-UI-06):** los `mipmap-*/ic_launcher*` y `ic_launcher-playstore.png` los genera `tools/generate_launcher_icon.py` desde `drawable-nodpi/logo_smartfood_circle.png`. Nunca se editan a mano; si cambia el logo, se vuelve a ejecutar el script.
- **Foco y teclado (spec 002):**
  - **Prohibido el scroll programático** (`scrollTo`, `animateScrollTo`) en las pantallas. El panel aplica el inset del teclado fuera del `verticalScroll` y Compose mantiene visible el campo enfocado.
  - El encabezado de marca se colapsa leyendo `WindowInsets.ime` en la fase de layout.
  - La etiqueta de `SmartFoodTextField` está dentro del `decorationBox`: tocarla enfoca el campo y forma parte de su semántica. En las pruebas, `onNodeWithText("<etiqueta>")` devuelve el campo editable.
  - Elementos flotantes: `AnchoredPopup` con `AbovePopupPositionProvider`, sin offsets fijos. El popup va **siempre encima** del campo; si no cabe, se ajusta a la barra de estado, nunca debajo. El checklist de contraseña es `PasswordFieldWithChecklist`.

## Pruebas

- **Unitarias:** JUnit4 + `kotlinx-coroutines-test`, sin librería de mocks. Los fakes compartidos están en `app/src/sharedTest/.../testutil/`, que Gradle agrega a las unitarias y a las instrumentadas (`FakeAuthRepository`, `FakeUserRepository`, `FakeSessionPreferences`, `FakePasswordRecoveryRepository`, `Result.authError`). `MainDispatcherRule` sigue en `app/src/test/.../testutil/`. Para probar efectos: `viewModel.effects.first()` dentro de `runTest`.
- Los fakes del inventario están en `testutil/InventoryFakes.kt` (`FakeInventoryRepository` con `outcome`, `gate` y `pendingWrites`, `FakePhotoSync`, `FakePhotoRepository`, `FakeSpeechToText`…).
- En las instrumentadas, `androidTest/.../testutil/PantallaEnFlujo` monta una pantalla sin estado una vez y cambia su `UiState` con `paso()`; `ComposeMatchers.kt` tiene `conTipo` y `mostrado`.
- `tools/ca_coverage.py --check tools/ca_coverage_baseline.txt` comprueba que ningún CA se quede sin prueba al reorganizarlas.
- `CustomTestRunner` inicializa un WorkManager de prueba (`SynchronousExecutor`): ninguna prueba instrumentada encola trabajos en el WorkManager real del dispositivo.
- `unitTests.isReturnDefaultValues = true` está activo porque los constructores de las excepciones de Firebase usan `TextUtils`.
- **Instrumentadas:** usan `@HiltAndroidTest`, `CustomTestRunner` (`HiltTestApplication`) y `createAndroidComposeRule<HiltTestActivity>()` (`HiltTestActivity` vive en `app/src/debug`). Las pruebas de pantalla usan la `XScreen` stateless con un `XUiState` construido a mano.

## Convenciones

- Código, KDoc, comentarios, commits y nombres de test en español.
- Cada archivo Kotlin abre con la cabecera KDoc que exige el curso: `Nombre`, `Autor: SmartFood Team`, `Fecha`, `Descripción` (y `@param` cuando aplica).
- Ningún secreto en el código ni en el APK (RNF-01).

## Configuración externa

Firebase (`smartfood-f62a1`, plan Blaze): Authentication con correo/contraseña, Cloud Firestore (`southamerica-east1`) y Cloud Functions. Reglas en `firestore.rules`: cada usuario solo accede a `users/{su uid}` (al crearlo debe incluir la constancia de la spec 005), y `passwordResets` y `welcomeEmails` están cerrados a los clientes. El inventario vive en `users/{uid}/inventory` (solo el dueño, con los datos validados) y sus fotos en Storage (`storage.rules`: JPEG de menos de 2 MB, sin sobrescribir); el bucket de Storage se crea en `southamerica-east1` antes de desplegar `storage.rules`. Las pruebas de ambas reglas están en `rules-test/`. Los secretos de Gmail se configuran con `firebase functions:secrets:set` (ver README).

La API key de Android tiene restricción por paquete + SHA-1. Cada computador necesita el `google-services.json` vigente (no se versiona) y su SHA-1 de debug registrada en Google Cloud Console. Si en Logcat aparece `API key expired` o `Requests from this Android client application com.smartfood.app are blocked`, la causa es esa configuración, no el código (ver "Primero: configurar tu equipo" en el README).
