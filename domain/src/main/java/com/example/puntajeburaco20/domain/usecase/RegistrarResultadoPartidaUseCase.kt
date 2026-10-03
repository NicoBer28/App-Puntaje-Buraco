package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import javax.inject.Inject

/**
 * Vuelca el resultado de una partida terminada en el historial: suma a los contadores de ambos
 * equipos y guarda la partida completa, ronda por ronda.
 */
class RegistrarResultadoPartidaUseCase @Inject constructor(
    private val estadisticas: EstadisticasRepository,
    private val partidasJugadas: PartidasJugadasRepository,
) {
    suspend operator fun invoke(partida: Partida) {
        val ganador = requireNotNull(partida.ganador) { "La partida todavía no terminó" }
        estadisticas.registrarResultado(
            ganador = partida.equipo(ganador),
            perdedor = partida.equipo(ganador.rival),
        )
        partidasJugadas.guardar(partida)
    }
}
