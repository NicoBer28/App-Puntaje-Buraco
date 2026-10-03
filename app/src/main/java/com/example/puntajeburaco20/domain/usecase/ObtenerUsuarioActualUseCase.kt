package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.SesionRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import javax.inject.Inject

class ObtenerUsuarioActualUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val sesion: SesionRepository,
) {
    /** @throws ErrorUsuario.SinSesion si nadie inició sesión. */
    suspend operator fun invoke(): Usuario {
        val id = sesion.usuarioActualId.value ?: throw ErrorUsuario.SinSesion
        return usuarios.obtener(id) ?: throw ErrorUsuario.UsuarioInexistente
    }
}
