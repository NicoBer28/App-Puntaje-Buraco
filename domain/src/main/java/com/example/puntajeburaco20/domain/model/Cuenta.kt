package com.example.puntajeburaco20.domain.model

import java.util.Locale

/**
 * Cuenta de acceso: el mail y la contraseña con los que una persona inicia sesión. Es distinta
 * del perfil ([Usuario]): una cuenta recién creada todavía no tiene uno, y hay perfiles sin cuenta.
 */
data class Cuenta(
    val uid: String,
    val mail: String,
    /** `true` si la persona ya abrió el enlace que se le envió por mail. */
    val verificada: Boolean,
) {
    companion object {
        /**
         * Forma en que se guarda y se compara un mail: sin espacios alrededor y en minúsculas, que
         * es como lo deja el servicio de cuentas.
         */
        fun claveDeMail(mail: String): String = mail.trim().lowercase(Locale.ROOT)
    }
}

/** En qué punto del ingreso está este dispositivo. */
sealed interface EstadoSesion {

    data object SinSesion : EstadoSesion

    /** Inició sesión pero todavía no verificó su mail: no puede hacer nada más. */
    data class SinVerificar(val cuenta: Cuenta) : EstadoSesion

    /** Cuenta verificada que todavía no eligió su nombre de usuario. */
    data class SinPerfil(val cuenta: Cuenta) : EstadoSesion

    data class Completa(val cuenta: Cuenta, val idPerfil: String) : EstadoSesion
}
