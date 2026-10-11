package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Borra un perfil que el usuario actual creó para otra persona y que nadie aceptó todavía. Su
 * nombre y su mail quedan libres; las partidas que jugó siguen en el historial de los demás.
 */
class BorrarPerfilACargoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val observarSesion: ObservarSesionUseCase,
) {
    suspend operator fun invoke(perfil: PerfilACargo) {
        val sesion = observarSesion().first() as? EstadoSesion.Completa ?: throw ErrorUsuario.SinSesion
        usuarios.borrarPerfilACargo(perfil.jugador, creador = sesion.cuenta)
    }
}
