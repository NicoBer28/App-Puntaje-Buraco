package com.example.puntajeburaco20.data.local

import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.domain.model.Ronda
import kotlinx.serialization.Serializable

/**
 * Representación serializable de una [Partida], desacoplada del modelo de dominio.
 *
 * Una partida guardada por una versión anterior de la app, que identificaba a los jugadores solo
 * por su nombre, no tiene estos campos: no se puede leer y se descarta.
 */
@Serializable
internal data class PartidaGuardada(
    val jugadoresUno: List<JugadorGuardado>,
    val jugadoresDos: List<JugadorGuardado>,
    val idEmpieza: String,
    val rondas: List<RondaGuardada>,
    val ganador: LadoEquipo? = null,
) {
    fun aDominio(): Partida {
        val uno = jugadoresUno.map { it.aDominio() }
        val dos = jugadoresDos.map { it.aDominio() }
        return Partida(
            equipoUno = Equipo(uno),
            equipoDos = Equipo(dos),
            empieza = (uno + dos).first { it.id == idEmpieza },
            rondas = rondas.map { it.aDominio() },
            ganador = ganador,
        )
    }

    companion object {
        fun desde(partida: Partida) = PartidaGuardada(
            jugadoresUno = partida.equipoUno.jugadores.map(JugadorGuardado::desde),
            jugadoresDos = partida.equipoDos.jugadores.map(JugadorGuardado::desde),
            idEmpieza = partida.empieza.id,
            rondas = partida.rondas.map(RondaGuardada::desde),
            ganador = partida.ganador,
        )
    }
}

@Serializable
internal data class JugadorGuardado(val id: String, val nombre: String) {
    fun aDominio() = Jugador(id, nombre)

    companion object {
        fun desde(jugador: Jugador) = JugadorGuardado(jugador.id, jugador.nombre)
    }
}

@Serializable
internal data class RondaGuardada(val equipoUno: PuntajeGuardado, val equipoDos: PuntajeGuardado) {
    fun aDominio() = Ronda(equipoUno.aDominio(), equipoDos.aDominio())

    companion object {
        fun desde(ronda: Ronda) = RondaGuardada(PuntajeGuardado.desde(ronda.equipoUno), PuntajeGuardado.desde(ronda.equipoDos))
    }
}

@Serializable
internal data class PuntajeGuardado(val base: Int, val puntos: Int) {
    fun aDominio() = PuntajeRonda(base, puntos)

    companion object {
        fun desde(puntaje: PuntajeRonda) = PuntajeGuardado(puntaje.base, puntaje.puntos)
    }
}
