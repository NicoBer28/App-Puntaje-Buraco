package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.SesionRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import javax.inject.Inject

/** Crea una cuenta nueva e inicia sesión con ella. */
class RegistrarUsuarioUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val sesion: SesionRepository,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(nombre: String, password: String): Usuario {
        validador.validar(nombre, password)
        // crear() falla con NombreEnUso si el nombre ya está tomado.
        val usuario = usuarios.crear(nombre, password)
        sesion.iniciar(usuario.id)
        return usuario
    }
}
