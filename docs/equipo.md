# Configuración para integrantes del equipo

Pasos que cada integrante hace en un computador nuevo antes de ejecutar la app contra el Firebase del proyecto. Quien solo quiere probar la app debe usar la [APK del Release](../README.md#instalar-la-app); quien quiere compilar, la [guía de instalación](instalacion.md).

## ⚠️ Primero: configurar tu equipo (obligatorio al clonar o hacer pull)

Haz estos dos pasos **antes de ejecutar la app** en un computador nuevo y cada vez que cambie la configuración de
Firebase. Si no los haces, la app compila y abre, pero el registro y el login fallan con *"Ocurrió un error
inesperado"*.

### 1. Pedir el `google-services.json` vigente

El archivo `app/google-services.json` **no está en el repositorio** (está en `.gitignore`), así que `git pull` no lo
trae ni lo actualiza.

- La única API key válida es la que administra **Zhair** en Google Cloud Console (proyecto `smartfood-f62a1`).
  Pídele a él el `google-services.json` actual por un canal privado y cópialo en `app/`.
- No uses copias viejas ni archivos de otro proyecto. Una key vencida o distinta produce:
  `API key expired. Please renew the API key.`
- Después de reemplazarlo: **Build → Clean Project** y volver a ejecutar.

### 2. Registrar la huella SHA-1 de tu computador

La API key solo acepta peticiones de apps Android autorizadas (paquete `com.smartfood.app` + huella SHA-1). Cada
computador firma el APK de debug con su propio `~/.android/debug.keystore`, así que **cada integrante tiene una SHA-1
distinta** y debe registrarla. Si falta, aparece:
`Requests from this Android client application com.smartfood.app are blocked.`

1. En la raíz del proyecto, ejecuta:

   ```bash
   ./gradlew :app:signingReport        # En Windows: gradlew.bat :app:signingReport
   ```

   Copia la línea `SHA1:` del bloque `Variant: debug`. La huella no es un secreto.
2. Envíasela a Zhair. Él la agrega en dos lugares:
   - **Google Cloud Console** → *APIs y servicios → Credenciales* → la API key de Android → *Restricciones de
     aplicación* → **Agregar** `com.smartfood.app` + la SHA-1. Este paso es el que quita el bloqueo.
   - **Firebase Console** → *Configuración del proyecto → Tus apps → Android* → **Agregar huella digital**.
3. Espera unos 5 minutos a que se propague el cambio. No hace falta recompilar.

Si cambias de computador o borras `~/.android/debug.keystore`, tu SHA-1 cambia: repite el paso 2.

### ¿Cómo sé cuál de los dos falla?

En Logcat, filtra por `package:com.smartfood.app level:error`. El mensaje que sale con la etiqueta
`RecaptchaCallWrapper` indica la causa:

| Mensaje en Logcat | Causa | Solución |
|---|---|---|
| `API key expired. Please renew the API key.` | `google-services.json` viejo o de otra fuente | Paso 1 |
| `Requests from this Android client application com.smartfood.app are blocked.` | SHA-1 de tu computador sin registrar | Paso 2 |

El conflicto en archivos de `.idea/` (como `deploymentTargetSelector.xml`) **no** tiene relación con estos errores:
solo guardan preferencias de Android Studio.


## Puesta en marcha

1. Completar la sección **"Primero: configurar tu equipo"** de esta guía (arriba): `google-services.json`
   vigente en `app/` y SHA-1 registrada.
2. Abrir el proyecto en Android Studio y sincronizar Gradle.
3. Solo una vez por proyecto (ya está hecho en `smartfood-f62a1`), en la consola de Firebase:
   - **Authentication** → habilitar *Correo/contraseña*.
   - **Firestore Database** → crear la base de datos.
   - Publicar las reglas: `firebase deploy --only firestore:rules`.
4. Ejecutar la configuración `app`.

La recuperación de contraseña y el correo de bienvenida necesitan además las Cloud Functions desplegadas (ver la
sección siguiente). Sin ellas, usa el modo demo o los emuladores.
