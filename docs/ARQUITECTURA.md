# Arquitectura

La app sigue **Clean Architecture + MVVM**, la arquitectura recomendada por Google para Android.
El código se divide en tres capas y las dependencias apuntan siempre hacia adentro:

```
ui  ──►  domain  ◄──  data
```

- **`domain`**: Kotlin puro (sin Android ni Firebase). Contiene las reglas del juego y de negocio.
- **`data`**: implementa las interfaces del dominio con tecnologías concretas (Firestore,
  DataStore, LiteRT/TensorFlow Lite).
- **`ui`**: pantallas (Jetpack Compose + Material 3) y su lógica de presentación (ViewModels).

Cada capa es un **módulo Gradle**, así que el compilador hace cumplir la regla: `:domain` es un
módulo Kotlin/JVM sin dependencias de Android, `:data` es una librería Android que depende de
`:domain`, y `:app` (UI + DI) depende de ambos.

```
:app (ui, di)  ──►  :data  ──►  :domain
      └───────────────────────────►
```

Hilt (`app/.../di/`) conecta las capas: es el único lugar que sabe qué implementación usa cada
interfaz. Los SDKs que solo usa la capa de datos (Firestore, DataStore) los provee el propio
`:data` (`data/di/InfraestructuraDatosModule.kt`).

## Estructura

```
domain/   (módulo :domain, paquete com.example.puntajeburaco20.domain)
├── model/                    Jugador, Usuario, Equipo, Partida (con sus rondas), Estadisticas,
│                             PartidaJugada, HistorialJugador, FichaDetectada…
├── error/ErrorUsuario.kt     Errores de negocio (nombre en uso, ya es amigo, …)
├── repository/               Interfaces: UsuarioRepository, EstadisticasRepository,
│                             PartidasJugadasRepository, SesionRepository,
│                             PartidaEnCursoRepository, SincronizacionRepository
├── service/                  ValidadorCredenciales, CalculadoraPuntosFichas
└── usecase/                  Una clase por operación de negocio
    (src/testFixtures: repositorios en memoria que usan los tests de todas las capas)

data/     (módulo :data, paquete com.example.puntajeburaco20.data)
├── di/                       Firestore, DataStore, Json
├── firestore/                Usuarios, estadísticas, partidas jugadas, sincronización
│                             + EsquemaFirestore
├── local/                    Sesión y partida en curso (DataStore)
└── vision/                   DetectorFichas (interfaz) y TfliteDetectorFichas (YOLO sobre LiteRT)

app/      (módulo :app, paquete com.example.puntajeburaco20)
├── PuntajeBuracoApp.kt       Application (punto de entrada de Hilt)
├── di/
│   ├── AppModule.kt          Dispatchers
│   └── DataModule.kt         Interfaz → implementación de cada repositorio
└── ui/
    ├── MainActivity.kt       Activa edge-to-edge y aloja la navegación
    ├── navegacion/           Rutas y grafo de navegación (Navigation Compose)
    ├── tema/                 Colores, tipografía (Barlow) y formas: TemaBuraco
    ├── common/               Componentes compartidos (encabezado, tarjetas, selector de
    │                         jugador, tabla de rondas), UiText, mensajes de error
    ├── login/                Iniciar sesión / registrarse
    ├── nuevapartida/         Pantalla principal: elegir jugadores, cerrar sesión
    ├── puntaje/              Anotador de la partida (+ camara/ para detectar fichas)
    ├── amigos/               Agregar / eliminar amigos, crear cuentas
    ├── historial/            Estadísticas por jugador o pareja
    └── partidas/             "Mis partidas": historial ronda por ronda, racha y promedio
```

## Flujo de una acción

Ejemplo: el usuario toca **Agregar amigo**.

1. `AmigosScreen` llama a `AmigosViewModel.agregar(nombre)`. La pantalla no tiene lógica.
2. El ViewModel ejecuta `AgregarAmigoUseCase` en una corrutina.
3. El caso de uso aplica las reglas (no vacío, no uno mismo, existe, no es amigo ya) usando
   `UsuarioRepository` (interfaz) y lanza un `ErrorUsuario` si alguna falla.
4. `FirestoreUsuarioRepository` escribe en Firestore (un batch atómico para ambos usuarios).
   No espera al servidor: Firestore guarda la escritura en el dispositivo y la sube cuando hay
   conexión. Mientras tanto `SincronizacionRepository` informa que hay cambios pendientes y la
   pantalla principal lo avisa.
5. El ViewModel emite un `Evento.Mensaje` con un `UiText`. La pantalla lo muestra en un
   snackbar (`Mensajero`).

El estado de cada pantalla se expone con `StateFlow` y los eventos únicos (mensajes, navegación)
con un `Channel`. Las pantallas leen el estado con `collectAsStateWithLifecycle` y recolectan los
eventos solo mientras están visibles (`RecolectarEventos`).

## Diseño

Todas las pantallas comparten la misma estructura (`PantallaBuraco`): un encabezado azul oscuro
con esquinas redondeadas, contenido desplazable en tarjetas y, si hace falta, un pie fijo con la
acción principal. Los colores están en `ui/tema/Colores.kt`: el esquema de Material 3 (claro y
oscuro, según el sistema) más `ColoresBuraco`, con el color de cada equipo (cobalto y petróleo) y
el dorado del ganador. La tipografía es Barlow, y Barlow Condensed para títulos y puntajes
(`res/font`, licencia OFL en `docs/licencias/OFL-Barlow.txt`).

## Cambiar de base de datos

El dominio y la UI no conocen Firestore. Para migrar (por ejemplo a Supabase, Room o una API
propia):

1. Escribir nuevas clases que implementen `UsuarioRepository` y `EstadisticasRepository`.
2. Enlazarlas en `di/DataModule.kt` en lugar de las de Firestore.

No hay que tocar ninguna pantalla ni regla de negocio. Lo mismo vale para la sesión y la partida
en curso (hoy en DataStore) o para el detector de fichas.

## Compatibilidad con datos anteriores

- **Preferencias**: la primera vez que arranca, DataStore migra el archivo de SharedPreferences de
  versiones anteriores (`SharedPreferencesMigration`), así que la sesión no se cierra.
- **Partida en curso**: las versiones anteriores guardaban solo la última ronda y los totales.
  `PartidaGuardada` los sigue leyendo (las rondas previas a la última se agrupan en una).
- **Parejas**: el id pasó de `"anazoe"` a `"ana|zoe"` para evitar choques. Se escribe solo en el
  documento nuevo, y al leer se suman el nuevo y el del formato anterior.

## Navegación

`ui/navegacion/NavegacionApp.kt` arranca siempre en `NuevaPartidaScreen` (navegación
condicional, como recomienda Google). Esa pantalla redirige:

- al **login** si no hay sesión (al iniciar sesión, el login se cierra y se vuelve);
- a la **partida en curso** si la app se cerró en medio de una.

Al cerrar sesión, el estado de esa pantalla pasa a "sin sesión" y vuelve a redirigir al login.

## Compilar

- JDK 17 o superior para correr Gradle (funciona con el JDK que trae Android Studio).
- `app/google-services.json`: está en `.gitignore`. Se descarga de la consola de Firebase
  (proyecto `app-puntaje-burako`).
- Modelo de detección: `app/src/main/assets/best_float32.tflite` y `labels.txt`. Tampoco están en
  el repo. Sin ellos la app funciona igual, pero el botón **Cámara** muestra un aviso.

```bash
./gradlew assembleDebug                 # APK de debug
./gradlew :domain:test testDebugUnitTest # tests unitarios de las tres capas
./gradlew lintDebug                     # análisis estático
./gradlew connectedDebugAndroidTest     # tests de UI (necesita un emulador o dispositivo)
./gradlew assembleRelease               # APK de release (minificado con R8)
```

### Release

El build de release está minificado con R8 (`isMinifyEnabled` + `isShrinkResources`). Las reglas
para LiteRT están en `data/consumer-rules.pro` y se aplican solas a la app.

Para firmarlo, crear `keystore.properties` en la raíz (no se versiona):

```properties
storeFile=ruta/a/la/clave.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Sin ese archivo, el APK de release se genera sin firmar.

### Integración continua

`.github/workflows/android.yml` corre en cada push y pull request: tests unitarios, lint y
compilación de los tests de UI. Como `google-services.json` no está en el repo, usa una
configuración falsa (`.github/ci/google-services.json`): alcanza para compilar porque ningún test
habla con Firebase.

## Tests

- **Unitarios** (JVM, sin emulador): `domain/src/test` cubre las reglas de la partida, el
  historial, las validaciones y los casos de uso; `data/src/test` la serialización de la partida
  (incluido el formato anterior); `app/src/test` los ViewModels.
- **De UI** (`app/src/androidTest`, Compose UI Test + Hilt): recorren login, anotar y deshacer rondas,
  terminar una partida y verla en "Mis partidas", y cerrar sesión. `RepositoriosEnMemoriaModule`
  reemplaza a `DataModule`, así que no tocan Firestore. Los elementos se buscan por `testTag`.

Todos usan los mismos repositorios en memoria, publicados por `:domain` como *test fixtures*
(`domain/src/testFixtures`).

## Versiones

Todas las versiones están centralizadas en `gradle/libs.versions.toml` y apuntan a la última
estable, con dos excepciones:

- **AGP** queda en 9.3.0 porque es la última que soporta la versión instalada de Android Studio.
  Al actualizar el IDE se puede subir.
- **LiteRT**: las versiones 2.1.6 en adelante (y `litert-support`)
declaran namespaces duplicados, que AGP 9 rechaza
([issue #6965](https://github.com/google-ai-edge/LiteRT/issues/6965)). Por eso se usa la 2.1.5 y
el preprocesamiento de imágenes se hace a mano en `TfliteDetectorFichas` (girar y escalar cada
cuadro en un solo paso, sobre un bitmap reutilizado).

Para ver qué dependencias tienen versiones nuevas, correr `./gradlew lintDebug` y buscar las
advertencias `GradleDependency`.
