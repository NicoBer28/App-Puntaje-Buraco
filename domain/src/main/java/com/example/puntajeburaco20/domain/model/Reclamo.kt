package com.example.puntajeburaco20.domain.model

/**
 * Perfil sin cuenta que el usuario creó para otra persona y que sigue a su cargo, con el [mail]
 * para el que lo dejó reservado (`null` si todavía no le cargó ninguno).
 */
data class PerfilACargo(val jugador: Jugador, val mail: String?)

/**
 * Un perfil reservado para el mail de la cuenta que inició sesión: se lo creó [nombreCreador]
 * cuando esa persona todavía no usaba la app, y ahora puede aceptarlo o decir que no es suyo.
 *
 * @param partidas cuántas partidas tiene jugadas el perfil, o `null` si no se pudo averiguar.
 */
data class Reclamo(val perfil: Jugador, val nombreCreador: String, val partidas: Long? = null)
