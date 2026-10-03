package com.example.puntajeburaco20.domain.model

/** Un jugador solo (partidas de 2) o una pareja (partidas de 4). */
data class Equipo(val jugadores: List<Jugador>) {

    init {
        require(jugadores.size in 1..2) { "Un equipo tiene 1 o 2 jugadores" }
    }

    /** Identificador estable: no depende del orden en que se cargaron los integrantes. */
    val id: String get() = jugadores.map { it.id }.sorted().joinToString(separator = "")

    val esPareja: Boolean get() = jugadores.size == 2
}

/** Posición de un equipo dentro de una partida. */
enum class LadoEquipo {
    UNO,
    DOS;

    val rival: LadoEquipo get() = if (this == UNO) DOS else UNO
}
