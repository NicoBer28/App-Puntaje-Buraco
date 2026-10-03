# Mejoras pendientes

Cambios grandes detectados durante el refactor de arquitectura. Se dejaron para después porque
cambian funcionalidad, requieren migrar datos de Firestore o son trabajo grande por sí mismos.
Están ordenados por prioridad.

## Seguridad y datos (prioridad alta)

1. **Contraseñas en texto plano y login en el cliente.** `users/{id}.Password` se guarda sin
   hashear y la app la compara localmente. Cualquiera con la app puede leer todas las contraseñas.
   Conviene migrar a **Firebase Authentication** (la rama `feature/copia-inicial` ya tiene un
   `AuthRepository` con email y Google) y escribir **reglas de seguridad** de Firestore. Con la
   arquitectura nueva alcanza con reimplementar `UsuarioRepository.autenticar/crear`.
2. **Ids de parejas que pueden chocar.** El id de una pareja es la concatenación de los ids de
   sus integrantes (`"ana" + "zoe" = "anazoe"`). Dos parejas distintas pueden dar el mismo id:
   `"abcd"+"efg"` y `"abc"+"defg"` son ambas `"abcdefg"`. Para no perder el historial se mantuvo el
   formato. Solución: usar un separador (`"abcd|efg"`) y migrar los documentos de `doubles`.
3. **La identidad es el nombre.** El id de usuario es el nombre en minúsculas y `AmigosNombre`
   duplica los nombres. Por eso no se puede renombrar a nadie. Con Firebase Auth convendría usar
   el `uid` como id y guardar el nombre una sola vez.
4. **Condición de carrera al registrarse.** Se verifica que el nombre esté libre y después se crea
   la cuenta, en dos pasos. Dos registros simultáneos con el mismo nombre podrían pisarse. Se
   resuelve con una transacción, o desaparece con Firebase Auth.

## Funcionalidad

5. **Cerrar sesión.** No existe: la única forma de cambiar de usuario es borrar los datos de la
   app.
6. **Historial de partidas completo.** Hoy solo se guardan contadores (jugadas y ganadas). Guardar
   cada partida con sus rondas permitiría ver el detalle, rachas y puntajes promedio.
7. **Deshacer la última ronda.** Si se carga mal un puntaje, ahora no hay forma de corregirlo.
8. **Indicar el estado de sincronización.** Firestore encola escrituras sin conexión, pero la app
   no avisa que hay datos pendientes de subir.

## Plataforma y UI

9. **`targetSdk` 35+.** Sigue en 33. Google Play exige 35 para publicar actualizaciones. Implica
   adaptar las pantallas a *edge-to-edge* (contenido detrás de las barras del sistema).
10. **Migrar la UI a Jetpack Compose + Material 3.** Los layouts usan márgenes fijos en `dp` y
    guías absolutas: en pantallas chicas o grandes se desacomodan. Los ViewModels ya exponen
    `StateFlow`, así que se pueden reutilizar tal cual con Compose. También habilitaría modo
    oscuro.
11. **Cambiar el paquete `com.example.puntajeburaco20`.** `com.example` no se puede publicar en
    Play. Requiere registrar la app de nuevo en Firebase (nuevo `google-services.json`).
12. **`uses-feature` de cámara obligatorio.** El manifest exige cámara con autofoco, lo que oculta
    la app en Play para dispositivos sin ella, aunque la cámara es opcional. Pasar a
    `android:required="false"`.
13. **Accesibilidad.** Faltan `contentDescription` y algunos botones son chicos para tocar.

## Ingeniería

14. **Versionar el modelo de detección.** `best_float32.tflite` y `labels.txt` no están en el
    repo, así que la detección no anda en un clon limpio. Opciones: Git LFS, o descargar el modelo
    al primer uso (Firebase ML / Storage).
15. **Módulos Gradle por capa** (`:domain`, `:data`, `:app`). Así el compilador impide que una capa
    use otra que no debe. Hoy la separación es por paquetes y depende de la disciplina.
16. **DataStore en lugar de SharedPreferences** para la sesión y la partida en curso. Es la API
    recomendada (asíncrona y transaccional). Solo cambian las dos clases de `data/local`.
17. **Integración continua.** Un workflow de GitHub Actions que corra `testDebugUnitTest` y
    `lintDebug` en cada push.
18. **Tests de UI** (Espresso o Compose testing) para los flujos principales.
19. **Build de release.** `isMinifyEnabled = false` y no hay configuración de firma. Al activar
    R8 hay que agregar reglas para LiteRT.
20. **Rendimiento de la cámara.** Cada cuadro genera dos `Bitmap` (conversión y rotación). Se puede
    reutilizar un buffer, o pasar la rotación al modelo.
21. **Actualizar LiteRT** a 2.2.0 o superior cuando Google corrija el conflicto de namespaces con
    AGP 9 ([issue #6965](https://github.com/google-ai-edge/LiteRT/issues/6965)).
