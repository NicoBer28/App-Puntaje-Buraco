package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.domain.model.Equipo

/**
 * Nombres de colecciones y campos en Firestore.
 *
 * Los perfiles y las cuentas usan el esquema nuevo (ver docs/PLAN_AUTENTICACION.md):
 *
 * ```
 * perfiles/{idPerfil}                    nombre, uid (ausente si no tiene login), creadoPor
 * perfiles/{idPerfil}/amigos/{idAmigo}   nombre, desde    siempre de a dos: uno en cada perfil
 * nombres/{nombre}                       perfil    reserva el nombre de usuario, en minúsculas
 * cuentas/{uid}                          perfil    perfil vinculado a la cuenta de acceso
 * mails/{mail}                           perfil, uid    un mail no puede tener dos perfiles
 * ```
 *
 * Las estadísticas y las partidas siguen en el esquema anterior hasta que se migren:
 *
 * ```
 * users/{idUsuario}                                 Partidas Jugadas, Partidas Ganadas
 * users/{idUsuario}/statistics/{idRival}            Partidas Jugadas, Partidas Ganadas
 * users/{idUsuario}/partidas/{idPartida}            EquipoUno, EquipoDos, Empieza, Rondas,
 *                                                   Ganador, Fecha
 * doubles/{idPareja}                                Partidas Jugadas, Partidas Ganadas
 * doubles/{idPareja}/statisticsDoubles/{idRival}    Partidas Jugadas, Partidas Ganadas
 * ```
 *
 * El id de una pareja es [Equipo.id] ("ana|zoe"). Las versiones anteriores concatenaban los ids
 * sin separador ([idParejaAnterior]); esos documentos se siguen leyendo pero ya no se escriben.
 */
internal object EsquemaFirestore {
    const val PERFILES = "perfiles"
    const val NOMBRES = "nombres"
    const val CUENTAS = "cuentas"
    const val MAILS = "mails"
    const val AMIGOS = "amigos"

    const val NOMBRE = "nombre"
    const val UID = "uid"
    const val CREADO_POR = "creadoPor"
    const val PERFIL = "perfil"
    const val DESDE = "desde"

    // Esquema anterior
    const val USUARIOS = "users"
    const val ESTADISTICAS_INDIVIDUALES = "statistics"
    const val PAREJAS = "doubles"
    const val ESTADISTICAS_PAREJAS = "statisticsDoubles"
    const val PARTIDAS = "partidas"

    const val PARTIDAS_JUGADAS = "Partidas Jugadas"
    const val PARTIDAS_GANADAS = "Partidas Ganadas"

    const val EQUIPO_UNO = "EquipoUno"
    const val EQUIPO_DOS = "EquipoDos"
    const val EMPIEZA = "Empieza"
    const val RONDAS = "Rondas"
    const val GANADOR = "Ganador"
    const val FECHA = "Fecha"
    const val BASE_UNO = "BaseUno"
    const val PUNTOS_UNO = "PuntosUno"
    const val BASE_DOS = "BaseDos"
    const val PUNTOS_DOS = "PuntosDos"

    fun idParejaAnterior(pareja: Equipo): String = pareja.jugadores.map { it.id }.sorted().joinToString("")
}
