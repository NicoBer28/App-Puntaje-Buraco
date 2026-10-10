package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ObtenerUsuarioActualUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val observarSesion: ObservarSesionUseCase,
) {
    /** @throws ErrorUsuario.SinSesion si nadie inició sesión o la cuenta todavía no tiene perfil. */
    suspend operator fun invoke(): Usuario {
        val sesion = observarSesion().first() as? EstadoSesion.Completa ?: throw ErrorUsuario.SinSesion
        return usuarios.obtener(sesion.idPerfil) ?: throw ErrorUsuario.UsuarioInexistente
    }
}
