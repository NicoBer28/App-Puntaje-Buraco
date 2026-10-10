package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.SesionAnteriorRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Vincula a la cuenta que inició sesión el perfil que esa persona ya usaba antes de que la app
 * tuviera cuentas con mail, en lugar de crearle uno nuevo con [CrearPerfilUseCase]. Así conserva
 * sus amigos y su historial.
 */
class VincularPerfilAnteriorUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
    private val sesionAnterior: SesionAnteriorRepository,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(nombre: String, passwordAnterior: String): Jugador {
        if (nombre.isEmpty() || passwordAnterior.isEmpty()) throw ErrorUsuario.CamposIncompletos
        validador.validarNombre(nombre)
        val cuenta = auth.cuenta.first() ?: throw ErrorUsuario.SinSesion
        if (!cuenta.verificada) throw ErrorUsuario.MailSinVerificar

        val jugador = usuarios.vincularAnterior(cuenta, nombre, passwordAnterior)
        // El dispositivo ya no necesita recordar con qué usuario se entraba antes.
        sesionAnterior.olvidar()
        return jugador
    }
}
