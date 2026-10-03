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
 * Las versiones anteriores no guardaban las rondas sino solo la última y los totales
 * ([ultimaRondaUno], [ultimaRondaDos], [totalUno], [totalDos]). Esos campos se siguen leyendo para
 * poder retomar una partida empezada antes de actualizar la app, pero ya no se escriben.
 */
@Serializable
internal data class PartidaGuardada(
    val equipoUno: List<String>,
    val equipoDos: List<String>,
    val empieza: String,
    val rondas: List<RondaGuardada>? = null,
    val ganador: LadoEquipo? = null,
    val ultimaRondaUno: PuntajeGuardado? = null,
    val ultimaRondaDos: PuntajeGuardado? = null,
    val totalUno: Int = 0,
    val totalDos: Int = 0,
) {
    fun aDominio() = Partida(
        equipoUno = Equipo(equipoUno.map(::Jugador)),
        equipoDos = Equipo(equipoDos.map(::Jugador)),
        empieza = Jugador(empieza),
        rondas = rondas?.map { it.aDominio() } ?: rondasDelFormatoAnterior(),
        ganador = ganador,
    )

    /**
     * El formato anterior no tiene el detalle de cada ronda: todas las anteriores a la última se
     * agrupan en una sola, de modo que los totales y la última ronda se mantienen.
     */
    private fun rondasDelFormatoAnterior(): List<Ronda> {
        val ultima = Ronda(
            equipoUno = ultimaRondaUno?.aDominio() ?: PuntajeRonda.CERO,
            equipoDos = ultimaRondaDos?.aDominio() ?: PuntajeRonda.CERO,
        )
        val anteriores = Ronda(
            equipoUno = PuntajeRonda(base = totalUno - ultima.equipoUno.total, puntos = 0),
            equipoDos = PuntajeRonda(base = totalDos - ultima.equipoDos.total, puntos = 0),
        )
        return listOf(anteriores, ultima).filter { it != Ronda.CERO }
    }

    companion object {
        fun desde(partida: Partida) = PartidaGuardada(
            equipoUno = partida.equipoUno.jugadores.map { it.nombre },
            equipoDos = partida.equipoDos.jugadores.map { it.nombre },
            empieza = partida.empieza.nombre,
            rondas = partida.rondas.map(RondaGuardada::desde),
            ganador = partida.ganador,
        )
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
