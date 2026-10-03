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

/**
 * Estado de una partida de Buraco. Es inmutable: cada operación devuelve una partida nueva.
 *
 * En partidas de 4 el equipo uno son los dos primeros jugadores elegidos y el equipo dos los
 * dos últimos. La ronda la empieza cada jugador por turno, alternando equipos.
 */
data class Partida(
    val equipoUno: Equipo,
    val equipoDos: Equipo,
    val empieza: Jugador,
    val ultimaRondaUno: PuntajeRonda = PuntajeRonda.CERO,
    val ultimaRondaDos: PuntajeRonda = PuntajeRonda.CERO,
    val totalUno: Int = 0,
    val totalDos: Int = 0,
    val ganador: LadoEquipo? = null,
) {

    init {
        require(equipoUno.jugadores.size == equipoDos.jugadores.size) {
            "Los equipos deben tener la misma cantidad de jugadores"
        }
    }

    val modo: ModoJuego get() = if (equipoUno.esPareja) ModoJuego.PAREJAS else ModoJuego.INDIVIDUAL

    val terminada: Boolean get() = ganador != null

    /** Orden en que los jugadores se turnan para empezar cada ronda. */
    val ordenDeInicio: List<Jugador>
        get() = when (modo) {
            ModoJuego.INDIVIDUAL -> listOf(equipoUno.jugadores[0], equipoDos.jugadores[0])
            ModoJuego.PAREJAS -> listOf(
                equipoUno.jugadores[0],
                equipoDos.jugadores[1],
                equipoUno.jugadores[1],
                equipoDos.jugadores[0],
            )
        }

    fun equipo(lado: LadoEquipo): Equipo = when (lado) {
        LadoEquipo.UNO -> equipoUno
        LadoEquipo.DOS -> equipoDos
    }

    fun registrarRonda(rondaUno: PuntajeRonda, rondaDos: PuntajeRonda): Partida {
        check(!terminada) { "La partida ya terminó" }
        return copy(
            ultimaRondaUno = rondaUno,
            ultimaRondaDos = rondaDos,
            totalUno = totalUno + rondaUno.total,
            totalDos = totalDos + rondaDos.total,
            empieza = siguienteEnEmpezar(),
        )
    }

    fun finalizar(ganador: LadoEquipo): Partida {
        check(!terminada) { "La partida ya terminó" }
        return copy(ganador = ganador)
    }

    private fun siguienteEnEmpezar(): Jugador {
        val orden = ordenDeInicio
        val actual = orden.indexOf(empieza)
        return if (actual == -1) empieza else orden[(actual + 1) % orden.size]
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
