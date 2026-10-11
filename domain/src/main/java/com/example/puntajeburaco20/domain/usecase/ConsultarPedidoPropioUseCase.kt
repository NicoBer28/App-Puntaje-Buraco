package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** El pedido de un perfil creado por otro que la cuenta que inició sesión tiene sin resolver, si hay. */
class ConsultarPedidoPropioUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
) {
    suspend operator fun invoke(): PedidoDeReclamo? {
        val cuenta = auth.cuenta.first() ?: throw ErrorUsuario.SinSesion
        return usuarios.buscarPedidoPropio(cuenta)
    }
}
