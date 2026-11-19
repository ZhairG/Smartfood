# Plan 012: Modo sin facturación

> Cómo se implementa la [spec](./spec.md). Tareas en [`tasks.md`](./tasks.md).

## 1. Interruptor

```kotlin
// core/domain/BackendMode.kt (Kotlin puro)
enum class BackendMode { BLAZE, FREE }

interface BackendModeProvider {
    /** El modo activo. Si el interruptor no se pudo leer, FREE, que funciona en los dos planes. */
    fun current(): BackendMode
    /** Pide a Remote Config el valor más reciente, sin esperar la respuesta. */
    fun refresh()
}

class GetBackendModeUseCase @Inject constructor(private val provider: BackendModeProvider) {
    operator fun invoke(): BackendMode = provider.current()
}
```

- `core/data/RemoteConfigBackendModeProvider` (`@Singleton`): parámetro `blaze_activo`, por defecto `false`;
  `minimumFetchIntervalInSeconds` de 3600 en release y 0 en debug. Si `BuildConfig.FORCED_BACKEND_MODE` es `"blaze"` o
  `"free"`, devuelve ese modo sin leer Remote Config.
- `SmartfoodApp.onCreate()` llama a `refresh()`. Cada operación lee `current()` al empezar.
- `CoreDataModule` provee `FirebaseRemoteConfig` y enlaza el provider.
- Gradle: `firebase-config` (BOM) y `buildConfigField("String", "FORCED_BACKEND_MODE", …)` con la propiedad
  `-PbackendMode` en debug y `""` en release.

## 2. Repositorios por modo

Cada uno se registra en Hilt y en cada llamada delega según `current()`:

| Interfaz | Blaze (sin cambios) | Gratuito (nueva) | Registrada |
|---|---|---|---|
| `PasswordRecoveryRepository` | `RemotePasswordRecoveryRepository` | `LinkPasswordRecoveryRepository` | `ModeAwarePasswordRecoveryRepository` (el demo sigue teniendo prioridad) |
| `EmailChangeRepository` | `RemoteEmailChangeRepository` | `LinkEmailChangeRepository` | `ModeAwareEmailChangeRepository` |
| `PhotoRepository` (inventario) | `FirebaseStoragePhotoRepository` | `FirestorePhotoRepository` | `ModeAwarePhotoRepository` |
| `ProfilePhotoRepository` | `FirebaseStorageProfilePhotoRepository` | `FirestoreProfilePhotoRepository` | `ModeAwareProfilePhotoRepository` |

- `PasswordRecoveryRepository` y `EmailChangeRepository` suman `val usesCode: Boolean`: el ViewModel decide si muestra
  los pasos del código o el aviso del enlace.
- En las fotos, `delete(path)` decide **por la ruta**, no por el modo: una ruta `users/{uid}/media/…` se borra en
  Firestore; una de Storage se borra en Storage en modo Blaze y es un no-op con éxito en el gratuito.

## 3. Fotos en Firestore

```
users/{uid}/media/{mediaId}   data (bytes ≤ 900 KB), contentType ('image/jpeg'), createdAt
```

- `mediaId`: `inv-{itemId}-{uuid}` para el inventario y `profile-{uuid}` para el perfil. Nunca se sobrescribe: el flujo
  actual sube la nueva y después borra la anterior por su ruta, sin cambios en los casos de uso ni en los workers.
- `ItemPhoto.path` = `users/{uid}/media/{mediaId}`; `ItemPhoto.url` = `smartfood-media://users/{uid}/media/{mediaId}`.
  En el perfil, los mismos valores van en `photoPath` y `photoUrl`.
- `PhotoCompressor.compressToFit(file, maxBytes)`: 1080 px y JPEG 80; si pasa de `maxBytes`, calidad 60 y 40, y
  después escala al 75 % hasta que quepa.
- `MediaFetcher` (Coil 3): `Fetcher.Factory<Uri>` para el esquema `smartfood-media`. Lee el documento con
  `FirestoreMediaStore.read` y lo guarda en `cacheDir/smartfood-media/{sha256(url)}`. `SmartfoodApp` implementa
  `SingletonImageLoader.Factory` para registrarlo.

## 4. Pantallas

- **Recuperación:** `RecoveryStep.LINK_SENT`. `PasswordRecoveryUiState.isLinkMode` viene de
  `PasswordRecoveryRepository.usesCode` a través de `GetRecoveryModeUseCase`. Con el enlace, `sendCode()` pasa a
  `LINK_SENT` y arranca la misma espera de 60 s; «Reenviar enlace» repite `sendCode()`.
- **Editar información:** si `EmailChangeRepository.usesCode` es `false`, `save()` no abre `EmailCode`: abre
  `Saved(emailChangedTo = null, emailLinkSentTo = correo)`.
- **Registro:** `RegisterUseCase` llama a `AuthRepository.sendEmailVerification()` en el modo gratuito; un fallo se ignora.

## 5. Reglas (`firestore.rules`)

```
match /users/{uid}/media/{mediaId} {
  allow read, delete: if isOwner(uid);
  allow create: if isOwner(uid)
    && request.resource.data.keys().hasOnly(['data', 'contentType', 'createdAt'])
    && request.resource.data.data is bytes
    && request.resource.data.data.size() <= 900 * 1024
    && request.resource.data.contentType == 'image/jpeg';
  allow update: if false;
}
```

`validProfileUpdate()` acepta `photoPath` de `users/{uid}/profile/…` o `users/{uid}/media/…`.

## 6. Migración

`functions/scripts/migrar-storage-a-firestore.mjs` (no se despliega). Recorre `users/{uid}` y su `inventory`: cada
`photoPath` que no empieza con `users/{uid}/media/` se descarga de Storage, se guarda en `media` y se cambian
`photoPath` y `photoUrl`. Sin `--aplicar` solo simula. Las fotos de más de 900 KB se saltan con un aviso.

```bash
gcloud auth application-default login
node functions/scripts/migrar-storage-a-firestore.mjs             # simulación
node functions/scripts/migrar-storage-a-firestore.mjs --aplicar   # copia y cambia las rutas
```
