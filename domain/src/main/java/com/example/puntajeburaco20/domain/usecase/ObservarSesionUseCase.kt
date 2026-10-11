package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Sigue el estado de la sesión: combina la cuenta que inició sesión con el perfil que tiene
 * vinculado. Emite de nuevo cada vez que cambia cualquiera de los dos.
 */
class ObservarSesionUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<EstadoSesion> = auth.cuenta
        .flatMapLatest { cuenta ->
            when {
                cuenta == null -> flowOf(EstadoSesion.SinSesion)
                !cuenta.verificada -> flowOf(EstadoSesion.SinVerificar(cuenta))
                else -> usuarios.observarIdDeCuenta(cuenta.uid).map { idPerfil ->
                    if (idPerfil == null) EstadoSesion.SinPerfil(cuenta) else EstadoSesion.Completa(cuenta, idPerfil)
                }
            }
        }
        .distinctUntilChanged()
}
