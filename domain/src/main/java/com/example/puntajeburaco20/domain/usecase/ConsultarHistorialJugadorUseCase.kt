package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.HistorialJugador
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import javax.inject.Inject

/** Últimas partidas del usuario logueado, con su racha y puntaje promedio. */
class ConsultarHistorialJugadorUseCase @Inject constructor(
    private val partidasJugadas: PartidasJugadasRepository,
    private val obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
) {
    suspend operator fun invoke(): HistorialJugador {
        val jugador = obtenerUsuarioActual().jugador
        return HistorialJugador(jugador, partidasJugadas.obtenerDe(jugador, LIMITE))
    }

    companion object {
        /** Cuántas partidas se traen: alcanza para la racha y el promedio sin leer todo el historial. */
        const val LIMITE = 50
    }
}
