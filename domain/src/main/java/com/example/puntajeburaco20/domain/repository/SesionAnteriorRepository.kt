package com.example.puntajeburaco20.domain.repository

/**
 * Sesión que este dispositivo tenía iniciada con una versión de la app anterior a las cuentas con
 * mail, cuando se entraba con el nombre de usuario. Sirve para ofrecerle a esa persona que
 * vincule su perfil en lugar de crear uno nuevo.
 */
interface SesionAnteriorRepository {

    /** Nombre de usuario de esa sesión, o `null` si el dispositivo no tenía ninguna. */
    suspend fun nombreDeUsuario(): String?

    suspend fun olvidar()
}
