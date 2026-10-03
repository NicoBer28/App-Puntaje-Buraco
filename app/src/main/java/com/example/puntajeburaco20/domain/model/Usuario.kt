package com.example.puntajeburaco20.domain.model

/** Cuenta registrada en la aplicación, junto con sus amigos. */
data class Usuario(
    val jugador: Jugador,
    val amigos: List<Jugador>,
) {
    val id: String get() = jugador.id
    val nombre: String get() = jugador.nombre

    fun esAmigoDe(otro: Jugador): Boolean = amigos.any { it.esMismaPersona(otro) }

    /** Quienes pueden sentarse a jugar con este usuario: él mismo y sus amigos. */
    fun jugadoresDisponibles(): List<Jugador> = listOf(jugador) + amigos
}
