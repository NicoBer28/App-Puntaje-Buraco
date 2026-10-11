package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Elimina la cuenta del usuario actual y su perfil, con sus amistades. Su nombre y su mail
 * quedan libres. Las partidas que jugó no se borran: siguen en el historial de los demás, con el
 * nombre que tenía.
 *
 * Pide la contraseña de nuevo: primero se la comprueba, para no borrar el perfil y que después
 * el servicio de cuentas se niegue a eliminar la cuenta.
 */
class BorrarCuentaUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
    private val partidaEnCurso: PartidaEnCursoRepository,
    private val observarSesion: ObservarSesionUseCase,
) {
    suspend operator fun invoke(password: String) {
        if (password.isEmpty()) throw ErrorUsuario.CamposIncompletos
        val sesion = observarSesion().first() as? EstadoSesion.Completa ?: throw ErrorUsuario.SinSesion
        val actual = usuarios.obtener(sesion.idPerfil) ?: throw ErrorUsuario.UsuarioInexistente

        try {
            auth.confirmarIdentidad(password)
        } catch (_: ErrorUsuario.CredencialesIncorrectas) {
            throw ErrorUsuario.ContrasenaIncorrecta
        }
        partidaEnCurso.eliminar()
        usuarios.borrarPerfilPropio(actual.jugador, sesion.cuenta)
        auth.borrarCuenta()
    }
}
