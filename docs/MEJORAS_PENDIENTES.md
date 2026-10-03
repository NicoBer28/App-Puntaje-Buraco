# Mejoras pendientes

Cambios grandes detectados durante el refactor de arquitectura que todavía no se hicieron, porque
requieren configurar servicios externos, decisiones de producto o trabajo grande por sí mismos.
Están ordenados por prioridad.

## Seguridad y datos (prioridad alta)

1. **Contraseñas en texto plano y login en el cliente.** `users/{id}.Password` se guarda sin
   hashear y la app la compara localmente. Cualquiera con la app puede leer todas las contraseñas.
   Conviene migrar a **Firebase Authentication** (la rama `feature/copia-inicial` ya tiene un
   `AuthRepository` con email y Google) y escribir **reglas de seguridad** de Firestore. Con la
   arquitectura nueva alcanza con reimplementar `UsuarioRepository.autenticar/crear`.
   Requiere decidir antes:
   - Habilitar el proveedor en la consola de Firebase.
   - Cómo migrar las cuentas existentes (por ejemplo, crear la cuenta de Auth la primera vez que
     cada usuario inicia sesión con su contraseña actual, y borrar el campo `Password`).
   - Firebase Auth exige contraseñas de 6 caracteres o más; hoy se permiten de 3 a 8.
   - "Crear usuario para un amigo" no encaja con Auth (crear otra cuenta cambia la sesión
     actual): habría que reemplazarlo por invitaciones o crear la cuenta desde un backend.
2. **La identidad es el nombre.** El id de usuario es el nombre en minúsculas y `AmigosNombre`
   duplica los nombres. Por eso no se puede renombrar a nadie. Con Firebase Auth convendría usar
   el `uid` como id y guardar el nombre una sola vez. Depende del punto 1.

## Plataforma y UI

3. **Cambiar el paquete `com.example.puntajeburaco20`.** `com.example` no se puede publicar en
   Play. Requiere registrar la app de nuevo en Firebase (nuevo `google-services.json`).

## Ingeniería

4. **Versionar el modelo de detección.** `best_float32.tflite` y `labels.txt` no están en el
   repo, así que la detección no anda en un clon limpio. Opciones: Git LFS, o descargar el modelo
   al primer uso (Firebase ML / Storage). Hace falta tener los archivos del modelo.
5. **Actualizar LiteRT** cuando Google corrija el conflicto de namespaces con AGP 9
   ([issue #6965](https://github.com/google-ai-edge/LiteRT/issues/6965)). Se probó la 2.2.0
   (octubre de 2026) y sigue fallando.
6. **Tests de UI en CI.** Hoy el workflow solo los compila; correrlos necesita un emulador
   (por ejemplo `reactivecircus/android-emulator-runner`), lo que hace el build bastante más lento.

## Hecho

Se resolvieron en la rama `refactor/arquitectura`:

- Ids de parejas con separador (`"ana|zoe"`). Los documentos con el formato anterior se siguen
  leyendo y se suman, así que no hizo falta migrar datos.
- Registro atómico (transacción): dos registros simultáneos con el mismo nombre no se pisan.
- Cerrar sesión.
- Historial de partidas completo, con pantalla "Mis partidas" (detalle por ronda, racha y
  promedio).
- Deshacer la última ronda.
- Aviso de datos pendientes de sincronizar.
- `targetSdk` 36 con edge-to-edge.
- Cámara opcional en el manifest y mejoras de accesibilidad.
- Módulos Gradle por capa (`:domain`, `:data`, `:app`).
- UI migrada a Jetpack Compose + Material 3, con rediseño completo y modo oscuro.
- DataStore en lugar de SharedPreferences (con migración automática de los datos guardados).
- Integración continua con GitHub Actions.
- Tests de UI (Espresso + Hilt) de los flujos principales.
- Build de release con R8 y firma configurable.
- Cámara sin crear bitmaps por cuadro.
