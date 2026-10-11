package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas

/**
 * Resultados de jugadores individuales y de parejas. No se registran aparte: salen de las
 * partidas guardadas en [PartidasJugadasRepository].
 *
 * Las consultas necesitan conexión y fallan con [ErrorUsuario.SinConexion] si no la hay.
 */
interface EstadisticasRepository {

    /** Totales del equipo contra cualquier rival, o `null` si nunca jugó. */
    suspend fun obtenerGenerales(equipo: Equipo): Estadisticas?

    /** Resultados del [equipo] contra el [rival], o `null` si nunca se enfrentaron. */
    suspend fun obtenerEnfrentamiento(equipo: Equipo, rival: Equipo): Estadisticas?
}
