package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.domain.model.Equipo

/**
 * Nombres de colecciones y campos en Firestore. Los de usuarios y estadísticas son los mismos que
 * usaba la versión anterior de la app, por lo que los datos existentes siguen siendo compatibles.
 *
 * ```
 * users/{idUsuario}                                 Nombre, Password, Amigos, AmigosNombre,
 *                                                   Partidas Jugadas, Partidas Ganadas
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
    const val USUARIOS = "users"
    const val ESTADISTICAS_INDIVIDUALES = "statistics"
    const val PAREJAS = "doubles"
    const val ESTADISTICAS_PAREJAS = "statisticsDoubles"
    const val PARTIDAS = "partidas"

    const val NOMBRE = "Nombre"
    const val PASSWORD = "Password"
    const val AMIGOS_IDS = "Amigos"
    const val AMIGOS_NOMBRES = "AmigosNombre"
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
