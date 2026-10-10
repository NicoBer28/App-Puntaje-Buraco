# Plan: cuentas con mail, esquema nuevo y reglas de seguridad

Reemplaza el login hecho a mano (nombre + contraseña en texto plano dentro de Firestore) por
**Firebase Authentication con mail verificado**, reorganiza los datos en un **esquema nuevo** y
agrega **reglas de seguridad** a Firestore. Resuelve los puntos 1 y 2 de
[MEJORAS_PENDIENTES.md](MEJORAS_PENDIENTES.md).

Todo entra en el plan gratuito de Firebase (Spark). No hace falta backend ni Cloud Functions.

Las fases 1 a 3 se desarrollan y prueban contra el emulador de Firebase, sin tocar los datos
reales. La fase 4 migra los datos y es la única publicación obligada (**el corte**). Las fases
5 a 7 se publican después, cada una por separado.

## Conceptos

- **Perfil**: el jugador. Es el documento `perfiles/{idPerfil}`, con un id automático. Las
  partidas y las amistades lo referencian por ese id, no por el nombre.
- **Cuenta**: el login de Firebase Auth (mail + contraseña). Se identifica por su `uid`.
- **Perfil sin login**: un perfil que todavía no está vinculado a ninguna cuenta. Juega y acumula
  historial, pero nadie puede entrar con él. Es el caso de alguien sin la app (por ejemplo, con
  iOS) y el de las cuentas actuales que todavía no migraron.
- **Reclamo**: un mail asociado a un perfil sin login, para que el dueño de ese mail pueda
  quedarse con el perfil cuando se registre. Se guarda en `mails/{mail}`, la misma colección que
  registra el mail de cada cuenta: así un mail nunca puede quedar asociado a dos perfiles.
- **Equipo**: un jugador solo o una pareja. Su clave es el id del perfil, o los dos ids
  ordenados y unidos con `|`.

## Decisiones tomadas

| Tema | Decisión | Por qué |
|---|---|---|
| Identidad | Id de perfil automático; el nombre de usuario se reserva en `nombres/` | Permite renombrar y borrar. Hay perfiles sin cuenta, así que el id no puede ser el `uid` |
| Inicio de sesión | Con mail y contraseña (mínimo 6 caracteres) | Lo exige Firebase Auth |
| Verificación | Obligatoria: sin mail verificado la cuenta no puede hacer nada | Evita cuentas con mails ajenos o falsos |
| Registro | En dos pasos: primero la cuenta, después el perfil | Permite reclamar un perfil existente antes de elegir nombre |
| Partidas | Un solo documento por partida. Las crea cualquier usuario verificado, participe o no | Quien anota puede no jugar. Una partida falsa queda en un único documento con autor |
| Estadísticas | Se calculan contando partidas; no se guardan contadores | Nadie escribe en los datos de otro, y no pueden quedar desfasadas |
| Amistad | Simétrica y sin aceptación. En la lista de otro, cada uno solo puede agregarse o quitarse a sí mismo. La lista de cada uno es privada | Se puede jugar con alguien sin esperar que acepte |
| Cuenta para otro | Perfil sin login, sin contraseña, con el mail de la persona obligatorio | Así después puede reclamarlo |
| Reclamo | Automático si el mail coincide; con aprobación de quien creó el perfil si no coincide | El nombre es público: no alcanza con saberlo |
| Un mail, un perfil | No se puede crear una cuenta para otro con un mail que ya tiene cuenta o reclamo | Evita perfiles duplicados, que no se pueden fusionar |
| Borrar un perfil | Sin login: solo quien lo creó. Vinculado: solo su dueño | Una vez reclamado, el perfil es de la persona |
| Migración de cuentas | Sin fecha límite. De cada contraseña vieja queda solo un hash, que solo leen las reglas | No hay forma de conseguir los mails de los usuarios actuales |
| Migración de datos | Un script reescribe todo al esquema nuevo en un único corte | Cambiar el esquema rompe las versiones viejas de todos modos |

## Esquema de datos

Reemplaza por completo al de `EsquemaFirestore.kt`. Los nombres son tentativos.

```
perfiles/{idPerfil}                      id automático
  nombre: "Ana"
  uid: "kX3…"                            ausente si no tiene login
  creadoPor: "pQ9…"                      uid; solo perfiles creados para otro

perfiles/{idPerfil}/amigos/{idAmigo}
  nombre: "Zoe"
  desde: <fecha>

perfiles/{idPerfil}/pedidosReclamo/{uid}     (fase 6)
  mail: "juan.perez@gmail.com"
  fecha: <fecha>

nombres/{nombreEnMinusculas}             reserva el nombre de usuario
  perfil: "{idPerfil}"

cuentas/{uid}
  perfil: "{idPerfil}"

mails/{mail}
  perfil: "{idPerfil}"
  uid: "kX3…"                            ausente si es un reclamo
  creadoPor, nombreCreador               solo en reclamos

partidas/{idPartida}                     un solo documento por partida
  equipoUno: ["idAna", "idZoe"]
  equipoDos: ["idJuan", "idLeo"]
  jugadores: ["idAna", "idZoe", "idJuan", "idLeo"]
  nombres: { idAna: "Ana", idZoe: "Zoe", … }
  equipos: ["idAna|idZoe", "idJuan|idLeo"]
  enfrentamiento: "idAna|idZoe~idJuan|idLeo"
  equipoGanador: "idAna|idZoe"
  empieza: "idAna"
  rondas: [{ baseUno, puntosUno, baseDos, puntosDos }]
  fecha: <fecha del servidor>
  creadaPor: "kX3…"                      uid; ausente en las partidas migradas

estadisticasPrevias/{equipo}             congeladas, solo lectura
  jugadas: 40
  ganadas: 22
estadisticasPrevias/{equipo}/rivales/{equipoRival}
  jugadas: 12
  ganadas: 7

credencialesViejas/{idPerfil}            ninguna app la lee
  hash: "9f2c…"
```

Notas:

- Los perfiles son de lectura pública entre usuarios, así que el mail nunca se guarda ahí. Cada
  documento de `mails/` lo lee solo el dueño de ese mail (y quien creó el reclamo, si es uno).
  La app no puede preguntarle a Firebase Auth si un mail ya está registrado; por eso hace falta
  esta colección.
- El nombre se copia en `amigos/` y en cada partida para no leer un perfil por cada jugador.
- `estadisticasPrevias/` guarda las partidas anteriores a que la app registrara el historial
  completo, que hoy existen solo como contadores.

### Cómo se resuelve cada consulta

| Qué se necesita | Cómo se obtiene |
|---|---|
| Perfil de la cuenta que inició sesión | Leer `cuentas/{uid}` |
| Buscar a alguien por nombre | Leer `nombres/{nombre}` |
| Mis partidas | `partidas` donde `jugadores` contiene mi id, por fecha |
| Jugadas de un equipo | Contar donde `equipos` contiene al equipo |
| Ganadas de un equipo | Contar donde `equipoGanador` es el equipo |
| Historial contra un rival | Contar por `enfrentamiento`, y por `equipoGanador` para las ganadas |

A cada conteo se le suma lo que haya en `estadisticasPrevias/`.

### Quién lee y escribe

| Dato | Quién lee | Quién escribe |
|---|---|---|
| `perfiles/` y `nombres/` | Cualquier usuario verificado | El dueño. Quien lo creó, mientras no tenga `uid` |
| `perfiles/{id}/amigos/` | El dueño del perfil | El dueño de cualquiera de los dos lados, y siempre los dos lados juntos |
| `partidas/` | Cualquier usuario verificado | Las crea cualquier usuario verificado, con su `uid` en `creadaPor`. Nadie las modifica |
| `cuentas/{uid}` | Esa cuenta | Esa cuenta |
| `mails/{mail}` | El dueño de ese mail y quien creó el reclamo | Igual. Un mail existente no se puede pisar |
| `estadisticasPrevias/` | Cualquier usuario verificado | Nadie |
| `credencialesViejas/` | Nadie | Nadie |
| `users/` y `doubles/` (esquema viejo) | Nadie | Nadie |

"Dueño de cualquiera de los dos lados" equivale a que cada uno solo puede agregarse o quitarse
a sí mismo de la lista de otro. Para un perfil sin login, el dueño es quien lo creó.

Las reglas rechazan una amistad a medias: se crea o se borra en las dos listas a la vez. Por eso
a cada uno le alcanza con leer su propia lista, y nadie necesita ver la de otro.

## Fase 0 — Preparación

No cambia nada en la app.

- [x] Habilitar el proveedor **Correo electrónico/contraseña** en la consola de Firebase.
- [x] Pasar a español las plantillas de mail (verificación y cambio de contraseña).
- [x] Agregar `firebase-auth` a `gradle/libs.versions.toml` y a `data/build.gradle.kts`.
- [x] Instalar la CLI de Firebase y crear `firebase.json`, `firestore.rules` y
      `firestore.indexes.json` en el repo. Las reglas son las publicadas hoy (todo abierto).
- [x] Configurar los emuladores de Auth y Firestore, y un build de la app que apunte a ellos
      (ver "Emuladores de Firebase" en [ARQUITECTURA.md](ARQUITECTURA.md)).
- [x] Respaldo de la base actual con `scripts/firestore/respaldar.mjs`.

## Fase 1 — Cuentas con mail y perfiles

Registro e ingreso con mail verificado, sobre las colecciones nuevas.

- [x] `AuthRepository` en `domain` y su implementación con Firebase en `data`: registrar, iniciar
      y cerrar sesión, enviar verificación, recargar el estado y recuperar contraseña. La rama
      `feature/copia-inicial` tiene un `AuthRepository.java` que sirve de referencia.
- [x] `Jugador` pasa a tener un id propio, separado del nombre. Los nombres de usuario se
      normalizan con `Usuario.claveDeNombre`.
- [x] La sesión se deriva de la cuenta de Auth y de `cuentas/{uid}`, en lugar de DataStore.
      `SesionRepository` desaparece: lo reemplaza `ObservarSesionUseCase`.
- [x] `ValidadorCredenciales`: validar formato de mail y contraseña de 6 caracteres o más. Las
      reglas del nombre de usuario no cambian.
- [x] Pantallas: ingreso con mail y contraseña, registro, "revisá tu correo" (reenviar y
      "ya verifiqué"), elegir nombre de usuario y "olvidé mi contraseña".
- [x] Navegación con cuatro estados: sin sesión, sin verificar, verificado sin perfil y completo.
      La decide la raíz de la app (`RaizApp`) según la sesión; ya no existe una ruta de login.
- [x] Al elegir nombre se crean, en una sola operación, `perfiles/{id}`, `nombres/{nombre}`,
      `cuentas/{uid}` y `mails/{mail}`. Si el nombre ya está reservado, falla.
- [x] Reglas y tests de reglas para `perfiles/`, `nombres/`, `cuentas/` y `mails/`.
- [x] Actualizar los fakes (`domain/src/testFixtures`), los tests de sesión y de validación, y
      los tests de UI de login.

**Listo cuando**: en el emulador se puede registrar una cuenta, verificarla, elegir nombre,
cerrar sesión y volver a entrar. Amigos y partidas siguen rotos hasta las fases 2 y 3.
Comprobado el 9/10/2026 en un emulador de Android contra los emuladores de Auth y Firestore.

Dos tareas de fases posteriores se adelantaron porque el cambio de `Jugador` obligaba a tocarlas:
el dominio y la pantalla de "crear usuario para otro" ya no piden contraseña (fase 2) y la
partida en curso ya guarda los ids de los jugadores (fase 3).

**Implicaciones**

- Se entra con el mail, no con el nombre de usuario.
- Aparece la recuperación de contraseña, que hoy no existe.
- Registrarse e iniciar sesión requieren conexión; después la sesión funciona offline.
- El mail de verificación puede caer en spam. Un mail mal escrito deja a la persona trabada: la
  pantalla de verificación tiene que permitir salir y registrarse de nuevo.

## Fase 2 — Amigos y cuentas para otros

- [x] `UsuarioRepository` sobre el esquema nuevo: buscar por `nombres/`, y amigos en la
      subcolección `amigos/` de cada perfil.
- [x] Agregar y quitar amigos escribe los dos lados en una sola operación, como hoy.
- [x] **Crear cuenta para otro**: `CrearUsuarioAmigoUseCase` deja de pedir contraseña. Crea un
      perfil sin login con `creadoPor`, reserva su nombre y lo agrega como amigo. El mail llega
      en la fase 5. El perfil nace ya con la amistad, en la misma operación: las reglas no
      aceptan uno que nadie tenga en su lista.
- [x] Reglas y tests de reglas para `amigos/` y para los perfiles sin login.
- [x] Actualizar los tests de amigos y el flujo de UI correspondiente.

**Listo cuando**: en el emulador dos cuentas se agregan y se quitan, y una crea un perfil para
un tercero y lo ve en su lista.
Comprobado el 9/10/2026 en un emulador de Android contra los emuladores de Auth y Firestore,
también sin conexión (la amistad se guarda en el dispositivo y sube al volver la señal).

**Implicaciones**

- Buscar a alguien por su nombre necesita conexión, salvo que ya se lo haya buscado antes en ese
  dispositivo. Crear un usuario para otro necesita conexión siempre.
- Quien creó un perfil sin login puede quitarlo de la lista de cualquiera, porque responde por él.

## Fase 3 — Partidas y estadísticas

- [x] `PartidasJugadasRepository`: guardar una partida es crear un solo documento en `partidas/`.
      "Mis partidas" consulta por `jugadores`.
- [x] `EstadisticasRepository`: las estadísticas se obtienen contando partidas y sumando
      `estadisticasPrevias/`. Desaparece `registrarResultado`: `RegistrarResultadoPartidaUseCase`
      solo guarda la partida.
- [x] Índice compuesto para "Mis partidas" (`jugadores` + `fecha`) en `firestore.indexes.json`.
      Hay otro para el historial contra un rival (`enfrentamiento` + `equipoGanador`), por las
      dudas: el emulador no exige índices, así que recién al publicar se sabe si hacía falta.
- [x] Partida en curso (`PartidaGuardada`): guarda los ids de los jugadores. Una partida
      empezada con una versión anterior, que solo tenía nombres, se descarta al abrir la app.
- [x] Pantalla de estadísticas: sin conexión avisa que hacen falta datos del servidor y deja a
      la vista el último resultado consultado.
- [x] Reglas y tests de reglas para `partidas/` y `estadisticasPrevias/`.
- [x] Actualizar los tests de puntaje, historial, estadísticas y "Mis partidas".

**Listo cuando**: en el emulador se juega una partida completa y aparece en "Mis partidas" y en
las estadísticas de todos los jugadores.
Comprobado el 9/10/2026 en un emulador de Android contra los emuladores de Auth y Firestore: de
a dos y de a cuatro, con estadísticas previas cargadas a mano, desde las cuentas de los dos
jugadores y terminando una partida sin conexión.

**Implicaciones**

- Crear y terminar partidas sigue funcionando sin conexión: la partida se guarda en el
  dispositivo y se sube cuando hay señal. Los jugadores disponibles salen de la copia local del
  perfil y los amigos.
- Las estadísticas necesitan conexión: los conteos de Firestore no funcionan offline.
- Se puede anotar una partida en la que uno no juega. No aparece en "Mis partidas" de quien la
  anotó, igual que hoy.
- Si el servidor rechaza una partida guardada sin conexión, se pierde sin aviso. Hoy pasa lo
  mismo, pero con reglas hay más motivos de rechazo.
- Las reglas comprueban que una partida sea coherente (equipos, ganador, campos de consulta),
  pero no que los jugadores existan ni que sus nombres sean los reales. Comprobarlo haría que
  una partida se pierda si alguien cambia de nombre o borra su perfil mientras se juega.
- La app ya no lee ni escribe `users/` ni `doubles/`. Esas colecciones quedan solo para el
  script de migración de la fase 4.

## Fase 4 — Migración y corte

**Publicación obligada.** A partir de acá las versiones anteriores de la app dejan de funcionar.

- [ ] Script de migración (Admin SDK, se corre en la máquina local). Lee `users/` y `doubles/` y
      escribe el esquema nuevo:
  - un perfil por usuario, con su nombre reservado y sus amigos;
  - `credencialesViejas/{idPerfil}` con el hash de la contraseña;
  - una sola partida por cada grupo de copias de `users/{id}/partidas`;
  - `estadisticasPrevias/` con los contadores actuales, descontando las partidas que sí tienen
    documento para no contarlas dos veces.
- [ ] Parejas con el id del formato anterior (nombres pegados sin separador): el script las
      resuelve probando las parejas de usuarios existentes, y lista las que no pueda resolver.
- [ ] No se migran los usuarios de prueba (`prueba`, `test`, `test2` y `test3`), ni sus amistades,
      partidas y estadísticas. Tampoco la colección `users_v2` ni el campo `Recuperar`, que son
      de versiones anteriores de la app.
- [ ] El script no borra `users/` ni `doubles/`: quedan como respaldo, bloqueadas por las reglas.
      Se puede correr más de una vez sin duplicar datos.
- [ ] Botón **"Ya tenía un perfil"** en el paso de elegir nombre: pide el nombre de usuario y la
      contraseña vieja. Las reglas calculan el hash de lo que la persona escribe y lo comparan con
      `credencialesViejas/` usando `get()`. Si coinciden, el perfil queda vinculado a la cuenta.
- [ ] Si el dispositivo tenía sesión de la versión anterior (clave `usuarioActual` de DataStore),
      precargar ese nombre y borrar la clave después de vincular.
- [ ] Probar el script completo contra el emulador, con una copia de los datos reales.

**Orden del corte**

1. Publicar las reglas y los índices. Las versiones viejas dejan de poder leer y escribir.
2. Correr el script de migración.
3. Distribuir la versión nueva.

**Implicaciones**

- No se pierden datos: perfiles, amigos, partidas y estadísticas pasan al esquema nuevo, y las
  colecciones viejas quedan como respaldo.
- Todos los usuarios actuales quedan como perfiles sin login hasta que cada uno vincule su mail.
  No hay fecha límite: quien vuelva a los seis meses lo hace solo.
- Nadie puede leer contraseñas, ni robar o borrar perfiles.
- Quien siga en una versión vieja no puede usar la app hasta actualizar. Lo que tuviera sin
  sincronizar se pierde.
- Las cuentas que alguien creó para otro con la versión vieja son cuentas actuales comunes: se
  vinculan con su contraseña.

## Fase 5 — Mail obligatorio y reclamo del perfil

Se puede publicar sola.

- [ ] Crear cuenta para otro pide **nombre y mail**. Además del perfil se crea `mails/{mail}`
      como reclamo (sin `uid`).
- [ ] Si `mails/{mail}` ya existe, la creación falla: ese mail ya tiene una cuenta o un reclamo.
      La app avisa que esa persona ya tiene perfil y que hay que agregarla como amiga por su
      nombre de usuario.
- [ ] Quien creó el perfil puede corregir el mail mientras nadie lo haya reclamado. Sirve también
      para cargarle el mail a los perfiles creados antes de esta fase.
- [ ] Después de verificar el mail, la app busca `mails/{mail}`. Si hay un reclamo, ofrece el
      perfil ("Nico te creó el perfil Juan, con 12 partidas. ¿Es tuyo?"). Al aceptar se vincula
      y el reclamo pasa a ser el registro de su cuenta (se le escribe `uid`); al rechazar, el
      reclamo se borra y la persona elige un nombre nuevo.
- [ ] Reglas: un reclamo lo crea cualquier usuario verificado si ese mail está libre; lo leen
      quien lo creó y el dueño verificado de ese mail; lo corrige o borra quien lo creó. Vincular
      un perfil sin login exige que exista el reclamo para el mail verificado de la cuenta.

**Implicaciones**

- Hay que pedirle el mail a la persona en el momento de crearle la cuenta.
- La persona tiene que registrarse con ese mismo mail. Si usa otro, entra el caso de la fase 6.
- Mientras no exista la app para su plataforma, la persona no tiene dónde registrarse: el perfil
  queda esperando.
- Al crear una cuenta para otro se puede deducir si un mail ya está registrado en la app.

## Fase 6 — "Ya tenía un perfil" para perfiles creados por otro

Cubre el mail mal escrito. Se puede publicar sola.

- [ ] "Ya tenía un perfil" acepta también perfiles creados por otro: en lugar de pedir
      contraseña, crea un pedido en `perfiles/{idPerfil}/pedidosReclamo/{uid}`.
- [ ] Quien creó el perfil ve el pedido al abrir la app ("juan.perez@gmail.com dice ser Juan.
      ¿Es él?"). Aceptar equivale a corregir el mail del reclamo; rechazar borra el pedido.
- [ ] Pantalla de espera para quien pidió, con la opción de elegir un nombre nuevo (sin heredar
      el historial).
- [ ] Reglas: el pedido lo crea quien lo pide con su mail verificado; lo lee y lo resuelve quien
      creó el perfil.

**Implicaciones**

- No hay notificaciones push: el pedido se ve recién cuando el creador abre la app.
- No se pueden fusionar dos perfiles. Quien elige un nombre nuevo pierde el historial del
  perfil que le habían creado.

## Fase 7 — Renombrar y borrar perfiles

Se puede publicar sola.

- [ ] **Renombrar**: reservar el nombre nuevo, liberar el anterior y actualizar `nombre` en el
      perfil y en las listas de sus amigos.
- [ ] **Borrar un perfil sin login**: lo hace quien lo creó, desde la pantalla de amigos. Se
      borran el perfil, su nombre reservado, su reclamo y sus amistades.
- [ ] **Borrar la cuenta propia**: desde el perfil. Además de lo anterior se borran la cuenta de
      Auth, `cuentas/{uid}` y `mails/{mail}`. Firebase pide haber iniciado sesión hace poco.
- [ ] Reglas: borra quien creó el perfil mientras no tenga `uid`, y solo el dueño después.

**Implicaciones**

- El nombre y el mail quedan libres para que los use otra persona.
- Las partidas ya jugadas no se borran: siguen en el historial de los otros jugadores, con el
  nombre que el perfil tenía en ese momento.
- Las partidas viejas conservan el nombre anterior de quien se renombró.

## Cómo queda el registro al final

Después de registrarse y verificar el mail:

| Situación | Qué pasa |
|---|---|
| Hay un reclamo para ese mail | Se le ofrece el perfil directamente |
| Toca "Ya tenía un perfil" y es una cuenta vieja | Pone su contraseña vieja y se vincula al instante |
| Toca "Ya tenía un perfil" y se lo creó otro | Se envía el pedido a quien lo creó |
| Ninguna de las anteriores | Elige un nombre de usuario nuevo |

## Riesgos aceptados

- **Partidas falsas**: cualquier usuario verificado puede crear una partida que nombre a otro, y
  ponerle el nombre que quiera. Es el costo de poder jugar con alguien sin esperar que acepte.
  Queda en un único documento, con su autor en `creadaPor`.
- **Amigos no elegidos**: cualquiera puede agregarse a la lista de otro. Se lo puede quitar, pero
  puede volver a agregarse.
- **Fuerza bruta sobre contraseñas viejas**: nadie las lee, pero un usuario verificado puede
  probar una tras otra. Con contraseñas de 3 a 8 caracteres es posible para alguien decidido.
- **Nombres ocupados**: un perfil creado para otro ocupa ese nombre hasta que se lo borre o
  renombre.

## Más adelante

- Ingreso con Google (el mail ya viene verificado).
- Lista de bloqueados, si los amigos no elegidos molestan.
- App Check o un límite de intentos, para la fuerza bruta sobre contraseñas viejas.
- Ocultar una partida del historial propio, si las partidas falsas molestan.
- Borrar `users/` y `doubles/` cuando el esquema nuevo lleve un tiempo funcionando bien.
