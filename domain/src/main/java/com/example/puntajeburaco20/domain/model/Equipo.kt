package com.example.puntajeburaco20.domain.model

/** Un jugador solo (partidas de 2) o una pareja (partidas de 4). */
data class Equipo(val jugadores: List<Jugador>) {

    init {
        require(jugadores.size in 1..2) { "Un equipo tiene 1 o 2 jugadores" }
    }

    /**
     * Identificador estable: no depende del orden en que se cargaron los integrantes. Los ids de
     * una pareja se separan con [SEPARADOR_IDS] para que dos parejas distintas no den el mismo id
     * (sin separador, "abcd"+"efg" y "abc"+"defg" serían ambas "abcdefg").
     */
    val id: String get() = jugadores.map { it.id }.sorted().joinToString(separator = SEPARADOR_IDS)

    val esPareja: Boolean get() = jugadores.size == 2

    fun incluye(jugador: Jugador): Boolean = jugadores.any { it.esMismaPersona(jugador) }

    companion object {
        /** No puede aparecer en el id de un perfil (solo tienen letras y números). */
        const val SEPARADOR_IDS = "|"
    }
}

/** Posición de un equipo dentro de una partida. */
enum class LadoEquipo {
    UNO,
    DOS;

    val rival: LadoEquipo get() = if (this == UNO) DOS else UNO
}
