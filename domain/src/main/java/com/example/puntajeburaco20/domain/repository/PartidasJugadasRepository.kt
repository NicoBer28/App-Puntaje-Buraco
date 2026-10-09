package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PartidaJugada

/** Historial detallado (ronda por ronda) de las partidas terminadas. */
interface PartidasJugadasRepository {

    /** Guarda la partida terminada en el historial de cada uno de sus jugadores. */
    suspend fun guardar(partida: Partida)

    /** Las últimas [limite] partidas del [jugador], de la más reciente a la más vieja. */
    suspend fun obtenerDe(jugador: Jugador, limite: Int): List<PartidaJugada>
}
