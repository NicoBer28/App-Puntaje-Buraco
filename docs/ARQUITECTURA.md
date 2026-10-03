# Arquitectura

La app sigue **Clean Architecture + MVVM**, la arquitectura recomendada por Google para Android.
El código se divide en tres capas y las dependencias apuntan siempre hacia adentro:

```
ui  ──►  domain  ◄──  data
```

- **`domain`**: Kotlin puro (sin Android ni Firebase). Contiene las reglas del juego y de negocio.
- **`data`**: implementa las interfaces del dominio con tecnologías concretas (Firestore,
  SharedPreferences, TensorFlow Lite).
- **`ui`**: pantallas (Fragments) y su lógica de presentación (ViewModels).

Hilt (`di/`) conecta las capas: es el único lugar que sabe qué implementación usa cada interfaz.

## Estructura de paquetes

```
com.example.puntajeburaco20
├── PuntajeBuracoApp.kt           Application (punto de entrada de Hilt)
├── di/
│   ├── AppModule.kt              SharedPreferences, Firestore, Json, dispatchers
│   └── DataModule.kt             Interfaz → implementación de cada repositorio
├── domain/
│   ├── model/                    Jugador, Usuario, Equipo, Partida, Estadisticas, FichaDetectada…
│   ├── error/ErrorUsuario.kt     Errores de negocio (nombre en uso, ya es amigo, …)
│   ├── repository/               Interfaces: UsuarioRepository, EstadisticasRepository,
│   │                             SesionRepository, PartidaEnCursoRepository
│   ├── service/                  ValidadorCredenciales, CalculadoraPuntosFichas
│   └── usecase/                  Una clase por operación de negocio
├── data/
│   ├── firestore/                Repositorios de usuarios y estadísticas + EsquemaFirestore
│   ├── local/                    Sesión y partida en curso (SharedPreferences)
│   └── vision/                   DetectorFichas (interfaz) y TfliteDetectorFichas (YOLO)
└── ui/
    ├── MainActivity.kt           Solo aloja la navegación
    ├── common/                   UiText, mensajes de error, selectores de jugadores, extensiones
    ├── login/                    Iniciar sesión / registrarse
    ├── nuevapartida/             Pantalla principal: elegir jugadores
    ├── puntaje/                  Anotador de la partida (+ camara/ para detectar fichas)
    ├── amigos/                   Agregar / eliminar amigos, crear cuentas
    └── historial/                Estadísticas
```

## Flujo de una acción

Ejemplo: el usuario toca **Agregar amigo**.

1. `AmigosFragment` llama a `AmigosViewModel.agregar(nombre)`. El Fragment no tiene lógica.
2. El ViewModel ejecuta `AgregarAmigoUseCase` en una corrutina.
3. El caso de uso aplica las reglas (no vacío, no uno mismo, existe, no es amigo ya) usando
   `UsuarioRepository` (interfaz) y lanza un `ErrorUsuario` si alguna falla.
4. `FirestoreUsuarioRepository` escribe en Firestore (un batch atómico para ambos usuarios).
5. El ViewModel emite un `Evento.Mensaje` con un `UiText`. El Fragment lo muestra como Toast.

El estado de cada pantalla se expone con `StateFlow` y los eventos únicos (mensajes, navegación)
con un `Channel`. Los Fragments los recolectan solo mientras están visibles (`recolectar`).

## Cambiar de base de datos

El dominio y la UI no conocen Firestore. Para migrar (por ejemplo a Supabase, Room o una API
propia):

1. Escribir nuevas clases que implementen `UsuarioRepository` y `EstadisticasRepository`.
2. Enlazarlas en `di/DataModule.kt` en lugar de las de Firestore.

No hay que tocar ninguna pantalla ni regla de negocio. Lo mismo vale para la sesión y la partida
en curso (hoy en SharedPreferences) o para el detector de fichas.

## Navegación

`main_navigation.xml` arranca siempre en `NuevaPartidaFragment` (navegación condicional, como
recomienda Google). Esa pantalla redirige:

- al **login** si no hay sesión (al iniciar sesión, el login se cierra y se vuelve);
- a la **partida en curso** si la app se cerró en medio de una.

## Compilar

- JDK 17 o 21 (Gradle 8.10 no corre sobre JDK 25+). Android Studio usa su propio JDK.
- `app/google-services.json`: está en `.gitignore`. Se descarga de la consola de Firebase
  (proyecto `app-puntaje-burako`).
- Modelo de detección: `app/src/main/assets/best_float32.tflite` y `labels.txt`. Tampoco están en
  el repo. Sin ellos la app funciona igual, pero el botón **Cámara** muestra un aviso.

```bash
./gradlew assembleDebug        # APK de debug
./gradlew testDebugUnitTest    # tests unitarios
./gradlew lintDebug            # análisis estático
```

## Tests

Los tests unitarios (`app/src/test`) cubren el dominio (reglas de la partida, validaciones,
puntaje de fichas), los casos de uso, la serialización de la partida y los ViewModels de Puntaje
e Historial. Usan repositorios en memoria (`fakes/`), así que no necesitan Firebase ni un
emulador.
