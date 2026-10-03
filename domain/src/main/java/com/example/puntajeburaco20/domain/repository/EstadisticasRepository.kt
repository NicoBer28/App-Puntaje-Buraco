package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas

/** Historial de resultados, tanto de jugadores individuales como de parejas. */
interface EstadisticasRepository {

    /** Suma una partida jugada a ambos equipos y una ganada al [ganador]. */
    suspend fun registrarResultado(ganador: Equipo, perdedor: Equipo)

    /** Totales del equipo contra cualquier rival, o `null` si nunca jugó. */
    suspend fun obtenerGenerales(equipo: Equipo): Estadisticas?

    /** Resultados del [equipo] contra el [rival], o `null` si nunca se enfrentaron. */
    suspend fun obtenerEnfrentamiento(equipo: Equipo, rival: Equipo): Estadisticas?
}
