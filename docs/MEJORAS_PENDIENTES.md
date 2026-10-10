# Mejoras pendientes

Cambios grandes detectados durante el refactor de arquitectura que todavía no se hicieron, porque
requieren configurar servicios externos, decisiones de producto o trabajo grande por sí mismos.
Están ordenados por prioridad.

## Seguridad y datos (prioridad alta)

1. **Terminar el plan de cuentas.** De [PLAN_AUTENTICACION.md](PLAN_AUTENTICACION.md) faltan las
   fases 6 y 7: pedidos de reclamo cuando el mail no coincide, y renombrar y borrar perfiles.
   Después, repartir el APK.
2. **Borrar el esquema anterior.** `users/`, `doubles/` y `users_v2/` siguen en la base como
   respaldo de la migración, con las contraseñas viejas sin hashear. Las reglas los bloquean
   para todos, pero conviene borrarlos cuando el esquema nuevo lleve un tiempo funcionando bien
   (antes, hacer un respaldo con `scripts/firestore/respaldar.mjs`).

## Plataforma y UI

3. **Cambiar el paquete `com.example.puntajeburaco20`.** `com.example` no se puede publicar en
   Play. Requiere registrar la app de nuevo en Firebase (nuevo `google-services.json`).
4. **Clave de firma propia.** No hay `keystore.properties`, así que el APK que se reparte es el
   de debug, firmado con la clave de debug de la máquina que lo compiló. Un APK compilado en otra
   máquina no se instala encima del anterior: hay que desinstalar primero. Conviene crear una
   clave de release, guardarla fuera del repo y usarla siempre. El build de release todavía no se
   probó con las cuentas nuevas.

## Ingeniería

5. **Versionar el modelo de detección.** `best_float32.tflite` y `labels.txt` no están en el
   repo, así que la detección no anda en un clon limpio. Opciones: Git LFS, o descargar el modelo
   al primer uso (Firebase ML / Storage). Hace falta tener los archivos del modelo.
6. **Actualizar LiteRT** cuando Google corrija el conflicto de namespaces con AGP 9
   ([issue #6965](https://github.com/google-ai-edge/LiteRT/issues/6965)). Se probó la 2.2.0
   (octubre de 2026) y sigue fallando.
7. **Más tests en CI.** Hoy el workflow solo compila los tests de UI; correrlos necesita un
   emulador (por ejemplo `reactivecircus/android-emulator-runner`), lo que hace el build bastante
   más lento. Tampoco corre los tests de las reglas de seguridad (necesitan el emulador de
   Firestore) ni los del script de migración (Node).

## Hecho

Se resolvieron en la rama `feature/cuentas-y-esquema-nuevo` (fases 0 a 5 de
[PLAN_AUTENTICACION.md](PLAN_AUTENTICACION.md)):

- Cuentas con Firebase Authentication: mail verificado, contraseña de 6 caracteres o más y
  recuperación por mail. La app ya no guarda ni compara contraseñas.
- Reglas de seguridad de Firestore, con tests.
- Cada jugador tiene un id de perfil propio, separado del nombre.
- "Crear usuario para un amigo" crea un perfil sin cuenta, a cargo de quien lo creó y reservado
  para el mail de esa persona, que lo recibe cuando se registra con ese mail.
- Una partida es un solo documento y las estadísticas se calculan contándolas.
- Migración de los datos anteriores al esquema nuevo. Quien ya usaba la app recupera su perfil
  con la contraseña que tenía.
- El campo de contraseña muestra cada carácter al escribirlo y tiene un botón para verla entera.

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
