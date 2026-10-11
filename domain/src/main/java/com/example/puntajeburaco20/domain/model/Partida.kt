package com.example.puntajeburaco20.domain.model

/** Lo que anotó un equipo en una ronda. */
data class PuntajeRonda(
    val base: Int,
    val puntos: Int,
) {
    val total: Int get() = base + puntos

    companion object {
        val CERO = PuntajeRonda(base = 0, puntos = 0)
    }
}

/** Una ronda completa: lo que anotó cada equipo. */
data class Ronda(
    val equipoUno: PuntajeRonda,
    val equipoDos: PuntajeRonda,
) {
    fun de(lado: LadoEquipo): PuntajeRonda = when (lado) {
        LadoEquipo.UNO -> equipoUno
        LadoEquipo.DOS -> equipoDos
    }

    companion object {
        val CERO = Ronda(PuntajeRonda.CERO, PuntajeRonda.CERO)
    }
}

/**
 * Estado de una partida de Buraco. Es inmutable: cada operación devuelve una partida nueva.
 *
 * En partidas de 4 el equipo uno son los dos primeros jugadores elegidos y el equipo dos los
 * dos últimos. La ronda la empieza cada jugador por turno, alternando equipos.
 */
data class Partida(
    val equipoUno: Equipo,
    val equipoDos: Equipo,
    /** Quién empieza la próxima ronda. */
    val empieza: Jugador,
    val rondas: List<Ronda> = emptyList(),
    val ganador: LadoEquipo? = null,
) {

    init {
        require(equipoUno.jugadores.size == equipoDos.jugadores.size) {
            "Los equipos deben tener la misma cantidad de jugadores"
        }
    }

    val modo: ModoJuego get() = if (equipoUno.esPareja) ModoJuego.PAREJAS else ModoJuego.INDIVIDUAL

    val terminada: Boolean get() = ganador != null

    /** La última ronda anotada, o una en cero si todavía no se jugó ninguna. */
    val ultimaRonda: Ronda get() = rondas.lastOrNull() ?: Ronda.CERO

    val totalUno: Int get() = total(LadoEquipo.UNO)

    val totalDos: Int get() = total(LadoEquipo.DOS)

    /** Solo se puede corregir una partida en juego que tenga alguna ronda anotada. */
    val sePuedeDeshacer: Boolean get() = !terminada && rondas.isNotEmpty()

    /** Orden en que los jugadores se turnan para empezar cada ronda. */
    val ordenDeInicio: List<Jugador>
        get() = when (modo) {
            ModoJuego.INDIVIDUAL -> listOf(equipoUno.jugadores[0], equipoDos.jugadores[0])
            ModoJuego.PAREJAS -> listOf(
                equipoUno.jugadores[0],
                equipoDos.jugadores[0],
                equipoUno.jugadores[1],
                equipoDos.jugadores[1],
            )
        }

    fun equipo(lado: LadoEquipo): Equipo = when (lado) {
        LadoEquipo.UNO -> equipoUno
        LadoEquipo.DOS -> equipoDos
    }

    fun total(lado: LadoEquipo): Int = rondas.sumOf { it.de(lado).total }

    /** Lado en el que juega el [jugador], o `null` si no participa de la partida. */
    fun ladoDe(jugador: Jugador): LadoEquipo? = LadoEquipo.entries.firstOrNull { equipo(it).incluye(jugador) }

    fun registrarRonda(rondaUno: PuntajeRonda, rondaDos: PuntajeRonda): Partida {
        check(!terminada) { "La partida ya terminó" }
        return copy(
            rondas = rondas + Ronda(rondaUno, rondaDos),
            empieza = turnoDesplazado(1),
        )
    }

    /** Quita la última ronda anotada y le devuelve el turno de empezar a quien la había empezado. */
    fun deshacerUltimaRonda(): Partida {
        check(sePuedeDeshacer) { "No hay una ronda para deshacer" }
        return copy(
            rondas = rondas.dropLast(1),
            empieza = turnoDesplazado(-1),
        )
    }

    fun finalizar(ganador: LadoEquipo): Partida {
        check(!terminada) { "La partida ya terminó" }
        return copy(ganador = ganador)
    }

    private fun turnoDesplazado(pasos: Int): Jugador {
        val orden = ordenDeInicio
        val actual = orden.indexOf(empieza)
        return if (actual == -1) empieza else orden[(actual + pasos).mod(orden.size)]
    }

    companion object {

        /** Crea una partida a partir de los jugadores en el orden en que fueron elegidos. */
        fun nueva(jugadores: List<Jugador>): Partida {
            require(jugadores.size == 2 || jugadores.size == 4) { "Se juega de a 2 o de a 4" }
            require(jugadores.distinctBy { it.id }.size == jugadores.size) {
                "Un jugador no puede estar dos veces en la misma partida"
            }
            val (uno, dos) = jugadores.chunked(jugadores.size / 2).map(::Equipo)
            return Partida(equipoUno = uno, equipoDos = dos, empieza = jugadores.first())
        }
    }
}
