package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import kotlinx.coroutines.flow.Flow

/**
 * Cuentas de acceso (mail y contraseña), independiente de quién las administre.
 *
 * Todas las operaciones necesitan conexión y fallan con [ErrorUsuario.SinConexion] si no la hay.
 */
interface AuthRepository {

    /**
     * Cuenta con la sesión iniciada en este dispositivo, o `null`. Emite de nuevo al entrar, al
     * salir y cuando [recargar] detecta que el mail fue verificado.
     */
    val cuenta: Flow<Cuenta?>

    /**
     * Crea la cuenta y deja la sesión iniciada con ella.
     *
     * @throws ErrorUsuario.MailEnUso si ya hay una cuenta con ese mail.
     * @throws ErrorUsuario.MailInvalido o [ErrorUsuario.ContrasenaCorta] si no cumplen el formato.
     */
    suspend fun registrar(mail: String, password: String): Cuenta

    /** @throws ErrorUsuario.CredencialesIncorrectas si el mail o la contraseña no coinciden. */
    suspend fun iniciarSesion(mail: String, password: String): Cuenta

    suspend fun cerrarSesion()

    /** Envía a la cuenta actual el mail con el enlace para verificar su dirección. */
    suspend fun enviarVerificacion()

    /**
     * Vuelve a consultar la cuenta actual: es la forma de enterarse de que el mail fue verificado,
     * porque eso pasa fuera de la app.
     */
    suspend fun recargar(): Cuenta?

    /** Envía un mail para elegir una contraseña nueva. No informa si el mail tiene cuenta o no. */
    suspend fun enviarRecuperacion(mail: String)
}
