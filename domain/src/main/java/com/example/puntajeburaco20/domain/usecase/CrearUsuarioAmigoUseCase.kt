package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Crea el perfil de otra persona que no usa la app y la agrega directamente como amiga del
 * usuario actual. El perfil no tiene cuenta de acceso: queda a cargo de quien lo creó, y
 * reservado para el mail de esa persona, que lo recibe cuando se registra.
 */
class CrearUsuarioAmigoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val observarSesion: ObservarSesionUseCase,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(nombre: String, mail: String) {
        val claveDeMail = Cuenta.claveDeMail(mail)
        if (nombre.isEmpty() || claveDeMail.isEmpty()) throw ErrorUsuario.CamposIncompletos
        validador.validarNombre(nombre)
        validador.validarMail(claveDeMail)
        val sesion = observarSesion().first() as? EstadoSesion.Completa ?: throw ErrorUsuario.SinSesion
        val actual = usuarios.obtener(sesion.idPerfil) ?: throw ErrorUsuario.UsuarioInexistente
        if (Usuario.claveDeNombre(nombre) == Usuario.claveDeNombre(actual.nombre)) {
            throw ErrorUsuario.EsElUsuarioActual
        }

        // Falla con NombreEnUso si el nombre ya está tomado, y con MailConPerfil si lo está el mail.
        usuarios.crearAmigoSinLogin(nombre, claveDeMail, creador = sesion.cuenta, amigoDe = actual.jugador)
    }
}
