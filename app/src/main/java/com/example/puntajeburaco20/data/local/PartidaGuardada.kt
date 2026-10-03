package com.example.puntajeburaco20.data.local

import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import kotlinx.serialization.Serializable

/** Representación serializable de una [Partida], desacoplada del modelo de dominio. */
@Serializable
internal data class PartidaGuardada(
    val equipoUno: List<String>,
    val equipoDos: List<String>,
    val empieza: String,
    val ultimaRondaUno: RondaGuardada,
    val ultimaRondaDos: RondaGuardada,
    val totalUno: Int,
    val totalDos: Int,
    val ganador: LadoEquipo? = null,
) {
    fun aDominio() = Partida(
        equipoUno = Equipo(equipoUno.map(::Jugador)),
        equipoDos = Equipo(equipoDos.map(::Jugador)),
        empieza = Jugador(empieza),
        ultimaRondaUno = ultimaRondaUno.aDominio(),
        ultimaRondaDos = ultimaRondaDos.aDominio(),
        totalUno = totalUno,
        totalDos = totalDos,
        ganador = ganador,
    )

    companion object {
        fun desde(partida: Partida) = PartidaGuardada(
            equipoUno = partida.equipoUno.jugadores.map { it.nombre },
            equipoDos = partida.equipoDos.jugadores.map { it.nombre },
            empieza = partida.empieza.nombre,
            ultimaRondaUno = RondaGuardada.desde(partida.ultimaRondaUno),
            ultimaRondaDos = RondaGuardada.desde(partida.ultimaRondaDos),
            totalUno = partida.totalUno,
            totalDos = partida.totalDos,
            ganador = partida.ganador,
        )
    }
}

@Serializable
internal data class RondaGuardada(val base: Int, val puntos: Int) {
    fun aDominio() = PuntajeRonda(base, puntos)

    companion object {
        fun desde(ronda: PuntajeRonda) = RondaGuardada(ronda.base, ronda.puntos)
    }
}
