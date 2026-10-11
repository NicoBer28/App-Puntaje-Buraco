package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Crea el perfil de la cuenta que inició sesión, con el nombre de usuario que eligió. */
class CrearPerfilUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(nombre: String): Usuario {
        validador.validarNombre(nombre)
        val cuenta = auth.cuenta.first() ?: throw ErrorUsuario.SinSesion
        if (!cuenta.verificada) throw ErrorUsuario.MailSinVerificar
        // crear() falla con NombreEnUso si el nombre ya está tomado.
        return usuarios.crear(cuenta, nombre)
    }
}
