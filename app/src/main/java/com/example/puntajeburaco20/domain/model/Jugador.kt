package com.example.puntajeburaco20.domain.model

import java.util.Locale

/**
 * Persona que participa de una partida.
 *
 * El nombre se muestra tal cual fue registrado, pero la identidad se determina por [id]
 * (el nombre sin distinguir mayúsculas), que es también el identificador de su cuenta.
 */
data class Jugador(val nombre: String) {

    val id: String get() = idDesdeNombre(nombre)

    fun esMismaPersona(otro: Jugador): Boolean = id == otro.id

    companion object {
        fun idDesdeNombre(nombre: String): String = nombre.lowercase(Locale.ROOT)
    }
}
