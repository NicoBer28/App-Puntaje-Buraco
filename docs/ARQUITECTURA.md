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
interfaz. Los SDKs que solo usa la capa de datos (Firebase Auth, Firestore, DataStore) los
provee el propio `:data` (`data/di/InfraestructuraDatosModule.kt`).

## Estructura

```
domain/   (módulo :domain, paquete com.example.puntajeburaco20.domain)
├── model/                    Jugador, Usuario, Cuenta, EstadoSesion, Equipo, Partida (con sus
│                             rondas), Estadisticas, PartidaJugada, HistorialJugador, ModoTema…
├── error/ErrorUsuario.kt     Errores de negocio (nombre en uso, mail sin verificar, …)
├── repository/               Interfaces: AuthRepository, UsuarioRepository,
│                             EstadisticasRepository, PartidasJugadasRepository,
│                             PartidaEnCursoRepository, SincronizacionRepository,
│                             PreferenciasRepository, SesionAnteriorRepository
├── service/                  ValidadorCredenciales, CalculadoraPuntosFichas
└── usecase/                  Una clase por operación de negocio
    (src/testFixtures: repositorios en memoria que usan los tests de todas las capas)

data/     (módulo :data, paquete com.example.puntajeburaco20.data)
├── di/                       Firebase Auth, Firestore, DataStore, Json
├── auth/                     Cuentas de acceso con Firebase Authentication
├── firestore/                Perfiles, estadísticas, partidas jugadas, sincronización
│                             + EsquemaFirestore
├── local/                    Partida en curso, preferencias y sesión de la versión anterior
│                             (DataStore)
└── vision/                   DetectorFichas (interfaz) y TfliteDetectorFichas (YOLO sobre LiteRT)

app/      (módulo :app, paquete com.example.puntajeburaco20)
├── PuntajeBuracoApp.kt       Application (punto de entrada de Hilt)
├── di/
│   ├── AppModule.kt          Dispatchers
│   └── DataModule.kt         Interfaz → implementación de cada repositorio
└── ui/
    ├── MainActivity.kt       Activa edge-to-edge, aplica el tema elegido y aloja la navegación
    ├── navegacion/           Raíz de la app y grafo de navegación (Navigation Compose)
    ├── sesion/               Estado de la sesión para toda la app
    ├── tema/                 Colores, tipografía (Barlow) y formas: TemaBuraco; tema elegido
    ├── common/               Componentes compartidos (encabezado, tarjetas, campos de texto y
    │                         de contraseña, selector de jugador, tabla de rondas), UiText,
    │                         mensajes de error
    ├── login/                Acceso: ingresar, crear cuenta, verificar el mail, elegir nombre
    │                         o recuperar el perfil que ya se tenía
    ├── nuevapartida/         Pantalla principal: elegir jugadores
    ├── perfil/               Datos de la cuenta, tema claro u oscuro, cerrar sesión
    ├── puntaje/              Anotador de la partida (+ camara/ para detectar fichas)
    ├── amigos/               Agregar / eliminar amigos, crear usuarios para otros
    ├── historial/            Estadísticas por jugador o pareja
    └── partidas/             "Mis partidas": historial ronda por ronda, racha y promedio
```

## Flujo de una acción

Ejemplo: el usuario toca **Agregar amigo**.

1. `AmigosScreen` llama a `AmigosViewModel.agregar(nombre)`. La pantalla no tiene lógica.
2. El ViewModel ejecuta `AgregarAmigoUseCase` en una corrutina.
3. El caso de uso aplica las reglas (no vacío, no uno mismo, existe, no es amigo ya) usando
   `UsuarioRepository` (interfaz) y lanza un `ErrorUsuario` si alguna falla.
4. `FirestoreUsuarioRepository` escribe en Firestore (un lote atómico con la amistad en la lista
   de cada uno). No espera al servidor: Firestore guarda la escritura en el dispositivo y la sube
   cuando hay conexión. Mientras tanto `SincronizacionRepository` informa que hay cambios
   pendientes y la pantalla principal lo avisa.
5. El ViewModel emite un `Evento.Mensaje` con un `UiText`. La pantalla lo muestra en un
   snackbar (`Mensajero`).

El estado de cada pantalla se expone con `StateFlow` y los eventos únicos (mensajes, navegación)
con un `Channel`. Las pantallas leen el estado con `collectAsStateWithLifecycle` y recolectan los
eventos solo mientras están visibles (`RecolectarEventos`).

## Diseño

Todas las pantallas comparten la misma estructura (`PantallaBuraco`): un encabezado azul oscuro
con esquinas redondeadas, contenido desplazable en tarjetas y, si hace falta, un pie fijo con la
acción principal. Los colores están en `ui/tema/Colores.kt`: el esquema de Material 3 (claro y
oscuro) más `ColoresBuraco`, con el color de cada equipo (cobalto y petróleo) y
el dorado del ganador. La tipografía es Barlow, y Barlow Condensed para títulos y puntajes
(`res/font`, licencia OFL en `docs/licencias/OFL-Barlow.txt`).

El tema sigue al sistema salvo que el usuario elija claro u oscuro en su perfil. La elección se
guarda en el dispositivo (`PreferenciasRepository`) y `MainActivity` la aplica a toda la app.

## Cambiar de base de datos

El dominio y la UI no conocen Firestore. Para migrar (por ejemplo a Supabase, Room o una API
propia):

1. Escribir nuevas clases que implementen `UsuarioRepository`, `EstadisticasRepository` y
   `PartidasJugadasRepository`.
2. Enlazarlas en `di/DataModule.kt` en lugar de las de Firestore.

No hay que tocar ninguna pantalla ni regla de negocio. Lo mismo vale para las cuentas de acceso
(`AuthRepository`, hoy con Firebase Authentication), la partida en curso (hoy en DataStore) o el
detector de fichas.

## Cuentas, perfiles y sesión

El plan completo de este cambio, con sus fases, está en
[PLAN_AUTENTICACION.md](PLAN_AUTENTICACION.md).

- Una **cuenta** (`Cuenta`) es el mail y la contraseña con los que alguien entra. Las maneja
  `AuthRepository`.
- Un **perfil** (`Usuario`) es el jugador: su nombre de usuario y sus amigos. Tiene un id propio,
  distinto del nombre y del de la cuenta, y lo maneja `UsuarioRepository`.
- `ObservarSesionUseCase` combina ambos en un `EstadoSesion`: sin sesión, sin verificar el mail,
  sin perfil (todavía no eligió nombre) o completa.

Hay perfiles **sin login**: los crea un usuario para alguien que no usa la app, y quedan a su
cargo. Las amistades son siempre de a dos (cada una figura en la lista de ambos perfiles) y no
necesitan que el otro acepte; la lista de amigos de cada uno es privada.

Las reglas de seguridad (`firestore.rules`) exigen mail verificado, que el perfil, su nombre
reservado, su cuenta y su mail se creen juntos, y que una amistad se cree o se borre en las dos
listas a la vez. Sus tests están en `scripts/firestore/reglas/`.

Los perfiles que existían antes de las cuentas con mail los creó la migración, sin dueño. Quien
usaba uno lo recupera desde el paso de elegir nombre ("Ya tenía un perfil"), con su usuario y la
contraseña que tenía (`VincularPerfilAnteriorUseCase`). La contraseña no se envía: la app manda
un hash (`EsquemaFirestore.pruebaDeClaveVieja`) y las reglas lo comparan con el que dejó la
migración en `credencialesViejas/`, que nadie puede leer.

## Partidas y estadísticas

Una partida terminada es un único documento de `partidas/`, que comparten todos sus jugadores y
que nadie modifica después. Guardarla no espera al servidor, así que funciona sin conexión.

Las estadísticas no se guardan: `FirestoreEstadisticasRepository` cuenta en el servidor las
partidas que jugó y ganó cada equipo, y les suma `estadisticasPrevias/` (los resultados
anteriores a que existiera el detalle por partida). Por eso consultarlas necesita conexión,
mientras que "Mis partidas" se puede ver con la copia que Firestore guarda en el dispositivo.

## Compatibilidad con datos anteriores

- **Preferencias**: la primera vez que arranca, DataStore migra el archivo de SharedPreferences de
  versiones anteriores (`SharedPreferencesMigration`).
- **Partida en curso**: las versiones anteriores identificaban a los jugadores por su nombre. Una
  partida guardada así no se puede retomar: se descarta al abrir la app.
- **Sesión**: las versiones anteriores guardaban en DataStore el usuario con la sesión iniciada.
  La app ya no lo usa para entrar, pero si lo encuentra (`SesionAnteriorRepository`) ofrece
  recuperar ese perfil en lugar de elegir un nombre nuevo.
- **Base de datos**: la app usa solo el esquema nuevo (`EsquemaFirestore`). Los datos de las
  versiones anteriores (`users/`, `doubles/`) los pasa el script de migración (ver "Scripts de
  Firestore").

## Navegación

`RaizApp` (`ui/navegacion/NavegacionApp.kt`) decide qué se muestra según el estado de la sesión:

- sin sesión completa, el **acceso** (`LoginScreen`), que a su vez muestra el paso que
  corresponda: ingresar o crear la cuenta, verificar el mail, y elegir el nombre de usuario o
  recuperar el perfil que ya se tenía;
- con sesión completa, el grafo de **pantallas** (`NavegacionApp`), que arranca siempre en
  `NuevaPartidaScreen` y lleva a la partida en curso si la app se cerró en medio de una.

Ninguna pantalla navega al acceso ni vuelve de él: alcanza con que cambie la sesión. Por eso
cerrar sesión desde el **perfil** es solo cerrar la sesión.

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
./gradlew :app:connectedDebugAndroidTest # tests de UI (necesita un emulador o dispositivo)
./gradlew assembleRelease               # APK de release (minificado con R8)

# tests de las reglas de seguridad de Firestore (la primera vez: npm install en scripts/firestore)
firebase emulators:exec --only firestore "npm --prefix scripts/firestore run reglas"
npm --prefix scripts/firestore test     # tests del script de migración (no necesitan emulador)
```

### Emuladores de Firebase

Para probar sin tocar los datos reales, el build de debug puede apuntar a los emuladores de Auth
y Firestore (configurados en `firebase.json`). Hace falta la CLI de Firebase
(`npm install -g firebase-tools`).

```bash
firebase emulators:start                                    # consola en http://localhost:4000
./gradlew installDebug -PemuladoresFirebase=10.0.2.2        # app apuntando a los emuladores
```

`10.0.2.2` es la máquina anfitriona vista desde el emulador de Android. En un dispositivo físico
se usa `localhost`, después de correr `adb reverse tcp:8080 tcp:8080` y
`adb reverse tcp:9099 tcp:9099`. Para no pasar el parámetro cada vez (por ejemplo, desde Android
Studio) se puede poner `emuladoresFirebase=10.0.2.2` en `local.properties`.

Sin ese parámetro la app usa el proyecto real, y el build de release lo ignora siempre. Los
emuladores arrancan vacíos; para conservar los datos entre ejecuciones:

```bash
firebase emulators:start --export-on-exit .emuladores                       # la primera vez
firebase emulators:start --import .emuladores --export-on-exit .emuladores  # las siguientes
```

Las reglas (`firestore.rules`) y los índices (`firestore.indexes.json`) se versionan en el repo y
se publican con `firebase deploy --only firestore`.

### Scripts de Firestore

Están en `scripts/firestore` (Node; la primera vez, `npm install`). Usan el Admin SDK, que no pasa
por las reglas de seguridad: contra el proyecto real necesitan la clave de una cuenta de servicio
(`--clave`), y con `FIRESTORE_EMULATOR_HOST` definida trabajan sobre el emulador.

```bash
node respaldar.mjs --clave clave.json                 # descarga toda la base a respaldos/
node migrar.mjs --clave clave.json                    # muestra qué haría la migración
node migrar.mjs --clave clave.json --escribir         # pasa users/ y doubles/ al esquema nuevo

# probar la migración con una copia de los datos reales
firebase emulators:start
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node restaurar.mjs respaldos/respaldo-….json
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node migrar.mjs --escribir
```

`restaurar.mjs` solo escribe en el emulador. La conversión de los datos está en
`migracion/plan.mjs`, que no habla con Firestore y tiene sus propios tests. Los respaldos y las
claves no se versionan.

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
- **De UI** (`app/src/androidTest`, Compose UI Test + Hilt): recorren ingresar, crear una cuenta
  (verificar el mail y elegir nombre), recuperar un perfil anterior, anotar y deshacer rondas,
  terminar una partida y verla en "Mis partidas", y cerrar sesión. `RepositoriosEnMemoriaModule`
  reemplaza a `DataModule`, así que no tocan Firebase. Los elementos se buscan por `testTag`.
- **De reglas de seguridad** (`scripts/firestore/reglas`, Node + emulador de Firestore): prueban
  `firestore.rules` haciendo de distintas cuentas, verificadas o no.
- **De la migración** (`scripts/firestore/migracion`, Node): la conversión del esquema anterior al
  nuevo, y que el hash de la contraseña vieja sea el mismo que calcula la app.

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
