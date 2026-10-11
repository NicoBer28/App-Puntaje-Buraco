package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Envía a ese mail un enlace para elegir una contraseña nueva. */
class RecuperarContrasenaUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(mail: String) {
        val mailLimpio = mail.trim()
        validador.validarMail(mailLimpio)
        auth.enviarRecuperacion(mailLimpio)
    }

    /**
     * Se lo envía a quien tiene la sesión iniciada, que no se acuerda de su contraseña actual.
     *
     * @return el mail al que se envió, para decirle dónde buscarlo.
     */
    suspend fun aLaCuentaActual(): String {
        val mail = auth.cuenta.first()?.mail ?: throw ErrorUsuario.SinSesion
        auth.enviarRecuperacion(mail)
        return mail
    }
}
