package com.example.puntajeburaco20.domain.model

/**
 * Cuenta de acceso: el mail y la contraseña con los que una persona inicia sesión. Es distinta
 * del perfil ([Usuario]): una cuenta recién creada todavía no tiene uno, y hay perfiles sin cuenta.
 */
data class Cuenta(
    val uid: String,
    val mail: String,
    /** `true` si la persona ya abrió el enlace que se le envió por mail. */
    val verificada: Boolean,
)

/** En qué punto del ingreso está este dispositivo. */
sealed interface EstadoSesion {

    data object SinSesion : EstadoSesion

    /** Inició sesión pero todavía no verificó su mail: no puede hacer nada más. */
    data class SinVerificar(val cuenta: Cuenta) : EstadoSesion

    /** Cuenta verificada que todavía no eligió su nombre de usuario. */
    data class SinPerfil(val cuenta: Cuenta) : EstadoSesion

    data class Completa(val cuenta: Cuenta, val idPerfil: String) : EstadoSesion
}
