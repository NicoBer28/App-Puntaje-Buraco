package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import javax.inject.Inject

class ConsultarEstadisticasUseCase @Inject constructor(
    private val estadisticas: EstadisticasRepository,
) {
    /**
     * Sin [rival] devuelve los totales generales del [equipo]; con rival, solo sus enfrentamientos.
     * Devuelve `null` si no hay partidas registradas.
     */
    suspend operator fun invoke(equipo: Equipo, rival: Equipo?): Estadisticas? =
        if (rival == null) {
            estadisticas.obtenerGenerales(equipo)
        } else {
            estadisticas.obtenerEnfrentamiento(equipo, rival)
        }
}
