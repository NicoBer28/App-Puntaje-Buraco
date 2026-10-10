package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
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
}
