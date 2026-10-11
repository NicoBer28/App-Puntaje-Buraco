package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * La persona dice que el perfil reservado para su mail no es suyo. El perfil sigue a cargo de
 * quien lo creó, y ella pasa a elegir un nombre de usuario como cualquier cuenta nueva.
 */
class RechazarReclamoUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
) {
    suspend operator fun invoke() {
        val cuenta = auth.cuenta.first() ?: throw ErrorUsuario.SinSesion
        if (!cuenta.verificada) throw ErrorUsuario.MailSinVerificar
        usuarios.rechazarReclamo(cuenta)
    }
}
