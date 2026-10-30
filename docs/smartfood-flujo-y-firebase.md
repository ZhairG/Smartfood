# SmartFood: flujo de trabajo y Firebase

Respuestas cortas a las preguntas de control del curso.

---

## 1. ¿Cuál es el flujo de trabajo?

Usamos **Spec Driven Development**: primero se escribe qué se va a hacer y después el código.

1. **Constitución** ([`specs/constitution.md`](../specs/constitution.md)): reglas que nunca se rompen (MVVM, sin secretos en el APK, fidelidad con Figma).
2. **`spec.md`**: historias de usuario (`HU-NN`) y criterios de aceptación (`CA-...`) en formato *Dado / Cuando / Entonces*.
3. **`plan.md`**: cómo se va a construir (modelos, interfaces, decisiones).
4. **`tasks.md`**: lista de tareas (`Tnnn`) y qué prueba cubre cada criterio.

En GitHub:

- Cada feature es un **Issue**. El plan se publica en el Issue y Zhair lo aprueba antes de programar.
- Hay **una rama por módulo** (por ejemplo, `feature/009-recetas-ia`) y **un commit por tarea**, en español y citando el CA:
  `feat(perfil): pantalla Editar información (spec 008, T007, CA-PER-02-01)`
- Hay **un Pull Request por módulo** hacia `master`. Antes de abrirlo deben pasar las pruebas, la compilación y lint. Solo Zhair lo fusiona.
- Nadie hace push directo a `master` ni usa `--force`.

---

## 2. ¿Cómo usamos Firebase?

| Servicio | Para qué |
|---|---|
| **Authentication** | Registro e inicio de sesión con correo y contraseña |
| **Cloud Firestore** | Base de datos: perfil, inventario e historial de cocina |
| **Cloud Storage** | Fotos de productos y de perfil |
| **Cloud Functions** | Lógica en el servidor: recuperar la contraseña, cambiar el correo y el correo de bienvenida |
| **Secret Manager** | Credenciales del correo, para que no estén en la app |

**Arquitectura:** `ui → domain ← data`

- **ui**: pantallas en Compose y ViewModels.
- **domain**: modelos, casos de uso e *interfaces* de repositorio. **No conoce Firebase.**
- **data**: la única capa que usa Firebase ([`FirestoreInventoryRepository`](../app/src/main/java/com/smartfood/app/feature/inventory/data/repository/FirestoreInventoryRepository.kt), etc.).
- **Hilt** conecta la interfaz con su implementación. En las pruebas se usa un repositorio falso.

---

## 3. ¿Cómo está la base de datos?

Firestore es NoSQL: guarda **colecciones** que contienen **documentos**, y cada documento es un conjunto de pares clave-valor.

```
users/{uid}                 ← perfil (nombre, correo, preferencias, términos aceptados)
├── inventory/{itemId}      ← productos (nombre, categoría, vencimiento, cantidad, precio, foto)
└── cookings/{cookingId}    ← recetas cocinadas (receta, porciones, dinero ahorrado)

passwordResets, welcomeEmails, emailChanges   ← solo las usa el servidor
```

- Cada usuario tiene sus datos dentro de `users/{su uid}`.
- Las recetas **no** están en Firestore: viajan dentro de la app ([`assets/recetas.json`](../app/src/main/assets/recetas.json)).

**Seguridad ([`firestore.rules`](../firestore.rules)):**

- Un usuario solo puede leer y escribir lo suyo: `request.auth.uid == uid`.
- El servidor valida los datos: campos permitidos, categorías válidas, formato de fecha, cantidades en rango.
- Las reglas tienen pruebas automáticas en [`rules-test/`](../rules-test/).

**Sin conexión:** Firestore guarda una copia local. Si no hay red, el cambio queda en el teléfono ("Guardado en tu teléfono") y se sube solo cuando vuelve la conexión.

---

## 4. ¿Cómo es un CRUD de punta a punta?

Ejemplo: **agregar un producto al inventario**.

```
Pantalla (Compose)  →  ViewModel  →  Caso de uso  →  Repositorio  →  Firestore
 toca "Guardar"        save()        AddItemUseCase   createItem()    users/{uid}/inventory
```

1. **Pantalla** ([`ProductEditorScreen`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt)): solo dibuja el estado y avisa del evento `SaveClicked`.
2. **ViewModel** ([`ProductEditorViewModel`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorViewModel.kt)): arma los datos del formulario, muestra "cargando" y llama al caso de uso.
3. **Caso de uso** ([`AddItemUseCase`](../app/src/main/java/com/smartfood/app/feature/inventory/domain/usecase/SaveItemUseCases.kt#L32)): valida los datos (las reglas de negocio están aquí) y llama a `repository.createItem(item)`.
4. **Repositorio** ([`FirestoreInventoryRepository`](../app/src/main/java/com/smartfood/app/feature/inventory/data/repository/FirestoreInventoryRepository.kt)): escribe en `users/{uid}/inventory/{id}`.
5. **Firestore**: las reglas revisan que el usuario sea el dueño y que los datos sean válidos.
6. **Respuesta**: si sale bien, se vuelve a la lista con el mensaje "Guardado". Si falla, se muestra el error y el formulario conserva lo escrito.

La lista se actualiza sola, porque está **escuchando** Firestore en tiempo real.

| Operación | Caso de uso | Llamada a Firestore |
|---|---|---|
| Create | [`AddItemUseCase`](../app/src/main/java/com/smartfood/app/feature/inventory/domain/usecase/SaveItemUseCases.kt#L32) | `document(id).set(...)` |
| Read | [`ObserveInventoryUseCase`](../app/src/main/java/com/smartfood/app/feature/inventory/domain/usecase/ObserveInventoryUseCase.kt) / [`GetItemUseCase`](../app/src/main/java/com/smartfood/app/feature/inventory/domain/usecase/GetItemUseCase.kt) | `snapshots()` / `document(id).get()` |
| Update | [`UpdateItemUseCase`](../app/src/main/java/com/smartfood/app/feature/inventory/domain/usecase/SaveItemUseCases.kt#L75) | `document(id).update(...)` |
| Delete | [`DeleteItemUseCase`](../app/src/main/java/com/smartfood/app/feature/inventory/domain/usecase/DeleteItemUseCase.kt) | `document(id).delete()` |

---

## 5. ¿Cómo se llama a la base de datos desde Kotlin?

**Dependencias** (`build.gradle.kts`):

```kotlin
implementation(platform("com.google.firebase:firebase-bom:<versión>"))
implementation("com.google.firebase:firebase-firestore")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:<versión>") // para .await()
```

**Referencia a la colección:**

```kotlin
val db = FirebaseFirestore.getInstance()
val uid = FirebaseAuth.getInstance().currentUser!!.uid
val inventario = db.collection("users").document(uid).collection("inventory")
```

**CRUD** (dentro de una corrutina, por ejemplo `viewModelScope.launch { ... }`):

```kotlin
// Crear
val ref = inventario.add(mapOf("name" to "Leche", "category" to "DAIRY")).await()

// Leer uno
val doc = inventario.document(ref.id).get().await()
val nombre = doc.getString("name")

// Leer varios con filtro
val lacteos = inventario.whereEqualTo("category", "DAIRY").get().await()

// Actualizar
inventario.document(ref.id).update("quantity", 2).await()

// Eliminar
inventario.document(ref.id).delete().await()
```

**Escuchar cambios en tiempo real:**

```kotlin
inventario.addSnapshotListener { snapshot, error ->
    if (error != null) return@addSnapshotListener
    val nombres = snapshot!!.documents.map { it.getString("name") }
}
```

**Manejar errores:**

```kotlin
try {
    inventario.document(id).delete().await()
} catch (e: FirebaseFirestoreException) {
    // PERMISSION_DENIED si las reglas lo rechazan
}
```

> En SmartFood estas llamadas **no** van en la pantalla ni en el ViewModel: van en el **repositorio** de la capa `data`. Así la lógica no depende de Firebase y se puede probar sin internet.

---

## 6. ¿Cómo ver o editar los datos sin la app?

- **Consola de Firebase**: proyecto `smartfood-f62a1` → *Firestore Database*. Ahí se ven y editan los documentos a mano. Ojo: la consola se salta las reglas.
- **Emulador local**: `firebase emulators:start --only auth,firestore,functions` y abrir `http://localhost:4000`. Es una base de datos de prueba que no toca la real.
