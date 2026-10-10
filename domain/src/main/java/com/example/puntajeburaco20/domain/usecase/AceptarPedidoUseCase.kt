package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * El usuario actual confirma que quien pidió un perfil que él creó es su dueño. El perfil queda
 * reservado para el mail de esa persona, que entonces puede aceptarlo.
 */
class AceptarPedidoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val observarSesion: ObservarSesionUseCase,
) {
    suspend operator fun invoke(pedido: PedidoDeReclamo) {
        val sesion = observarSesion().first() as? EstadoSesion.Completa ?: throw ErrorUsuario.SinSesion
        val actual = usuarios.obtener(sesion.idPerfil) ?: throw ErrorUsuario.UsuarioInexistente
        usuarios.aceptarPedido(pedido, creador = sesion.cuenta, nombreCreador = actual.nombre)
    }
}
