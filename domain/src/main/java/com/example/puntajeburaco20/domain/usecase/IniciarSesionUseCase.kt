package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.SesionRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import javax.inject.Inject

class IniciarSesionUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val sesion: SesionRepository,
) {
    suspend operator fun invoke(nombre: String, password: String): Usuario {
        if (nombre.isEmpty() || password.isEmpty()) throw ErrorUsuario.CamposIncompletos
        val usuario = usuarios.autenticar(nombre, password)
        sesion.iniciar(usuario.id)
        return usuario
    }
}
