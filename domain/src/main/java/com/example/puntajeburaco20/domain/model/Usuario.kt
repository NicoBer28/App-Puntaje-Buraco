package com.example.puntajeburaco20.domain.model

import java.util.Locale

/** Perfil de un jugador registrado en la aplicación, junto con sus amigos. */
data class Usuario(
    val jugador: Jugador,
    val amigos: List<Jugador>,
) {
    val id: String get() = jugador.id
    val nombre: String get() = jugador.nombre

    fun esAmigoDe(otro: Jugador): Boolean = amigos.any { it.esMismaPersona(otro) }

    /** Quienes pueden sentarse a jugar con este usuario: él mismo y sus amigos. */
    fun jugadoresDisponibles(): List<Jugador> = listOf(jugador) + amigos

    companion object {
        /**
         * Forma en que se reserva y se busca un nombre de usuario: sin distinguir mayúsculas, así
         * "Ana" y "ANA" son el mismo nombre.
         */
        fun claveDeNombre(nombre: String): String = nombre.lowercase(Locale.ROOT)
    }
}
