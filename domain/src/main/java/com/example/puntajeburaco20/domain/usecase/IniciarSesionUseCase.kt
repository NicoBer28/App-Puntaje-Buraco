package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.repository.AuthRepository
import javax.inject.Inject

class IniciarSesionUseCase @Inject constructor(
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(mail: String, password: String): Cuenta {
        val mailLimpio = mail.trim()
        if (mailLimpio.isEmpty() || password.isEmpty()) throw ErrorUsuario.CamposIncompletos
        return auth.iniciarSesion(mailLimpio, password)
    }
}
