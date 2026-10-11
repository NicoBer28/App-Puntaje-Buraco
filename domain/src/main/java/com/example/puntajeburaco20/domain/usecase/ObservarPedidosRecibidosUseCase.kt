package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Sigue los pedidos sin resolver sobre perfiles que el usuario actual creó para otros. */
class ObservarPedidosRecibidosUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val observarSesion: ObservarSesionUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<PedidoDeReclamo>> = observarSesion().flatMapLatest { sesion ->
        if (sesion is EstadoSesion.Completa) usuarios.observarPedidosRecibidos(sesion.cuenta) else flowOf(emptyList())
    }
}
