package com.example.puntajeburaco20.ui.common

import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.ModoJuego

/**
 * Estado de los selectores de jugadores (hasta 4 posiciones). Solo cuentan las posiciones
 * visibles según el [modo], y un jugador elegido en una posición no se ofrece en las demás.
 */
data class SeleccionJugadores(
    val disponibles: List<Jugador> = emptyList(),
    val modo: ModoJuego = ModoJuego.INDIVIDUAL,
    private val elegidos: List<Jugador?> = List(MAX_POSICIONES) { null },
) {

    /** Jugadores de las posiciones visibles, en orden; `null` donde todavía no se eligió. */
    val visibles: List<Jugador?> get() = elegidos.take(modo.cantidadJugadores)

    val completa: Boolean get() = visibles.none { it == null }

    fun elegido(posicion: Int): Jugador? = elegidos[posicion]

    fun opcionesPara(posicion: Int): List<Jugador> {
        val ocupados = visibles.filterIndexed { i, jugador -> i != posicion && jugador != null }
        return disponibles.filter { candidato -> ocupados.none { it!!.esMismaPersona(candidato) } }
    }

    fun elegir(posicion: Int, jugador: Jugador?): SeleccionJugadores =
        copy(elegidos = elegidos.toMutableList().apply { set(posicion, jugador) })

    /** Al cambiar de modo se vacían las posiciones que dejan de verse. */
    fun conModo(nuevoModo: ModoJuego): SeleccionJugadores = copy(
        modo = nuevoModo,
        elegidos = elegidos.mapIndexed { i, jugador ->
            jugador.takeIf { i < nuevoModo.cantidadJugadores }
        },
    )

    /** Actualiza la lista de jugadores, descartando elecciones que ya no estén disponibles. */
    fun conDisponibles(jugadores: List<Jugador>): SeleccionJugadores = copy(
        disponibles = jugadores,
        elegidos = elegidos.map { elegido ->
            elegido?.takeIf { jugadores.any { it.esMismaPersona(elegido) } }
        },
    )

    companion object {
        const val MAX_POSICIONES = 4
    }
}
