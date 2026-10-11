package com.example.puntajeburaco20.domain.model

/**
 * A cuántos puntos se juega. Si está [activo], la partida se da por ganada cuando un equipo llega
 * a esos [puntos]; si no, termina solo cuando los jugadores lo deciden.
 */
data class LimitePuntos(
    val puntos: Int = PREDETERMINADO,
    val activo: Boolean = false,
) {
    companion object {
        const val PREDETERMINADO = 3000

        /** Valores que se aceptan como límite. */
        val RANGO = 100..999_999
    }
}
