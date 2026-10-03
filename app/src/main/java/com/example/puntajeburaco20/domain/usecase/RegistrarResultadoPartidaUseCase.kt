package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import javax.inject.Inject

/** Vuelca el resultado de una partida terminada en el historial de ambos equipos. */
class RegistrarResultadoPartidaUseCase @Inject constructor(
    private val estadisticas: EstadisticasRepository,
) {
    suspend operator fun invoke(partida: Partida) {
        val ganador = requireNotNull(partida.ganador) { "La partida todavía no terminó" }
        estadisticas.registrarResultado(
            ganador = partida.equipo(ganador),
            perdedor = partida.equipo(ganador.rival),
        )
    }
}
