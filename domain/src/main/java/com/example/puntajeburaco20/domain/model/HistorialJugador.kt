package com.example.puntajeburaco20.domain.model

import kotlin.math.roundToInt

/** Partida terminada, tal como quedó guardada en el historial. */
data class PartidaJugada(
    val partida: Partida,
    /** Momento en que terminó, en milisegundos desde el 1/1/1970 (UTC). */
    val fecha: Long,
) {
    init {
        require(partida.terminada) { "Solo se guardan partidas terminadas" }
    }

    /** `true` si el [jugador] estuvo en el equipo ganador. */
    fun gano(jugador: Jugador): Boolean = partida.ladoDe(jugador) == partida.ganador
}

/** Las últimas partidas de un jugador y lo que se puede deducir de ellas. */
class HistorialJugador(
    val jugador: Jugador,
    partidas: List<PartidaJugada>,
) {
    /** De la más reciente a la más vieja. */
    val partidas: List<PartidaJugada> = partidas.sortedByDescending { it.fecha }

    val jugadas: Int get() = partidas.size

    val ganadas: Int get() = partidas.count { it.gano(jugador) }

    /** Partidas ganadas seguidas hasta la más reciente (0 si perdió la última). */
    val rachaActual: Int get() = partidas.takeWhile { it.gano(jugador) }.size

    /** Puntaje final promedio del equipo del jugador, o `null` si no jugó ninguna partida. */
    val promedioPuntos: Int?
        get() = partidas
            .mapNotNull { jugada -> jugada.partida.ladoDe(jugador)?.let(jugada.partida::total) }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()
}
