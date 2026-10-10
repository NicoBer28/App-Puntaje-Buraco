package com.example.puntajeburaco20.data.local

import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.domain.model.Ronda
import com.example.puntajeburaco20.domain.model.Usuario
import kotlinx.serialization.Serializable

/**
 * Representación serializable de una [Partida], desacoplada del modelo de dominio.
 *
 * Todavía se leen dos formatos de versiones anteriores, que ya no se escriben, para poder retomar
 * una partida empezada antes de actualizar la app:
 * - el que identificaba a los jugadores solo por su nombre ([equipoUno], [equipoDos], [empieza]);
 * - el que no guardaba las rondas sino solo la última y los totales ([ultimaRondaUno],
 *   [ultimaRondaDos], [totalUno], [totalDos]).
 */
@Serializable
internal data class PartidaGuardada(
    val jugadoresUno: List<JugadorGuardado>? = null,
    val jugadoresDos: List<JugadorGuardado>? = null,
    val idEmpieza: String? = null,
    val rondas: List<RondaGuardada>? = null,
    val ganador: LadoEquipo? = null,
    val equipoUno: List<String> = emptyList(),
    val equipoDos: List<String> = emptyList(),
    val empieza: String? = null,
    val ultimaRondaUno: PuntajeGuardado? = null,
    val ultimaRondaDos: PuntajeGuardado? = null,
    val totalUno: Int = 0,
    val totalDos: Int = 0,
) {
    fun aDominio(): Partida {
        val uno = jugadoresUno?.map { it.aDominio() } ?: equipoUno.map(JugadorGuardado::delFormatoAnterior)
        val dos = jugadoresDos?.map { it.aDominio() } ?: equipoDos.map(JugadorGuardado::delFormatoAnterior)
        val todos = uno + dos
        return Partida(
            equipoUno = Equipo(uno),
            equipoDos = Equipo(dos),
            empieza = todos.firstOrNull { it.id == idEmpieza }
                ?: todos.firstOrNull { it.nombre == empieza }
                ?: todos.first(),
            rondas = rondas?.map { it.aDominio() } ?: rondasDelFormatoAnterior(),
            ganador = ganador,
        )
    }

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

        /** En el formato anterior el id de un jugador era su nombre en minúsculas. */
        fun delFormatoAnterior(nombre: String) = Jugador(id = Usuario.claveDeNombre(nombre), nombre = nombre)
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
