package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * La cuenta que inició sesión desiste del perfil que había pedido. Después puede elegir un
 * nombre nuevo, sin el historial de ese perfil.
 */
class CancelarPedidoUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
) {
    suspend operator fun invoke() {
        val cuenta = auth.cuenta.first() ?: throw ErrorUsuario.SinSesion
        usuarios.cancelarPedido(cuenta)
    }
}
