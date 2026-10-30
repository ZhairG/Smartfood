# 04 · Perfil

Pantallas: [Perfil](#profilescreen), [Editar información](#editprofilescreen), [Notificaciones](#notificationsettingsscreen),
[Cambiar contraseña](#changepasswordscreen) y [Preferencias alimentarias](#foodpreferencesscreen). Índice general y mapa
de conceptos: [README](README.md). Los conceptos ya explicados se enlazan a [01](01-arranque-y-legal.md),
[02](02-autenticacion.md) y [03](03-inventario.md).

Las cuatro pantallas internas repiten el armazón del editor de productos:
[`SmartFoodTopBar`](03-inventario.md#producteditorscreen) + [`BoxWithConstraints`](03-inventario.md#c-boxwithconstraints)
+ [`CompositionLocalProvider(LocalFormMetrics …)`](03-inventario.md#c-provider) + [`imePadding`](03-inventario.md#c-ime).
Aquí se comenta solo lo propio de cada una.

---

## ProfileScreen

La pestaña Perfil: encabezado verde con la foto o las iniciales, nombre y correo; tres tarjetas de cifras; las opciones
de la cuenta; "Tema de la app" y "Cerrar Sesión" fijo abajo. Spec 001 HU-05; spec 008; Figma `306:369`.

**Qué recibe.** `state: ProfileUiState` (lee `dialog`, `fullName`, `email`, `initials`, `photoUrl`, `inventoryCount`,
`monthlySavingsCop`, `themeMode` e `isThemeSheetVisible`) y `onEvent`.

### Código comentado

Archivo: [ProfileScreen.kt, líneas 85–114](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L85-L114)

```kotlin
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // El encabezado es verde oscuro: la barra de estado lleva íconos claros.
    SystemBarEffect(isLightBackground = false)

    // "¿Cerrar sesión?" del Figma (604:2668); con cambios sin subir, avisa que podrían perderse (CA-INV-10-07).
    (state.dialog as? ProfileDialog.ConfirmLogout)?.let { dialog -> // [U3] as?: conversión segura; null si es otro tipo
        SmartFoodWarningDialog(
            title = stringResource(id = R.string.profile_logout_title),
            body = stringResource(
                id = if (dialog.hasPendingChanges) R.string.profile_logout_pending_question else R.string.profile_logout_body
            ),                                                // otro texto si hay cambios sin subir
            confirmText = stringResource(id = R.string.profile_logout_pending_confirm),
            dismissText = stringResource(id = R.string.profile_logout_pending_cancel),
            onConfirm = { onEvent(ProfileEvent.LogoutConfirmed) },
            onDismiss = { onEvent(ProfileEvent.DialogDismissed) },
            // Con cambios sin subir, el aviso va en rojo: confirmar puede perderlos (CA-INV-10-07).
            destructive = dialog.hasPendingChanges
        )
    }

    BoxWithConstraints(                                       // [U4] tamaño disponible para escalar todo el Perfil
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val dimens = profileDimens(maxWidth, maxHeight)       // [U4] medidas calculadas con el ancho y el alto reales
```

Archivo: [ProfileScreen.kt, líneas 119–156](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L119-L156)

```kotlin
            Column(
                verticalArrangement = Arrangement.spacedBy(dimens.gap),
                modifier = Modifier
                    .weight(1f)                               // ocupa lo que deja el encabezado y el botón de abajo
                    .verticalScroll(rememberScrollState())    // [U2] se desplaza si no cabe
                    .padding(horizontal = dimens.side, vertical = dimens.gap)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.statGap), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        value = stringResource(id = R.string.profile_stat_recipes_value),
                        label = stringResource(id = R.string.profile_stat_recipes),
                        dimens = dimens,
                        modifier = Modifier.weight(1f)        // [U1] weight(1f) en las tres: el mismo ancho para cada tarjeta
                    )
                    StatCard(
                        value = state.inventoryCount.toString(), // [U2] Int → String
                        label = stringResource(id = R.string.profile_stat_ingredients),
                        dimens = dimens,
                        modifier = Modifier.weight(1f)
                    )
                    // "Ahorrados este mes" (CA-RIA-04-06): pesos con punto de miles, "$0" sin cocinadas.
                    StatCard(
                        value = "$" + formatCop(state.monthlySavingsCop.toLong()), // "$12.500"
                        label = stringResource(id = R.string.profile_stat_savings),
                        dimens = dimens,
                        modifier = Modifier.weight(1f)
                    )
                }
                OptionsCard(                                  // la tarjeta de opciones avisa con una lambda por fila
                    dimens = dimens,
                    themeMode = state.themeMode,
                    onEditClick = { onEvent(ProfileEvent.EditProfileClicked) },
                    onPreferencesClick = { onEvent(ProfileEvent.PreferencesClicked) },
                    onNotificationsClick = { onEvent(ProfileEvent.NotificationsClicked) },
                    onPasswordClick = { onEvent(ProfileEvent.PasswordClicked) },
                    onThemeClick = { onEvent(ProfileEvent.ThemeClicked) }
                )
            }
```

Archivo: [ProfileScreen.kt, líneas 186–196](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L186-L196)

```kotlin
        if (state.isThemeSheetVisible) {                      // la hoja de tema existe solo si el estado lo dice
            CompositionLocalProvider(LocalFormMetrics provides formMetricsFor(maxHeight)) {
                OptionSheet(                                  // hoja genérica de opciones (la misma de categoría y unidad)
                    title = stringResource(id = R.string.profile_option_theme),
                    options = ThemeMode.entries,              // [U3] entries: todos los valores del enum
                    selected = state.themeMode,
                    label = { stringResource(id = it.titleRes) }, // [U3] lambda que convierte cada opción en texto
                    onConfirm = { mode -> onEvent(ProfileEvent.ThemeSelected(mode)) },
                    onDismiss = { onEvent(ProfileEvent.ThemeSheetDismissed) }
                )
            }
```

El avatar: la foto de Internet o las iniciales.

Archivo: [ProfileScreen.kt, líneas 223–238](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L223-L238)

```kotlin
                if (state.photoUrl != null) {
                    AsyncImage(                               // [U5] Coil: descarga en segundo plano y guarda en caché
                        model = state.photoUrl,               // la URL de Firebase Storage
                        contentDescription = null,            // el Box padre ya anuncia el nombre a TalkBack
                        contentScale = ContentScale.Crop,     // [U5] recorta para llenar el círculo sin deformar
                        modifier = Modifier.size(dimens.avatar)
                    )
                } else {
                    Text(
                        text = state.initials.orEmpty(),      // [U2] orEmpty(): "" si es null
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = dimens.initialsText,
                            fontWeight = FontWeight.Bold
                        )
                    )
```

La lista de opciones, armada con datos:

Archivo: [ProfileScreen.kt, líneas 330–355](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L330-L355)

```kotlin
    val options = listOf(                                     // [U3] lista de pares (ícono, texto)
        R.drawable.ic_figma_profile_edit to R.string.profile_option_edit, // [U3] "a to b" crea un Pair
        R.drawable.ic_figma_profile_preferences to R.string.profile_option_preferences,
        R.drawable.ic_figma_profile_notifications to R.string.profile_option_notifications,
        R.drawable.ic_figma_profile_security to R.string.profile_option_password,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(dimens.cardRadius))
            .border(CardBorder, MaterialTheme.colorScheme.outline, RoundedCornerShape(dimens.cardRadius))
    ) {
        options.forEachIndexed { index, (icon, textRes) ->    // [U3] desestructura el Pair en dos variables
            OptionRow(
                icon = icon,
                text = stringResource(id = textRes),
                dimens = dimens,
                onClick = when (textRes) {                    // [U2] when que devuelve la lambda de cada fila
                    R.string.profile_option_edit -> onEditClick
                    R.string.profile_option_preferences -> onPreferencesClick
                    R.string.profile_option_notifications -> onNotificationsClick
                    R.string.profile_option_password -> onPasswordClick
                    else -> null                              // sin acción: la fila no se puede tocar
                }
            )
            if (index < options.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline) // línea entre filas, no después de la última
```

Archivo: [ProfileScreen.kt, líneas 454–460](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L454-L460)

```kotlin
private fun profileDimens(width: Dp, height: Dp): ProfileDimens {
    val scale = (width / FigmaWidth).coerceIn(MinScale, 1f)   // ancho real / 412 dp del Figma, sin bajar de 0,8
    // En pantallas más bajas que el Figma, el encabezado y los espacios verticales se reducen.
    val compact = (height / FigmaHeight).coerceIn(MinScale, 1f) // alto real / 917 dp del Figma
    return ProfileDimens(                                     // [U3] data class con todas las medidas
        side = 28.dp * scale,                                 // los márgenes laterales escalan con el ancho
        gap = 20.dp * scale * compact,                        // los espacios verticales, también con el alto
```

`// …` Omitidos: `ProfileHeader` completo (nombre, correo y el botón de cámara sobre el avatar), `StatCard`,
`OptionRow`, el botón "Cerrar Sesión" (`OutlinedButton` con ícono y texto), el resto de `ProfileDimens` y el `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-asyncimage"></a>**`AsyncImage` (Coil)** | U5 · Cómo cargar y mostrar imágenes de Internet | Descarga y decodifica la imagen **sin bloquear** la interfaz, la guarda en caché (memoria y disco) y la decodifica al tamaño del composable. `ContentScale.Crop` llena el espacio recortando lo que sobra. | [L224](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L224) |
| <a id="c-escalado"></a>**Escalar con `BoxWithConstraints`** | U4 · Cómo adaptar tu app a diferentes tamaños de pantalla | El Perfil se diseñó para 412 × 917 dp. Con el tamaño real, `profileDimens` calcula un factor y multiplica cada medida (`28.dp * scale`). En un teléfono angosto todo se reduce en la misma proporción, sin bajar del 80 %. | [L454](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L454) |
| <a id="c-ascast"></a>**`as?` (conversión segura)** | U3 · Más aspectos básicos de Kotlin | `state.dialog as? ProfileDialog.ConfirmLogout` devuelve el diálogo si es de ese tipo, y `null` si no. Con `?.let` el aviso solo se dibuja en ese caso. | [L94](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L94) |
| <a id="c-pair"></a>**`Pair`, `to` y desestructuración** | U3 · Más aspectos básicos de Kotlin | `a to b` crea un `Pair`. En `{ index, (icon, textRes) -> }` los paréntesis separan el par en dos variables. Así la lista de opciones son **datos**, y una sola `OptionRow` dibuja las cuatro. | [L342](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L342) |
| <a id="c-entries"></a>**`enum.entries`** | U3 · Más aspectos básicos de Kotlin | Lista con todos los valores del `enum` en orden. La hoja de tema muestra `LIGHT`, `DARK` y `SYSTEM` sin escribirlos a mano. | [L190](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L190) |
| **`weight(1f)` repetido** | U1 · Crea un diseño básico | Tres hijos con `weight(1f)` en un `Row` se reparten el ancho por igual, sin calcular nada. | [L126](../../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L126) |

**Nota.** La KDoc de `ProfileScreen.kt` dice que "Cambiar contraseña" se ve deshabilitada, pero el código le pasa
`onPasswordClick` y la fila navega a su pantalla. La KDoc está desactualizada. Además, la primera cifra ("Recetas") muestra
un texto fijo de `strings.xml`, no un dato calculado.

---

## EditProfileScreen

Foto, nombre, correo, teléfono y fecha de nacimiento. Si el correo cambia, aparece "Contraseña actual". Spec 008,
HU-PER-02; Figma `361:649`.

**Qué recibe.** `state: EditProfileUiState` (lee `isSaveEnabled`, `isLoading`, `errorField`, `newPhotoPath`, `photoUrl`,
`initials`, los campos, `isEmailChanged`, `error` y `dialog`) y `onEvent`.

### Código comentado

Archivo: [EditProfileScreen.kt, líneas 87–98](../../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt#L87-L98)

```kotlin
            action = {
                // Figma 362:723: "Guardar" naranja en Bold de 20 sp; sin cambios se ve deshabilitado (CA-PER-02-02).
                TextButton(onClick = { onEvent(EditProfileEvent.SaveClicked) }, enabled = state.isSaveEnabled) {
                    Text(
                        text = stringResource(id = R.string.edit_profile_save),
                        color = MaterialTheme.colorScheme.secondary.copy(
                            alpha = if (state.isSaveEnabled) 1f else DisabledAlpha // se ve gris si no hay cambios
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
```

Archivo: [EditProfileScreen.kt, líneas 135–143](../../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt#L135-L143)

```kotlin
            ProfileAvatar(                                    // componente compartido con el Perfil
                photo = state.newPhotoPath?.let(::File) ?: state.photoUrl, // [U2] ?: (Elvis): la foto nueva o la de la nube
                initials = state.initials,
                size = spacing.iconDialogSize,
                initialsSize = InitialsSize,
                badgeBorder = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(id = R.string.cd_profile_change_photo),
                onClick = { onEvent(EditProfileEvent.PhotoClicked) }
            )
```

Archivo: [EditProfileScreen.kt, líneas 153–176](../../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt#L153-L176)

```kotlin
        SmartFoodTextField(
            label = stringResource(id = R.string.edit_profile_name),
            value = state.fullName,
            onValueChange = { onEvent(EditProfileEvent.FullNameChanged(it)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = errorField == EditProfileField.FULL_NAME // [U2] rojo si el error es de este campo
        )
        SmartFoodTextField(
            label = stringResource(id = R.string.edit_profile_email),
            value = state.email,
            onValueChange = { onEvent(EditProfileEvent.EmailChanged(it)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            isError = errorField == EditProfileField.EMAIL
        )
        // El correo nuevo se confirma con la contraseña actual (CA-PER-02-10).
        if (state.isEmailChanged) {                           // [U2] el campo aparece solo si el correo cambió
            SmartFoodPasswordField(
                label = stringResource(id = R.string.edit_profile_current_password),
                value = state.currentPassword,
                onValueChange = { onEvent(EditProfileEvent.CurrentPasswordChanged(it)) },
                placeholder = stringResource(id = R.string.edit_profile_current_password_hint),
                isError = errorField == EditProfileField.CURRENT_PASSWORD
            )
        }
```

Archivo: [EditProfileScreen.kt, líneas 200–210](../../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt#L200-L210)

```kotlin
        state.error?.takeIf { state.dialog !is EditProfileDialog.EmailCode }?.let { error -> // [U2] takeIf: solo si NO está el diálogo del código
            Text(
                text = stringResource(id = error.messageRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite } // TalkBack lo anuncia al aparecer
            )
        }
```

`// …` Omitidos: el armazón (igual al editor de productos), el teléfono (`KeyboardType.Phone`), la fecha
(`SmartFoodSelectorField` que abre el calendario), `EditProfileDialogs` (hoja de foto, calendario, "¿Guardar?",
`EmailCodeDialog` y "Guardado") y el `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-elvis"></a>**Operador Elvis `?:` y referencia `::File`** | U2 · Aspectos básicos de Kotlin | `a ?: b` usa `a` si no es `null` y si no, `b`. `newPhotoPath?.let(::File)` convierte la ruta en `File` solo si existe (`::File` es una referencia al constructor). El avatar muestra la foto recién elegida antes que la de la nube. | [L136](../../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt#L136) |
| <a id="c-takeif"></a>**`takeIf`** | U2 · Aspectos básicos de Kotlin | Devuelve el valor si cumple la condición, y `null` si no. El error se muestra debajo del formulario solo cuando no está abierto el diálogo del código, que lo muestra por su cuenta. | [L200](../../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt#L200) |
| **Campo condicional** | U2 · Interactuar con la IU y el estado | `if (state.isEmailChanged)` agrega o quita el campo de contraseña de la composición. El campo aparece apenas se cambia el correo, porque `isEmailChanged` se deriva del estado. | [L168](../../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt#L168) |
| **Botón "Guardar" deshabilitado** | U2 · Cómo agregar un botón a una app | `enabled = state.isSaveEnabled` bloquea el toque; el `alpha` del color hace que también **se vea** deshabilitado, porque el texto usa un color propio. | [L89](../../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt#L89) |

**Qué le da la Route.** [`EditProfileRoute.kt`](../../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileRoute.kt) abre la cámara del sistema (`ActivityResultContracts.TakePicture()`) o el
selector de fotos (`PickVisualMedia()`), borra del teléfono las fotos que ya no se van a guardar e intercepta "atrás"
con `BackHandler`.

---

## NotificationSettingsScreen

Tres interruptores: productos por vencer, recetas sugeridas e inventario vacío. Cada uno se guarda al tocarlo. Spec 008,
HU-PER-04; Figma `593:2079`.

**Qué recibe.** `state: NotificationSettingsUiState` (lee `isLoading`, `dialog` e `isOn(setting)`) y `onEvent`.

### Código comentado

Archivo: [NotificationSettingsScreen.kt, líneas 99–119](../../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsScreen.kt#L99-L119)

```kotlin
            val settings = listOf(                            // [U3] lista de (ajuste, (título, descripción))
                NotificationSetting.EXPIRING_PRODUCTS to
                    (R.string.notification_option_expiring to R.string.notification_option_expiring_description),
                NotificationSetting.SUGGESTED_RECIPES to
                    (R.string.notification_option_suggested_recipes to R.string.notification_option_suggested_recipes_description),
                NotificationSetting.EMPTY_INVENTORY to
                    (R.string.notification_option_empty_inventory to R.string.notification_option_empty_inventory_description)
            )
            settings.forEachIndexed { index, (setting, texts) -> // [U3] desestructuración del Pair
                NotificationSettingRow(
                    title = stringResource(id = texts.first), // first y second: los dos valores del Pair interno
                    description = stringResource(id = texts.second),
                    checked = state.isOn(setting),            // [U2] el interruptor refleja el estado
                    onCheckedChange = { enabled ->
                        onEvent(NotificationSettingsEvent.ToggleRequested(setting, enabled)) // avisa: no cambia solo
                    }
                )
                if (index < settings.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
```

Archivo: [NotificationSettingsScreen.kt, líneas 153–158](../../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsScreen.kt#L153-L158)

```kotlin
        Switch(                                               // [U2] interruptor de Material
            checked = checked,                                // [U2] elevación de estado: el valor llega de afuera
            onCheckedChange = onCheckedChange,
            colors = notificationSwitchColors(),              // [U3] colores del Figma (verde y pulgar blanco)
            modifier = Modifier.semantics { contentDescription = title } // TalkBack dice qué aviso enciende
        )
```

Archivo: [NotificationSettingsScreen.kt, líneas 69–78](../../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsScreen.kt#L69-L78)

```kotlin
    if (state.dialog is NotificationSettingsDialog.PermissionDenied) { // [U2] is: comprueba el tipo
        SmartFoodWarningDialog(                               // aviso de permiso negado
            title = stringResource(id = R.string.notification_permission_title),
            body = stringResource(id = R.string.notification_permission_body),
            confirmText = stringResource(id = R.string.notification_permission_open_settings),
            dismissText = stringResource(id = R.string.edit_profile_cancel),
            onConfirm = { onEvent(NotificationSettingsEvent.OpenSettingsClicked) },
            onDismiss = { onEvent(NotificationSettingsEvent.DialogDismissed) }
        )
    }
```

`// …` Omitidos: el armazón (barra y carga), la fila con título y descripción, los colores del `Switch` y el `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-switch"></a>**`Switch` con estado elevado** | U2 · Interactuar con la IU y el estado | El interruptor no cambia por sí solo: muestra `checked` y avisa con `onCheckedChange`. Solo cambia cuando llega un estado nuevo. | [L153](../../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsScreen.kt#L153) |
| **`is` (comprobación de tipo)** | U2 · Aspectos básicos de Kotlin | `state.dialog is PermissionDenied` es `true` si el diálogo es de ese tipo; con una `sealed interface` es la forma natural de preguntar qué caso es. | [L69](../../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsScreen.kt#L69) |
| **`Pair` anidado** | U3 · Más aspectos básicos de Kotlin | Cada ajuste se relaciona con un par de textos. `texts.first` es el título y `texts.second`, la descripción. | [L109](../../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsScreen.kt#L109) |

**Qué le da la Route.** [`NotificationSettingsRoute.kt`](../../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsRoute.kt) intercepta `onEvent`: si se enciende un interruptor y falta el
permiso `POST_NOTIFICATIONS` (Android 13+), primero lo pide con `rememberLauncherForActivityResult`. También tiene el
`BackHandler`.

---

## ChangePasswordScreen

Contraseña actual, nueva (con la lista de requisitos) y confirmación. Spec 008, HU-PER-05; Figma `603:2358`.

**Qué recibe.** `state: ChangePasswordUiState` (lee `isLoading`, `isSavedSuccess` y los campos), `onEvent` y `onBack`.

### Código comentado

Archivo: [ChangePasswordScreen.kt, líneas 107–124](../../app/src/main/java/com/smartfood/app/feature/profile/ui/password/ChangePasswordScreen.kt#L107-L124)

```kotlin
    if (state.isLoading) {                                    // diálogo de carga mientras cambia la contraseña
        SmartFoodLoadingDialog(text = stringResource(id = R.string.change_password_button_loading))
    }

    if (state.isSavedSuccess) {                               // "Guardado" con el ícono de check
        SmartFoodResultDialog(
            icon = {                                          // [U3] slot del ícono
                Icon(
                    imageVector = Icons.Outlined.Check,       // ícono de Material
                    contentDescription = null,                // decorativo: el título ya dice "Guardado"
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * GlyphFraction) // [U1] dp × número
                )
            },
            title = stringResource(id = R.string.change_password_saved),
            body = stringResource(id = R.string.change_password_saved_body)
        )
    }
```

`// …` Omitidos: el armazón (igual al editor de productos), `ChangePasswordForm` (tres `PasswordFieldWithChecklist`,
como en el [registro](02-autenticacion.md#registerscreen)), `ChangePasswordRoute` (en el mismo archivo: conecta el
ViewModel y navega con `ObserveEffects`) y el `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| **Diálogos según booleanos del estado** | U4 · Componentes de la arquitectura | `isLoading` e `isSavedSuccess` deciden qué diálogo se ve. La pantalla no cierra el diálogo: la lógica cambia el estado tras 2 s y la recomposición lo quita. | [L111](../../app/src/main/java/com/smartfood/app/feature/profile/ui/password/ChangePasswordScreen.kt#L111) |
| **Íconos de Material (`Icons.Outlined`)** | U3 · Cómo compilar apps fabulosas | Íconos vectoriales que vienen con Compose. La mayoría de la app usa íconos exportados del Figma (`ic_figma_*`); este es una excepción. | [L115](../../app/src/main/java/com/smartfood/app/feature/profile/ui/password/ChangePasswordScreen.kt#L115) |

---

## FoodPreferencesScreen

Chips de restricciones (varias), cocinas (varias), picante (una), presupuesto (una) y "Mi supermercado". Spec 008,
HU-PER-03; Figma `376:1260`.

**Qué recibe.** `state: FoodPreferencesUiState` (lee `isLoading`, `draft`, `error`, `isSaveEnabled` y `dialog`) y
`onEvent`.

### Código comentado

Archivo: [FoodPreferencesScreen.kt, líneas 105–131](../../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesScreen.kt#L105-L131)

```kotlin
            PreferenceGroup(title = stringResource(id = R.string.preferences_restrictions)) { // [U3] lambda final = slot
                DietaryRestriction.entries.forEach { restriction -> // [U3] un chip por cada valor del enum
                    PreferenceChip(
                        text = stringResource(id = restriction.titleRes),
                        selected = restriction in state.draft.restrictions, // [U2] in: ¿está en el conjunto? (varios)
                        onClick = { onEvent(FoodPreferencesEvent.RestrictionToggled(restriction)) }
                    )
                }
            }
            PreferenceGroup(title = stringResource(id = R.string.preferences_cuisines)) {
                Cuisine.entries.forEach { cuisine ->
                    PreferenceChip(
                        text = stringResource(id = cuisine.titleRes),
                        selected = cuisine in state.draft.cuisines,
                        onClick = { onEvent(FoodPreferencesEvent.CuisineToggled(cuisine)) }
                    )
                }
            }
            PreferenceGroup(title = stringResource(id = R.string.preferences_spice)) {
                SpiceLevel.entries.forEach { level ->
                    PreferenceChip(
                        text = stringResource(id = level.titleRes),
                        selected = state.draft.spice == level,    // == : solo uno a la vez
                        onClick = { onEvent(FoodPreferencesEvent.SpiceSelected(level)) }
                    )
                }
            }
```

Archivo: [FoodPreferencesScreen.kt, líneas 179–198](../../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesScreen.kt#L179-L198)

```kotlin
private fun PreferenceGroup(
    title: String,
    content: @Composable () -> Unit,                          // [U3] slot: los chips llegan desde afuera
) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        // Figma 376:1260: título en el verde oscuro de la app y Bold.
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        FlowRow(                                              // [U4] fila que pasa a la siguiente línea si no cabe
            horizontalArrangement = Arrangement.spacedBy(spacing.small), // espacio entre chips de una línea
            verticalArrangement = Arrangement.spacedBy(spacing.small)    // espacio entre líneas
        ) {
            content()                                         // dibuja los chips recibidos
        }
    }
}
```

Archivo: [FoodPreferencesScreen.kt, líneas 161–165](../../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesScreen.kt#L161-L165)

```kotlin
        SmartFoodPrimaryButton(                               // fuera del desplazable: siempre visible abajo
            text = stringResource(id = R.string.preferences_save),
            onClick = { onEvent(FoodPreferencesEvent.SaveClicked) },
            enabled = state.isSaveEnabled,                    // solo si algo cambió (borrador ≠ guardado)
            green = true,                                     // variante verde del botón
```

`// …` Omitidos: los grupos de presupuesto y supermercado (mismo patrón), el error, el armazón, los diálogos y el
`@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-flowrow"></a>**`FlowRow`** | U4 · Cómo adaptar tu app a diferentes tamaños de pantalla | Como un `Row`, pero cuando los chips no caben pasan a la línea siguiente. En un teléfono angosto quedan en tres líneas y en uno ancho, en dos, sin calcular columnas. Requiere `@OptIn(ExperimentalLayoutApi::class)`. | [L191](../../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesScreen.kt#L191) |
| <a id="c-slot-content"></a>**Slot `content: @Composable () -> Unit`** | U3 · Cómo compilar apps fabulosas | `PreferenceGroup` dibuja el título y el `FlowRow`; los chips llegan como contenido. Es el mismo patrón de `Column { }` o `Scaffold { }`: la lambda final es el slot. | [L181](../../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesScreen.kt#L181) |
| **Selección múltiple frente a única** | U2 · Interactuar con la IU y el estado | `restriction in state.draft.restrictions` (un `Set`) permite varios chips marcados; `state.draft.spice == level` permite solo uno. La pantalla solo pregunta; la regla de agregar o quitar está fuera. | [L109](../../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesScreen.kt#L109) |
