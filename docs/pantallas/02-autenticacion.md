# 02 · Autenticación

Pantallas: [Iniciar sesión](#loginscreen), [Registro](#registerscreen) y [Recuperar contraseña](#passwordrecoveryscreen).
Índice general y mapa de conceptos: [README](README.md). Los conceptos que ya se explicaron en
[01 · Arranque y legal](01-arranque-y-legal.md) se enlazan allá (por ejemplo [`Modifier`](01-arranque-y-legal.md#c-modifier)
o [`remember`](01-arranque-y-legal.md#c-remember)).

Las tres pantallas usan [`AuthScreenScaffold.kt`](../../app/src/main/java/com/smartfood/app/feature/auth/ui/components/AuthScreenScaffold.kt), el armazón común de autenticación. Recibe el encabezado
(`headerContent`) y el formulario (`formContent`) como slots, muestra el diálogo "sin conexión" cuando `isOffline` es
`true`, encoge el logo cuando se abre el teclado y elige el perfil de medidas `Compact`, `Medium` o `Regular`
(`LocalFormMetrics`) según el alto del teléfono.

---

## LoginScreen

Correo, contraseña, "Recuérdame", "¿Olvidaste tu contraseña?" y "Regístrate". Spec 001, HU-02; Figma `585:4420`.

**Qué recibe**

| Parámetro | Para qué |
|---|---|
| `state: LoginUiState` | Lee `email`, `password`, `rememberMe`, `isLoading`, `isOffline`, `emailHasError`, `passwordHasError` y `error` |
| `onEvent: (LoginEvent) -> Unit` | Avisa lo que hace la persona: `EmailChanged`, `PasswordChanged`, `RememberMeChanged`, `LoginClicked`, `RetryConnectionClicked` |
| `onNavigateToRegister`, `onNavigateToForgotPassword` | Navegación directa, sin lógica |

### Código comentado

Archivo: [LoginScreen.kt, líneas 58–93](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L58-L93)

```kotlin
fun LoginScreen(
    state: LoginUiState,                                      // [U4] estado inmutable que llega de afuera (UDF: el estado baja)
    onEvent: (LoginEvent) -> Unit,                            // [U4] un solo canal para los eventos (UDF: los eventos suben)
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenScaffold(                                       // armazón común de autenticación
        isOffline = state.isOffline,                          // muestra el diálogo "sin conexión" si el error fue de red
        isReconnecting = state.isLoading,                     // el diálogo dice "Conectando…" mientras reintenta
        onRetryConnection = remember(onEvent) { { onEvent(LoginEvent.RetryConnectionClicked) } }, // [U2] lambda recordada
        headerContent = {                                     // [U3] slot del encabezado
            LoginWelcomeHeader()
            Spacer(Modifier.height(LocalFormMetrics.current.sectionSpacing)) // [U1] espacio vacío; medida del perfil
        },
        modifier = modifier
    ) {                                                       // [U3] lambda final = slot del formulario (formContent)
        LoginFormFields(state = state, onEvent = onEvent)     // [U1] la pantalla se arma con composables pequeños
        Spacer(Modifier.height(LocalFormMetrics.current.sectionSpacing))

        LoginOptionsRow(
            state = state,
            onEvent = onEvent,
            onNavigateToForgotPassword = onNavigateToForgotPassword
        )
        // Con alto de sobra, el botón y el enlace bajan como en Figma (nodo 585:4420, CA-UI-07-03).
        FlexibleGap(minHeight = LocalFormMetrics.current.sectionSpacing)  // espacio mínimo + reparte el alto que sobra

        LoginActionButtons(
            isLoading = state.isLoading,
            onLoginClicked = remember(onEvent) { { onEvent(LoginEvent.LoginClicked) } },
            onNavigateToRegister = onNavigateToRegister
        )
        Spacer(Modifier.weight(2f))                           // [U1] weight: toma 2 partes del alto sobrante
    }
}
```

Archivo: [LoginScreen.kt, líneas 119–165](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L119-L165)

```kotlin
private fun LoginFormFields(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current              // [U2] CompositionLocal: controla el foco y el teclado

    Column(modifier = modifier) {                             // [U1] apila los campos hacia abajo
        SmartFoodTextField(                                   // campo de texto propio, basado en BasicTextField
            label = stringResource(id = R.string.login_email_label),
            contentType = EmailAccountContentType,            // le dice al autocompletado que es un correo de cuenta
            value = state.email,                              // [U2] muestra lo que dice el estado (elevación de estado)
            onValueChange = remember(onEvent) { { email -> onEvent(LoginEvent.EmailChanged(email)) } }, // [U2] cada letra → evento
            placeholder = stringResource(id = R.string.login_email_placeholder),
            enabled = !state.isLoading,                       // [U2] bloqueado mientras inicia sesión
            isError = state.emailHasError,                    // [U2] rojo según el estado
            keyboardOptions = KeyboardOptions(                // [U2] cómo es el teclado
                capitalization = KeyboardCapitalization.None, // sin mayúscula inicial
                autoCorrectEnabled = false,                   // sin autocorrector
                keyboardType = KeyboardType.Email,            // teclado con @ y .com
                imeAction = ImeAction.Next                    // la tecla de acción dice "Siguiente"
            ),
        )

        Spacer(Modifier.height(LocalFormMetrics.current.fieldSpacing))

        SmartFoodPasswordField(                               // campo de contraseña con el ojo para mostrarla
            label = stringResource(id = R.string.login_password_label),
            value = state.password,
            onValueChange = remember(onEvent) { { pass -> onEvent(LoginEvent.PasswordChanged(pass)) } },
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            imeAction = ImeAction.Done,                       // la tecla de acción dice "Listo"
            keyboardActions = KeyboardActions(                // [U2] qué pasa al tocar la tecla de acción
                onDone = {
                    focusManager.clearFocus()                 // cierra el teclado
                    onEvent(LoginEvent.LoginClicked)          // e intenta iniciar sesión
                }
            ),
        )

        AuthErrorMessage(                                     // mensaje de error animado debajo de los campos
            message = state.error.inlineError?.let { stringResource(id = it.messageRes()) }, // [U2] ?.let: solo si hay error
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

Archivo: [LoginScreen.kt, líneas 171–194](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L171-L194)

```kotlin
private fun LoginOptionsRow(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(                                                      // [U1] Row: elementos de lado a lado
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,     // [U1] uno a cada extremo
        verticalAlignment = Alignment.CenterVertically,       // [U1] centrados en altura
    ) {
        SmartFoodCheckbox(                                    // casilla "Recuérdame"
            label = stringResource(id = R.string.login_remember_me),
            checked = state.rememberMe,                       // [U2] marcada según el estado
            onCheckedChange = remember(onEvent) { { checked -> onEvent(LoginEvent.RememberMeChanged(checked)) } }
        )

        SmartFoodTextLink(                                    // texto que se puede tocar
            text = stringResource(id = R.string.login_forgot_password),
            textAlign = TextAlign.Center,
            onClick = onNavigateToForgotPassword
        )
    }
}
```

Archivo: [LoginScreen.kt, líneas 200–220](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L200-L220)

```kotlin
private fun ColumnScope.LoginActionButtons(                   // [U3] receptor ColumnScope: solo se llama dentro de un Column
    isLoading: Boolean,
    onLoginClicked: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    SmartFoodPrimaryButton(                                   // [U2] botón naranja de la app
        text = stringResource(id = R.string.login_button_text),
        loadingText = stringResource(id = R.string.login_button_loading), // "Iniciando sesión…" con puntos animados
        onClick = onLoginClicked,                             // [U2] el botón solo avisa; la lógica está en otro lado
        isLoading = isLoading                                 // mientras carga no se puede tocar
    )

    FlexibleGap(minHeight = LocalFormMetrics.current.sectionSpacing) // también es ColumnScope: usa weight por dentro

    SmartFoodPromptLink(                                      // "¿No tienes cuenta? Regístrate"
        prompt = stringResource(id = R.string.login_no_account_prompt),
        action = stringResource(id = R.string.login_register_action),
        onClick = onNavigateToRegister,
        modifier = Modifier.align(Alignment.CenterHorizontally) // [U1] align del ColumnScope: centra solo este hijo
    )
}
```

`// …` Omitidos: `LoginWelcomeHeader` (título y subtítulo, igual al [`LegalSectionBlock`](01-arranque-y-legal.md#legaldocumentscreen))
y el `@Preview`: [LoginScreen.kt, líneas 99–113](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L99-L113).

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-udf"></a>**Pantalla sin estado y flujo unidireccional (UDF)** | U4 · Componentes de la arquitectura | La pantalla **no** recibe el ViewModel: recibe `state` (lo que se ve) y `onEvent` (lo que hace la persona). El estado baja y los eventos suben. Gracias a eso se ve en `@Preview` y se prueba con estados escritos a mano. | [L59](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L59) |
| <a id="c-hoisting"></a>**Elevación de estado (`value` + `onValueChange`)** | U2 · Interactuar con la IU y el estado | El campo **muestra** `state.email` y **avisa** cada cambio. No guarda el texto en un `remember`: así el texto sobrevive al giro del teléfono y la lógica no depende de la interfaz. Al escribir "a": `onValueChange("a")` → evento → estado nuevo → **recomposición** → el campo muestra "a". | [L130](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L130) |
| <a id="c-keyboard"></a>**`KeyboardOptions` y `KeyboardActions`** | U2 · Interactuar con la IU y el estado | `KeyboardOptions` configura el teclado (tipo, mayúsculas, autocorrector, tecla de acción). `KeyboardActions` dice qué hacer cuando se toca esa tecla: aquí, "Listo" cierra el teclado e inicia sesión. Son sugerencias: algunos teclados ignoran el autocorrector. | [L135](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L135) |
| <a id="c-compositionlocal"></a>**`LocalFocusManager` (CompositionLocal)** | U3 · Cómo compilar apps fabulosas | Valor que Compose pone a disposición de todo el árbol sin pasarlo por parámetro. `LocalFocusManager.current` da el control del foco; `clearFocus()` cierra el teclado. `LocalFormMetrics` es otro, propio de la app, con las medidas del formulario. | [L124](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L124) |
| <a id="c-row"></a>**`Row`, `Arrangement.SpaceBetween`, `Alignment`** | U1 · Crea un diseño básico | `Row` pone a los hijos de lado a lado. `SpaceBetween` lleva el primero al inicio y el último al final; `CenterVertically` los alinea por el centro. | [L179](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L179) |
| <a id="c-weight"></a>**`Modifier.weight` y `Spacer`** | U1 · Crea un diseño básico | `Spacer` es espacio vacío. Con `weight(2f)` reparte el alto que sobra en proporción: 2 partes para él y 1 para cada `FlexibleGap`. Así, en un teléfono alto, el botón baja como en el Figma. | [L91](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L91) |
| <a id="c-scope"></a>**Receptor `ColumnScope`** | U3 · Más aspectos básicos de Kotlin | `ColumnScope.LoginActionButtons` es una **función de extensión** sobre el ámbito de un `Column`. Por eso adentro existen `Modifier.align(...)` y `weight`, que solo tienen sentido dentro de una columna. | [L200](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L200) |
| <a id="c-remember-key"></a>**`remember(onEvent) { { … } }`** | U2 · Interactuar con la IU y el estado | Recuerda la lambda mientras `onEvent` sea el mismo, para no crear una nueva en cada recomposición. Con Kotlin 2.0.20 o superior el compilador ya lo hace solo (*strong skipping*), así que hoy es una optimización redundante. | [L68](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L68) |
| <a id="c-letnull"></a>**`?.let`** | U2 · Aspectos básicos de Kotlin | Ejecuta el bloque solo si el valor no es `null`. `state.error.inlineError?.let { … }` convierte el error en texto si hay uno; si no, queda `null` y no se muestra nada. | [L161](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L161) |
| <a id="c-button"></a>**Botones (`onClick`, `enabled`)** | U2 · Cómo agregar un botón a una app | Un botón solo **avisa** con `onClick`. `SmartFoodPrimaryButton` se desactiva solo mientras `isLoading` para evitar el doble toque. | [L205](../../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt#L205) |

---

## RegisterScreen

Nombre, correo, contraseña con la lista de requisitos en vivo y la casilla de Términos y Política. Spec 001, HU-01;
spec 005; Figma `585:4447`. Tiene la **misma estructura** que `LoginScreen`; aquí solo se comenta lo distinto.

**Qué recibe.** `state: RegisterUiState`, `onEvent: (RegisterEvent) -> Unit`, `onNavigateToLogin`, `onOpenTerms` y
`onOpenPrivacy`.

### Código comentado

Archivo: [RegisterScreen.kt, líneas 56–94](../../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterScreen.kt#L56-L94)

```kotlin
fun RegisterScreen(
    state: RegisterUiState,
    onEvent: (RegisterEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    onOpenTerms: () -> Unit,                                  // abre los Términos (spec 005)
    onOpenPrivacy: () -> Unit,                                // abre la Política de privacidad
    modifier: Modifier = Modifier
) {
    AuthScreenScaffold(
        isOffline = state.isOffline,
        isReconnecting = state.isLoading,
        onRetryConnection = remember(onEvent) { { onEvent(RegisterEvent.RetryConnectionClicked) } },
        headerContent = {
            RegisterWelcomeHeader()
            Spacer(Modifier.height(LocalFormMetrics.current.sectionSpacing))
        },
        modifier = modifier
    ) {
        RegisterFormFields(state = state, onEvent = onEvent)
        Spacer(Modifier.height(LocalFormMetrics.current.sectionSpacing))

        SmartFoodTermsCheckbox(                               // casilla con dos enlaces dentro del texto
            checked = state.acceptedTerms,
            onCheckedChange = remember(onEvent) { { checked -> onEvent(RegisterEvent.AcceptedTermsChanged(checked)) } },
            onTermsClick = onOpenTerms,                       // tocar "Términos" abre el documento
            onPrivacyClick = onOpenPrivacy                    // tocar "Política" abre el otro
        )
        // Con alto de sobra, el botón y el enlace bajan como en Figma (nodo 585:4447, CA-UI-07-03).
        FlexibleGap(minHeight = LocalFormMetrics.current.sectionSpacing)

        RegisterActionButtons(
            isLoading = state.isLoading,
            enabled = state.acceptedTerms,                    // [U2] sin aceptar los términos, el botón queda gris
            onRegisterClicked = remember(onEvent) { { onEvent(RegisterEvent.RegisterClicked) } },
            onNavigateToLogin = onNavigateToLogin
        )
        Spacer(Modifier.weight(2f))
    }
}
```

Lo distinto en `RegisterFormFields`:

Archivo: [RegisterScreen.kt, líneas 130–140](../../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterScreen.kt#L130-L140)

```kotlin
            contentType = ContentType.PersonFullName,         // el autocompletado ofrece el nombre de la persona
            value = state.fullName,
            onValueChange = remember(onEvent) { { name -> onEvent(RegisterEvent.FullNameChanged(name)) } },
            placeholder = stringResource(id = R.string.register_name_placeholder),
            enabled = !state.isLoading,
            isError = state.fullNameHasError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words, // [U2] mayúscula al inicio de cada palabra
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
```

Archivo: [RegisterScreen.kt, líneas 163–177](../../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterScreen.kt#L163-L177)

```kotlin
        PasswordFieldWithChecklist(                           // contraseña + popup con los requisitos encima del campo
            label = stringResource(id = R.string.login_password_label),
            value = state.password,
            onValueChange = remember(onEvent) { { pass -> onEvent(RegisterEvent.PasswordChanged(pass)) } },
            validation = state.passwordValidation,            // qué reglas cumple ahora (calculado fuera de la pantalla)
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onEvent(RegisterEvent.RegisterClicked)
                }
            )
        )
```

`// …` Omitidos: `RegisterWelcomeHeader`, el campo de correo (igual al de Login), `RegisterActionButtons` (igual al de
Login, con `enabled`) y el `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| **Estado que habilita un botón** | U2 · Cómo agregar un botón a una app | `enabled = state.acceptedTerms`: la pantalla no decide con un `if` propio, solo refleja el estado. Al marcar la casilla llega un estado nuevo y el botón se activa en la recomposición. | [L88](../../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterScreen.kt#L88) |
| **Popup anclado y accesible** | U2 · Interactuar con la IU y el estado | `PasswordFieldWithChecklist` muestra los requisitos en un popup **encima** del campo enfocado; `SmartFoodTermsCheckbox` decide por carácter si el toque fue en un enlace y ofrece los enlaces a TalkBack como acciones. | [L77](../../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterScreen.kt#L77) |
| **Reutilizar composables** | U1 · Crea un diseño básico | Login y Registro comparten `AuthScreenScaffold`, `SmartFoodTextField`, `AuthErrorMessage` y `FlexibleGap`. Los cambios de diseño se hacen una vez. | [L64](../../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterScreen.kt#L64) |

---

## PasswordRecoveryScreen

Tres pasos en una sola pantalla: correo → código de 4 dígitos → nueva contraseña, con indicador de pasos y cuenta
regresiva para reenviar el código. Spec 001, HU-04; spec 003.

**Qué recibe.** `state: PasswordRecoveryUiState` (lee `step`, `email`, `otpCode`, `newPassword`, `confirmPassword`,
`passwordValidation`, `isLoading`, `error`, `resendCooldownSeconds` y los derivados `isEmailInCooldown`,
`otpCodeHasError`, `passwordsMatch` y `canResetPassword`), `onEvent` y `onNavigateToLogin`.

### Código comentado

Archivo: [PasswordRecoveryScreen.kt, líneas 68–103](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L68-L103)

```kotlin
fun PasswordRecoveryScreen(
    state: PasswordRecoveryUiState,
    onEvent: (PasswordRecoveryEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthScreenScaffold(
        topHeaderContent = {                                  // slot opcional encima del logo
            SmartFoodStepIndicator(currentStep = state.step.stepIndex) // puntos 1-2-3; el paso viene del estado
        },
        headerContent = {
            RecoveryHeader(step = state.step, email = state.email, onEvent = onEvent)
            Spacer(modifier = Modifier.height(LocalFormMetrics.current.sectionSpacing))
        },
        modifier = modifier
    ) {
        AnimatedContent(                                      // [U3] anima el cambio de contenido cuando cambia targetState
            targetState = state.step,                         // el valor que, al cambiar, dispara la animación
            // Sin SizeTransform: cada paso ya llena el alto visible, así que animar el tamaño solo prolongaría el
            // alto del paso anterior (CA-UI-07-03).
            transitionSpec = { (fadeIn() togetherWith fadeOut()).using(null) }, // [U3] fundido de entrada y de salida a la vez
            label = "recovery_step_transition"                // nombre para las herramientas de animación
        ) { targetStep ->                                     // el paso que se está dibujando (el viejo o el nuevo)
            when (targetStep) {                               // [U2] when exhaustivo sobre el enum: un formulario por paso
                RecoveryStep.EMAIL_REQUEST -> ForgotPasswordStep(
                    state = state,
                    onEvent = onEvent,
                    onNavigateToLogin = onNavigateToLogin
                )

                RecoveryStep.OTP_VERIFICATION -> VerifyCodeStep(state = state, onEvent = onEvent)

                RecoveryStep.NEW_PASSWORD -> CreateNewPasswordStep(state = state, onEvent = onEvent)
            }
        }
    }
}
```

Archivo: [PasswordRecoveryScreen.kt, líneas 117–121](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L117-L121)

```kotlin
        val title = when (step) {                             // [U2] when como EXPRESIÓN: devuelve el texto del paso
            RecoveryStep.EMAIL_REQUEST -> stringResource(id = R.string.recovery_step1_title)
            RecoveryStep.OTP_VERIFICATION -> stringResource(id = R.string.recovery_step2_title)
            RecoveryStep.NEW_PASSWORD -> stringResource(id = R.string.recovery_step3_title)
        }
```

Archivo: [PasswordRecoveryScreen.kt, líneas 164–165](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L164-L165)

```kotlin
private fun PasswordRecoveryUiState.errorText(): String? =    // [U3] función de extensión sobre el estado
    error?.let { stringResource(id = it.recoveryMessageRes()) } // error → texto propio de la recuperación, o null
```

Paso 1, el botón con la cuenta regresiva:

Archivo: [PasswordRecoveryScreen.kt, líneas 212–225](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L212-L225)

```kotlin
        SmartFoodPrimaryButton(
            text = if (state.isEmailInCooldown) {             // [U2] if como EXPRESIÓN: elige el texto del botón
                stringResource(
                    id = R.string.recovery_step1_button_countdown, // "Enviar código en %1$s"
                    formatCountdown(state.resendCooldownSeconds)   // argumento del texto: "0:42"
                )
            } else {
                stringResource(id = R.string.recovery_step1_button)
            },
            loadingText = stringResource(id = R.string.login_button_loading),
            onClick = remember(onEvent) { { onEvent(PasswordRecoveryEvent.SendCodeClicked) } },
            enabled = !state.isEmailInCooldown,               // [U2] deshabilitado mientras corre la espera
            isLoading = state.isLoading
        )
```

Paso 2, el código y "Reenviar código":

Archivo: [PasswordRecoveryScreen.kt, líneas 256–297](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L256-L297)

```kotlin
        SmartFoodOtpCodeInput(                                // 4 casillas para el código
            code = state.otpCode,
            onCodeChanged = remember(onEvent) { { code -> onEvent(PasswordRecoveryEvent.OtpCodeChanged(code)) } },
            isError = state.otpCodeHasError,                  // las 4 casillas en rojo si el código falló
            enabled = !state.isLoading,
            onDone = remember(onEvent) { { onEvent(PasswordRecoveryEvent.VerifyCodeClicked) } }
        )

        AuthErrorMessage(message = state.errorText(), modifier = Modifier.fillMaxWidth())

        FlexibleGap(minHeight = metrics.sectionSpacing, weight = 3f)  // Figma: nodo 585:4467

        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.recovery_step2_button),
            loadingText = stringResource(id = R.string.login_button_loading),
            onClick = remember(onEvent) { { onEvent(PasswordRecoveryEvent.VerifyCodeClicked) } },
            isLoading = state.isLoading
        )

        FlexibleGap(minHeight = metrics.sectionSpacing)

        if (state.isEmailInCooldown) {                        // [U2] if en la composición: dibuja una rama u otra
            // Mientras corre la espera, la fila dice "Reenviar código en" y el tiempo va en la píldora; así cabe en una
            // línea desde 360 dp de ancho (CA-REC-04-01 y CA-REC-04-04).
            SmartFoodPromptCountdown(
                prompt = stringResource(id = R.string.recovery_step2_resend_countdown_prompt),
                countdown = formatCountdown(state.resendCooldownSeconds),
                fontSize = 16.sp,  // Figma: nodo 585:4474
                contentDescription = pluralStringResource(    // [U1] texto en singular o plural según la cantidad
                    id = R.plurals.recovery_resend_countdown_a11y,
                    count = state.resendCooldownSeconds,      // elige "1 segundo" o "42 segundos"
                    state.resendCooldownSeconds               // y lo pone dentro del texto
                )
            )
        } else {
            SmartFoodPromptLink(
                prompt = stringResource(id = R.string.recovery_step2_resend_prompt),
                action = stringResource(id = R.string.recovery_step2_resend_action),
                onClick = remember(onEvent) { { onEvent(PasswordRecoveryEvent.ResendCodeClicked) } },
                fontSize = 16.sp  // Figma: nodo 585:4474
            )
        }
```

Paso 3, "Listo" solo cambia la contraseña si es válida:

Archivo: [PasswordRecoveryScreen.kt, línea 346](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L346)

```kotlin
                    if (state.canResetPassword) onEvent(PasswordRecoveryEvent.ResetPasswordClicked) // derivado del estado: válida y confirmada
```

`// …` Omitidos: el resto de `RecoveryHeader`, el campo de correo del paso 1, los dos campos del paso 3 (iguales al
de Registro) y los tres `@Preview` (uno por paso).

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-animatedcontent"></a>**`AnimatedContent`** | U3 · Cómo compilar apps fabulosas | Anima el cambio entre dos contenidos cuando cambia `targetState`. Durante el fundido, el contenido se dibuja con el paso viejo y con el nuevo a la vez; por eso la lambda recibe `targetStep` y no lee `state.step` directamente. `fadeIn() togetherWith fadeOut()` superpone las dos animaciones, y `using(null)` desactiva la animación de tamaño. | [L84](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L84) |
| <a id="c-when"></a>**`when` exhaustivo** | U2 · Aspectos básicos de Kotlin | Con un `enum` o una `sealed interface`, `when` sin `else` obliga a cubrir todos los casos: si se agrega un cuarto paso, la app no compila hasta dibujarlo. Como expresión, `when` también **devuelve** un valor (el título de cada paso). | [L91](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L91) |
| <a id="c-if-expr"></a>**`if` como expresión e `if` en la composición** | U2 · Aspectos básicos de Kotlin | `text = if (…) a else b` elige un valor. Un `if` dentro de un composable elige **qué se dibuja**: la rama que no se cumple no existe en la composición. | [L277](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L277) |
| <a id="c-format"></a>**Textos con argumentos y plurales** | U1 · Crea un diseño básico | `stringResource(id, arg)` reemplaza `%1$s` del `strings.xml`. `pluralStringResource` elige la forma según la cantidad ("1 segundo", "42 segundos"); aquí lo lee TalkBack. | [L284](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L284) |
| **Un solo `onEvent` para tres pasos** | U4 · Componentes de la arquitectura | Los tres formularios comparten el mismo estado y el mismo canal de eventos: el correo y el código no viajan entre pantallas, y "Cambiar correo" solo cambia `step`. | [L98](../../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L98) |
