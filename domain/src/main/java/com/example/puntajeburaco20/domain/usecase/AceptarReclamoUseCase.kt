package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** La persona confirma que el perfil reservado para su mail es suyo: queda vinculado a su cuenta. */
class AceptarReclamoUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
) {
    suspend operator fun invoke(): Jugador {
        val cuenta = auth.cuenta.first() ?: throw ErrorUsuario.SinSesion
        if (!cuenta.verificada) throw ErrorUsuario.MailSinVerificar
        return usuarios.aceptarReclamo(cuenta)
    }
}
