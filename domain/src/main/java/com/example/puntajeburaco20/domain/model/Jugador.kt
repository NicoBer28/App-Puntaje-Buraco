package com.example.puntajeburaco20.domain.model

/**
 * Persona que participa de una partida.
 *
 * La identidad la da el [id] de su perfil. El [nombre] es solo lo que se muestra: dos jugadores
 * con el mismo id son la misma persona aunque el nombre haya cambiado.
 */
data class Jugador(val id: String, val nombre: String) {

    fun esMismaPersona(otro: Jugador): Boolean = id == otro.id
}
