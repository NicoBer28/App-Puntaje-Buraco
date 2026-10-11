package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import javax.inject.Inject

/**
 * Crea una cuenta, deja la sesión iniciada con ella y le envía el mail de verificación. El
 * perfil se crea después, con [CrearPerfilUseCase], una vez verificado el mail.
 */
class RegistrarCuentaUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(mail: String, password: String): Cuenta {
        val mailLimpio = mail.trim()
        validador.validarCuenta(mailLimpio, password)
        val cuenta = auth.registrar(mailLimpio, password)
        // Si el envío falla la cuenta ya quedó creada: el mail se puede volver a pedir.
        auth.enviarVerificacion()
        return cuenta
    }
}
