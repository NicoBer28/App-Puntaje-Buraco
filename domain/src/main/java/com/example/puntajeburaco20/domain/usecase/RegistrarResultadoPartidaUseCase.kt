package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import javax.inject.Inject

/**
 * Guarda una partida terminada, ronda por ronda. Con eso alcanza para el historial y para las
 * estadísticas de ambos equipos, que se calculan a partir de las partidas guardadas.
 */
class RegistrarResultadoPartidaUseCase @Inject constructor(
    private val partidasJugadas: PartidasJugadasRepository,
) {
    suspend operator fun invoke(partida: Partida) {
        require(partida.terminada) { "La partida todavía no terminó" }
        partidasJugadas.guardar(partida)
    }
}
