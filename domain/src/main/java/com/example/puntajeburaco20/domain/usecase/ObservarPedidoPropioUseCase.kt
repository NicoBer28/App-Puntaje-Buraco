package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Sigue el pedido de la cuenta que inició sesión: emite `null` cuando deja de existir, porque
 * quien creó el perfil lo aceptó o lo rechazó.
 */
class ObservarPedidoPropioUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<PedidoDeReclamo?> = auth.cuenta.flatMapLatest { cuenta ->
        if (cuenta == null) flowOf(null) else usuarios.observarPedidoPropio(cuenta)
    }
}
