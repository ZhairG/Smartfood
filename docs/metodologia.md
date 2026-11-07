# Metodología y estado de las especificaciones

El proyecto sigue Spec Driven Development: ninguna funcionalidad se programa sin especificación. Aquí están el estado de cada spec y los comandos del día a día.

## Estado

| Tareas                              | Spec | Estado |
|-------------------------------------|---|---|
| 001 Autenticación                   | [`specs/001-autenticacion`](../specs/001-autenticacion/spec.md) | ✅ Implementado (v1) |
| 002 Adaptabilidad de la UI          | [`specs/002-adaptabilidad-ui`](../specs/002-adaptabilidad-ui/spec.md) | ✅ Implementado |
| 003 Recuperación de contraseña real | [`specs/003-recuperacion-real`](../specs/003-recuperacion-real/spec.md) | ✅ Implementado y desplegado |
| 004 Correo de bienvenida            | [`specs/004-correo-registro`](../specs/004-correo-registro/spec.md) | ✅ Implementado y desplegado |
| 005 Documentos legales              | [`specs/005-documentos-legales`](../specs/005-documentos-legales/spec.md) | ✅ Implementado (texto en vista previa) |
| 006 Inventario                      | [`specs/006-inventario`](../specs/006-inventario/spec.md) | ✅ Implementado (con uso sin conexión) |
| 008 Perfil                          | [`specs/008-perfil`](../specs/008-perfil/spec.md) | ✅ Implementado |
| 009 Recetas + IA                    | [`specs/009-recetas-ia`](../specs/009-recetas-ia/spec.md) | ✅ Implementado (agente CSP de plan de 3 días; [evaluación](ia/evaluacion-agente.md)) |
| 010 Recetas guardadas               | [`specs/010-recetas-guardadas`](../specs/010-recetas-guardadas/spec.md) | Implementada (#117, #130 y #131) |

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


## Comandos

```bash
./gradlew :app:assembleDebug              # Compilar APK debug
./gradlew :app:testDebugUnitTest          # Pruebas unitarias
./gradlew :app:connectedDebugAndroidTest  # Pruebas instrumentadas (requiere emulador)
./gradlew :app:lintDebug                  # Android Lint
npm --prefix functions test               # Pruebas de las Cloud Functions
npm --prefix rules-test ci                # Una vez: dependencias de las pruebas de reglas
firebase emulators:exec --only firestore,storage "npm --prefix rules-test test"   # Pruebas de firestore.rules y storage.rules
```
