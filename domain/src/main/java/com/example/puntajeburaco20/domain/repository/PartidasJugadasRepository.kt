package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PartidaJugada

/** Historial detallado (ronda por ronda) de las partidas terminadas. */
interface PartidasJugadasRepository {

    /**
     * Guarda la partida terminada. No espera al servidor: sin conexión queda en el dispositivo y
     * se sube después.
     */
    suspend fun guardar(partida: Partida)

    /** Las últimas [limite] partidas del [jugador], de la más reciente a la más vieja. */
    suspend fun obtenerDe(jugador: Jugador, limite: Int): List<PartidaJugada>

    /** Cuántas partidas terminadas tiene el [jugador]. Se cuentan en el servidor: necesita conexión. */
    suspend fun contarDe(jugador: Jugador): Long
}
