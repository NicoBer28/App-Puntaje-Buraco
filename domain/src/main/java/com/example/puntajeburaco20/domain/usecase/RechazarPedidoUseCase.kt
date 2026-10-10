package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import javax.inject.Inject

/** El usuario actual dice que quien pidió un perfil que él creó no es su dueño. */
class RechazarPedidoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
) {
    suspend operator fun invoke(pedido: PedidoDeReclamo) = usuarios.rechazarPedido(pedido)
}
