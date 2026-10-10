package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Carga o corrige el mail para el que está reservado un perfil que el usuario actual creó para
 * otra persona. Sirve mientras esa persona no lo haya aceptado.
 */
class ReservarPerfilACargoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val observarSesion: ObservarSesionUseCase,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(perfil: PerfilACargo, mail: String) {
        val claveDeMail = Cuenta.claveDeMail(mail)
        validador.validarMail(claveDeMail)
        if (claveDeMail == perfil.mail) return
        val sesion = observarSesion().first() as? EstadoSesion.Completa ?: throw ErrorUsuario.SinSesion
        val actual = usuarios.obtener(sesion.idPerfil) ?: throw ErrorUsuario.UsuarioInexistente

        // Falla con MailConPerfil si ese mail ya tiene una cuenta o un perfil reservado.
        usuarios.reservarPara(perfil, claveDeMail, creador = sesion.cuenta, nombreCreador = actual.nombre)
    }
}
