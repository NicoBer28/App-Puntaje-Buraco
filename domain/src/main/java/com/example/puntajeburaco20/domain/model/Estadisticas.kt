package com.example.puntajeburaco20.domain.model

/** Partidas jugadas y ganadas por un equipo (en general o contra un rival puntual). */
data class Estadisticas(
    val jugadas: Long,
    val ganadas: Long,
) {
    val perdidas: Long get() = jugadas - ganadas

    /** Las mismas partidas vistas desde el equipo rival. */
    fun desdeElRival(): Estadisticas = Estadisticas(jugadas = jugadas, ganadas = perdidas)

    operator fun plus(otras: Estadisticas): Estadisticas =
        Estadisticas(jugadas = jugadas + otras.jugadas, ganadas = ganadas + otras.ganadas)

    companion object {
        val VACIAS = Estadisticas(jugadas = 0, ganadas = 0)
    }
}
