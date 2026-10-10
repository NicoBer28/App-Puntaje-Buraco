package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.domain.model.Equipo

/**
 * Nombres de colecciones y campos en Firestore (ver docs/PLAN_AUTENTICACION.md).
 *
 * ```
 * perfiles/{idPerfil}                    nombre, uid (ausente si no tiene login), creadoPor
 * perfiles/{idPerfil}/amigos/{idAmigo}   nombre, desde    siempre de a dos: uno en cada perfil
 * nombres/{nombre}                       perfil    reserva el nombre de usuario, en minúsculas
 * cuentas/{uid}                          perfil    perfil vinculado a la cuenta de acceso
 * mails/{mail}                           perfil, uid    un mail no puede tener dos perfiles
 *
 * partidas/{idPartida}                   equipoUno, equipoDos, jugadores, nombres, equipos,
 *                                        enfrentamiento, equipoGanador, empieza, rondas, fecha,
 *                                        creadaPor
 *
 * estadisticasPrevias/{equipo}                    jugadas, ganadas
 * estadisticasPrevias/{equipo}/rivales/{rival}    jugadas, ganadas
 * ```
 *
 * Un equipo se identifica por [Equipo.id]: el id del perfil, o los dos ids ordenados y unidos con
 * "|". Las estadísticas no se guardan: se cuentan las partidas, y para eso cada una repite sus
 * equipos en los campos por los que se consulta (`equipos`, `enfrentamiento`, `equipoGanador`).
 * `estadisticasPrevias` tiene los resultados anteriores a que existiera el detalle por partida.
 */
internal object EsquemaFirestore {
    const val PERFILES = "perfiles"
    const val NOMBRES = "nombres"
    const val CUENTAS = "cuentas"
    const val MAILS = "mails"
    const val AMIGOS = "amigos"
    const val PARTIDAS = "partidas"
    const val ESTADISTICAS_PREVIAS = "estadisticasPrevias"
    const val RIVALES = "rivales"

    const val NOMBRE = "nombre"
    const val UID = "uid"
    const val CREADO_POR = "creadoPor"
    const val PERFIL = "perfil"
    const val DESDE = "desde"

    const val EQUIPO_UNO = "equipoUno"
    const val EQUIPO_DOS = "equipoDos"
    const val JUGADORES = "jugadores"
    const val NOMBRES_DE_JUGADORES = "nombres"
    const val EQUIPOS = "equipos"
    const val ENFRENTAMIENTO = "enfrentamiento"
    const val EQUIPO_GANADOR = "equipoGanador"
    const val EMPIEZA = "empieza"
    const val RONDAS = "rondas"
    const val FECHA = "fecha"
    const val CREADA_POR = "creadaPor"
    const val BASE_UNO = "baseUno"
    const val PUNTOS_UNO = "puntosUno"
    const val BASE_DOS = "baseDos"
    const val PUNTOS_DOS = "puntosDos"

    const val JUGADAS = "jugadas"
    const val GANADAS = "ganadas"

    private const val SEPARADOR_ENFRENTAMIENTO = "~"

    /** Identifica a dos equipos que se enfrentan, sin importar de qué lado jugó cada uno. */
    fun claveEnfrentamiento(equipo: Equipo, rival: Equipo): String =
        listOf(equipo.id, rival.id).sorted().joinToString(SEPARADOR_ENFRENTAMIENTO)
}
