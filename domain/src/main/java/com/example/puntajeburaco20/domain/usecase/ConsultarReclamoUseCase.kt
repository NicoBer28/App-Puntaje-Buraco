package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Reclamo
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Busca si alguien dejó un perfil reservado para el mail de la cuenta que inició sesión, que
 * todavía no tiene perfil. Se consulta antes de ofrecerle elegir un nombre de usuario.
 */
class ConsultarReclamoUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
    private val partidasJugadas: PartidasJugadasRepository,
) {
    suspend operator fun invoke(): Reclamo? {
        val cuenta = auth.cuenta.first() ?: throw ErrorUsuario.SinSesion
        if (!cuenta.verificada) throw ErrorUsuario.MailSinVerificar
        val reclamo = usuarios.buscarReclamo(cuenta) ?: return null
        // La cantidad de partidas ayuda a reconocer el perfil, pero no es imprescindible.
        val partidas = try {
            partidasJugadas.contarDe(reclamo.perfil)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return reclamo.copy(partidas = partidas)
    }
}
