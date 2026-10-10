# Plan: cuentas con mail, esquema nuevo y reglas de seguridad

Reemplaza el login hecho a mano (nombre + contraseña en texto plano dentro de Firestore) por
**Firebase Authentication con mail verificado**, reorganiza los datos en un **esquema nuevo** y
agrega **reglas de seguridad** a Firestore. Resuelve los puntos 1 y 2 de
[MEJORAS_PENDIENTES.md](MEJORAS_PENDIENTES.md).

Todo entra en el plan gratuito de Firebase (Spark). No hace falta backend ni Cloud Functions.

Las fases 1 a 3 se desarrollan y prueban contra el emulador de Firebase, sin tocar los datos
reales. La fase 4 migra los datos y es la única publicación obligada (**el corte**). Las fases
5 a 7 se publican después, cada una por separado.

**Estado al 10/10/2026**: las siete fases están hechas, con el corte aplicado y todas las reglas
publicadas en el proyecto real. Falta repartir el APK.

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

pedidosReclamo/{uid}                     a lo sumo uno por cuenta, con su uid como id
  perfil: "{idPerfil}"
  nombre: "Juan"                         el del perfil
  mail: "juan.perez@gmail.com"           el de quien lo pide
  creador: "pQ9…"                        uid de quien creó el perfil
  fecha: <fecha del servidor>

nombres/{nombreEnMinusculas}             reserva el nombre de usuario
  perfil: "{idPerfil}"

cuentas/{uid}
  perfil: "{idPerfil}"
  pruebaClaveVieja: "fe85…"              solo si vinculó un perfil anterior al corte

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
  hash: "9f2c…"                          ausente si el usuario no tenía contraseña
```

Notas:

- Los perfiles son de lectura pública entre usuarios, así que el mail nunca se guarda ahí. Cada
  documento de `mails/` lo lee solo el dueño de ese mail (y quien creó el reclamo, si es uno).
  La app no puede preguntarle a Firebase Auth si un mail ya está registrado; por eso hace falta
  esta colección.
- El nombre se copia en `amigos/` y en cada partida para no leer un perfil por cada jugador.
- `estadisticasPrevias/` guarda las partidas anteriores a que la app registrara el historial
  completo, que hoy existen solo como contadores.
- La contraseña vieja no viaja ni queda escrita tal cual. Al vincular, la app guarda en
  `cuentas/{uid}` una prueba: el hash de la contraseña junto con el id del perfil.
  `credencialesViejas/` tiene el hash de esa prueba, y las reglas comprueban que coincidan. Quien
  consiguiera leer `credencialesViejas/` no podría usar lo que hay ahí para vincular un perfil.
- El id de un perfil migrado tiene la forma de un id automático, pero se calcula a partir del id
  que el usuario tenía en `users/`. Así la migración da siempre el mismo resultado.

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
| `perfiles/` y `nombres/` | Cualquier usuario verificado | El dueño, que lo renombra y lo borra. Quien lo creó, mientras no tenga `uid`. Un perfil migrado, quien demuestre saber su contraseña vieja, y solo para quedárselo |
| `perfiles/{id}/amigos/` | El dueño del perfil | El dueño de cualquiera de los dos lados, y siempre los dos lados juntos |
| `partidas/` | Cualquier usuario verificado | Las crea cualquier usuario verificado, con su `uid` en `creadaPor`. Nadie las modifica |
| `cuentas/{uid}` | Esa cuenta | Esa cuenta: la crea con su perfil y la borra con él |
| `mails/{mail}` | El dueño de ese mail y quien creó el reclamo. Que un mail está libre lo ve cualquier usuario verificado | El registro de una cuenta, su dueño. Un reclamo lo crea y lo borra quien creó el perfil; el dueño del mail lo acepta (pasa a ser el registro de su cuenta) o lo borra. Un mail existente no se puede pisar |
| `estadisticasPrevias/` | Cualquier usuario verificado | Nadie |
| `pedidosReclamo/{uid}` | Esa cuenta y quien creó el perfil pedido | Lo crea esa cuenta, si no tiene perfil. Lo borran ella o quien creó el perfil. Nadie lo modifica |
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

- [x] Script de migración (`scripts/firestore/migrar.mjs`, con el Admin SDK; se corre en la
      máquina local). Lee `users/` y `doubles/` y escribe el esquema nuevo:
  - un perfil por usuario, con su nombre reservado y sus amigos;
  - `credencialesViejas/{idPerfil}` con el hash de la contraseña;
  - una sola partida por cada grupo de copias de `users/{id}/partidas`;
  - `estadisticasPrevias/` con los contadores actuales, descontando las partidas que sí tienen
    documento para no contarlas dos veces.
- [x] Parejas con el id del formato anterior (nombres pegados sin separador): el script las
      resuelve probando las parejas de usuarios existentes, y lista las que no pueda resolver.
- [x] No se migran los usuarios de prueba (`prueba`, `test`, `test2` y `test3`), ni sus amistades,
      partidas y estadísticas. Tampoco la colección `users_v2` ni el campo `Recuperar`, que son
      de versiones anteriores de la app. Las partidas que los demás jugaron contra ellos se
      descuentan de sus totales.
- [x] El script no borra `users/` ni `doubles/`: quedan como respaldo, bloqueadas por las reglas.
      Se puede correr más de una vez sin duplicar datos: un perfil o una partida que ya existen
      no se vuelven a escribir, así que tampoco pisa lo que haya cambiado desde la app.
- [x] Sin `--escribir` el script solo muestra qué haría y qué hay para revisar a mano (amistades
      de un solo lado, equipos sin identificar, contadores que no cierran).
- [x] Botón **"Ya tenía un perfil"** en el paso de elegir nombre: pide el nombre de usuario y la
      contraseña vieja. Las reglas calculan el hash de lo que la app envía y lo comparan con
      `credencialesViejas/` usando `get()`. Si coinciden, el perfil queda vinculado a la cuenta.
- [x] Si el dispositivo tenía sesión de la versión anterior (clave `usuarioActual` de DataStore),
      el acceso arranca directamente en ese paso, con el nombre ya escrito; la clave se borra
      después de vincular.
- [x] Las reglas bloquean `users/`, `doubles/` y `users_v2/`.
- [x] Probar el script completo contra el emulador, con una copia de los datos reales
      (`scripts/firestore/restaurar.mjs` carga un respaldo en el emulador).

**Listo cuando**: en el emulador, con una copia de los datos reales ya migrada, alguien que
usaba la versión anterior crea su cuenta, recupera su perfil con su contraseña vieja y ve sus
amigos y sus estadísticas.
Comprobado el 10/10/2026 con el respaldo del 9/10: 20 perfiles, 27 amistades y 60 documentos de
estadísticas; ninguna partida, porque las únicas con detalle son de usuarios de prueba. Lo único
para revisar fue un jugador con una partida más en su total que sumando por rival. En un emulador
de Android, con la sesión anterior de un usuario puesta a mano en el dispositivo, la app ofreció
recuperar ese perfil, rechazó una contraseña equivocada y con la real entró con sus amigos y sus
estadísticas.

**Orden del corte**

1. Publicar las reglas y los índices. Las versiones viejas dejan de poder leer y escribir.
2. Hacer un respaldo nuevo, ahora que los datos viejos ya no cambian.
3. Correr el script de migración: primero sin `--escribir`, para revisar los avisos.
4. Distribuir la versión nueva.

```bash
firebase deploy --only firestore
cd scripts/firestore
node respaldar.mjs --clave /ruta/a/la-clave.json
node migrar.mjs --clave /ruta/a/la-clave.json
node migrar.mjs --clave /ruta/a/la-clave.json --escribir
```

Los pasos 1 a 3 se hicieron el 10/10/2026: quedaron 20 perfiles, 27 amistades y 60 documentos de
estadísticas, todos los perfiles sin dueño y con su credencial vieja. Ese mismo día la primera
cuenta real recuperó su perfil con la versión nueva. Falta pasarle el APK al resto.

El APK que se reparte es el de debug, porque todavía no hay una clave de firma propia (ver
[MEJORAS_PENDIENTES.md](MEJORAS_PENDIENTES.md)). Android solo lo instala encima de la versión
anterior si las dos se compilaron en la misma máquina; si no, hay que desinstalar primero, y
entonces la persona tiene que tocar "Ya tenía un perfil" porque el teléfono ya no recuerda su
usuario.

**Implicaciones**

- No se pierden datos: perfiles, amigos, partidas y estadísticas pasan al esquema nuevo, y las
  colecciones viejas quedan como respaldo.
- Todos los usuarios actuales quedan como perfiles sin login hasta que cada uno vincule su mail.
  No hay fecha límite: quien vuelva a los seis meses lo hace solo.
- Quien actualiza la app en el mismo teléfono ve directamente el paso para recuperar su perfil.
  En un teléfono nuevo tiene que tocar "Ya tenía un perfil": si en cambio elige un nombre nuevo,
  arranca sin historial y ya no puede vincular el perfil viejo con esa cuenta.
- Un perfil que alguien creó para otro después del corte no tiene contraseña: hasta la fase 6 no
  se puede recuperar, y la app lo avisa.
- Nadie puede leer contraseñas, ni robar o borrar perfiles.
- Quien siga en una versión vieja no puede usar la app hasta actualizar. Lo que tuviera sin
  sincronizar se pierde.
- Las cuentas que alguien creó para otro con la versión vieja son cuentas actuales comunes: se
  vinculan con su contraseña.

## Fase 5 — Mail obligatorio y reclamo del perfil

Se puede publicar sola.

- [x] Crear cuenta para otro pide **nombre y mail**. Además del perfil se crea `mails/{mail}`
      como reclamo (sin `uid`). El mail se guarda en minúsculas, como lo deja Firebase Auth.
- [x] Si `mails/{mail}` ya existe, la creación falla: ese mail ya tiene una cuenta o un reclamo.
      La app avisa que esa persona ya tiene perfil y que hay que agregarla como amiga por su
      nombre de usuario.
- [x] Quien creó el perfil puede corregir el mail mientras nadie lo haya reclamado, desde la
      lista "Usuarios que creaste" de la pantalla Amigos. Sirve también para cargarle el mail a
      los perfiles creados antes de esta fase. Corregirlo es borrar el reclamo y crear otro,
      porque el id del reclamo es el mail.
- [x] Después de verificar el mail, la app busca `mails/{mail}`. Si hay un reclamo, ofrece el
      perfil ("Nico creó el perfil Juan para vos. Ya tiene 12 partidas anotadas."). Al aceptar se
      vincula y el reclamo pasa a ser el registro de su cuenta (queda solo con `perfil` y `uid`);
      al rechazar, el reclamo se borra y la persona elige un nombre nuevo.
- [x] Mientras no responda, la cuenta no puede crear otro perfil ni recuperar uno anterior: la
      app se lo vuelve a ofrecer.
- [x] Reglas: un reclamo lo crea cualquier usuario verificado si ese mail está libre, y solo para
      un perfil sin dueño que haya creado; lo leen quien lo creó y el dueño verificado de ese
      mail; lo borra quien lo creó, y también el dueño del mail si dice que no es suyo. Vincular
      un perfil sin login exige que exista el reclamo para el mail verificado de la cuenta.

**Listo cuando**: en el emulador alguien crea un usuario para otra persona con su mail, lo corrige,
y esa persona se registra con ese mail y se queda con el perfil o lo rechaza.
Comprobado el 10/10/2026 en un emulador de Android contra los emuladores de Auth y Firestore:
crear con un mail escrito con mayúsculas, el aviso para un mail que ya tiene cuenta, corregir el
mail, aceptar el perfil (con sus amigos y la cantidad de partidas) y rechazarlo para elegir un
nombre nuevo.

**Publicación**: las reglas se publicaron el 10/10/2026 (`firebase deploy --only firestore`); el
APK todavía no se repartió. Tienen que ir antes que el APK: con las reglas anteriores, esta
versión no puede saber si un mail está libre y avisa siempre que ya tiene perfil. Para la versión
de la fase 4 no cambian nada.

**Implicaciones**

- Hay que pedirle el mail a la persona en el momento de crearle la cuenta.
- La persona tiene que registrarse con ese mismo mail. Si usa otro, entra el caso de la fase 6.
- Mientras no exista la app para su plataforma, la persona no tiene dónde registrarse: el perfil
  queda esperando.
- Al crear una cuenta para otro se puede deducir si un mail ya está registrado en la app.
- Las reglas no pueden exigir el mail al crear el perfil, porque el perfil es público y no lo
  guarda: lo exige la app. Quien siga con la versión de la fase 4 crea perfiles sin mail, y se
  lo puede cargar después.
- Quien creó el perfil ve el mail mientras nadie lo acepte; después deja de verlo.

## Fase 6 — "Ya tenía un perfil" para perfiles creados por otro

Cubre el mail mal escrito. Se puede publicar sola.

- [x] "Ya tenía un perfil" acepta también perfiles creados por otro: con el usuario y sin
      contraseña, crea un pedido en `pedidosReclamo/{uid}`. Va en una colección propia y no
      debajo del perfil: así la cuenta que lo hizo lo encuentra por su uid, y quien creó el
      perfil lista los suyos con una sola consulta.
- [x] Quien creó el perfil ve el pedido al abrir la app, en un aviso sobre cualquier pantalla
      ("juan.perez@gmail.com dice ser Juan, el usuario que creaste"). Puede confirmarlo, negarlo
      o dejarlo para después. Aceptar equivale a corregir el mail del reclamo; rechazar borra el
      pedido.
- [x] Pantalla de espera para quien pidió, con la opción de elegir un nombre nuevo (sin heredar
      el historial). Si se lo confirman con la app abierta, pasa sola a ofrecerle el perfil; si
      se lo niegan, le avisa y vuelve a elegir.
- [x] Reglas: el pedido lo crea una cuenta sin perfil, con su mail verificado, para un perfil sin
      dueño creado por otro; lo leen ella y quien creó el perfil; lo borra cualquiera de los dos.
- [x] Una cuenta sin perfil que volvía a ingresar, o que reabría la app, se quedaba en la
      pantalla de ingreso: Firestore no avisa cuando el servidor confirma que un documento sigue
      sin existir. Es el caso de quien espera que le confirmen un pedido. Corregido.

**Listo cuando**: en el emulador alguien se registra con un mail distinto del reservado, pide el
perfil, quien lo creó lo confirma desde la app y esa persona entra con él.
Comprobado el 10/10/2026 en un emulador de Android contra los emuladores de Auth y Firestore:
pedir el perfil, que se lo nieguen con la app abierta, pedirlo de nuevo, que quien lo creó lo
confirme desde el aviso, y volver a ingresar y aceptar el perfil.

**Publicación**: las reglas se publicaron el 10/10/2026; el APK todavía no se repartió.

**Implicaciones**

- No hay notificaciones push: el pedido se ve recién cuando el creador abre la app.
- No se pueden fusionar dos perfiles. Quien elige un nombre nuevo pierde el historial del
  perfil que le habían creado.
- Quien creó el perfil ve el mail de quien lo pide: es lo único que tiene para saber si es esa
  persona.
- Cada cuenta puede tener un solo pedido a la vez. Para pedir otro perfil tiene que desistir.
- Cualquier cuenta verificada puede pedir cualquier perfil creado para otro; depende de que
  quien lo creó no confirme a un desconocido.

## Fase 7 — Renombrar y borrar perfiles

Se puede publicar sola.

- [x] **Renombrar**: reservar el nombre nuevo, liberar el anterior y actualizar `nombre` en el
      perfil y en las listas de sus amigos. El propio, desde el perfil; el de un usuario creado
      para otro, desde "Usuarios que creaste" en la pantalla Amigos, junto con su mail.
- [x] **Borrar un perfil sin login**: lo hace quien lo creó, desde la pantalla de amigos. Se
      borran el perfil, su nombre reservado, su reclamo, los pedidos que le hayan hecho y sus
      amistades.
- [x] **Borrar la cuenta propia**: desde el perfil. Además de lo anterior se borran la cuenta de
      Auth, `cuentas/{uid}` y `mails/{mail}`. Firebase pide haber iniciado sesión hace poco: la
      app pide la contraseña y la comprueba antes de borrar nada.
- [x] Reglas: renombra y borra quien creó el perfil mientras no tenga `uid`, y solo el dueño
      después. Un perfil no puede quedarse con dos nombres reservados ni con ninguno, y se borra
      junto con su nombre (y con su cuenta y su mail, si tiene dueño).
- [x] Las amistades se corrigen o se quitan de a tres por operación: las reglas consultan los dos
      perfiles de cada una y tienen un tope de documentos por operación. Al borrar van primero
      las amistades y al final el perfil, así que si se corta en el medio el perfil sigue
      existiendo y se puede volver a intentar.

**Listo cuando**: en el emulador alguien cambia su nombre, edita y borra un usuario que creó para
otro, y borra su cuenta.
Comprobado el 10/10/2026 en un emulador de Android contra los emuladores de Auth y Firestore, con
un perfil con 8 amigos: renombrarlo (el nombre cambió en las 8 listas), renombrar, cambiarle el
mail y borrar un usuario creado para otro con 6 amistades, y borrar la cuenta propia (rechazó
una contraseña equivocada; con la real no quedó ningún documento ni la cuenta de Auth) y
registrarse de nuevo con el mismo mail y el mismo nombre.

**Publicación**: las reglas se publicaron el 10/10/2026; el APK todavía no se repartió.

**Implicaciones**

- El nombre y el mail quedan libres para que los use otra persona.
- Las partidas ya jugadas no se borran: siguen en el historial de los otros jugadores, con el
  nombre que el perfil tenía en ese momento.
- Las partidas viejas conservan el nombre anterior de quien se renombró.
- Si la corrección del nombre en las listas de los amigos se corta en el medio, algunos lo siguen
  viendo con el nombre anterior. Es solo lo que se muestra: el perfil ya es el mismo para todos.
- El nombre de quien creó un perfil queda copiado en la reserva de ese perfil: si después se
  renombra, a quien se le ofrezca el perfil le va a figurar el nombre anterior.
- Los usuarios que alguien creó para otros no se borran con su cuenta. Siguen en las listas de
  sus demás amigos y su dueño todavía puede aceptarlos con el mail reservado, pero ya no queda
  nadie que pueda renombrarlos, borrarlos ni confirmar un pedido.

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
