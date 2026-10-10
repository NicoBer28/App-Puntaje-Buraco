package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.SesionAnteriorRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * La cuenta que inició sesión se queda con el perfil que esa persona ya tenía, en lugar de crear
 * uno nuevo con [CrearPerfilUseCase]: así conserva sus amigos y su historial.
 *
 * Si el perfil es anterior a las cuentas con mail, lo demuestra con la contraseña que usaba y
 * queda vinculado en el momento. Si se lo creó otra persona nunca tuvo contraseña: se le pide a
 * quien lo creó que confirme que es ella, y mientras tanto queda esperando.
 */
class VincularPerfilAnteriorUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val usuarios: UsuarioRepository,
    private val sesionAnterior: SesionAnteriorRepository,
    private val validador: ValidadorCredenciales,
) {
    /** @return `null` si el perfil quedó vinculado, o el pedido que quedó esperando respuesta. */
    suspend operator fun invoke(nombre: String, passwordAnterior: String): PedidoDeReclamo? {
        if (nombre.isEmpty()) throw ErrorUsuario.CamposIncompletos
        validador.validarNombre(nombre)
        val cuenta = auth.cuenta.first() ?: throw ErrorUsuario.SinSesion
        if (!cuenta.verificada) throw ErrorUsuario.MailSinVerificar

        try {
            usuarios.vincularAnterior(cuenta, nombre, passwordAnterior)
        } catch (_: ErrorUsuario.PerfilCreadoPorOtro) {
            return usuarios.pedirPerfil(cuenta, nombre)
        } catch (e: ErrorUsuario.ContrasenaAnteriorIncorrecta) {
            // Sin contraseña solo se puede pedir un perfil creado por otro.
            throw if (passwordAnterior.isEmpty()) ErrorUsuario.CamposIncompletos else e
        }
        // El dispositivo ya no necesita recordar con qué usuario se entraba antes.
        sesionAnterior.olvidar()
        return null
    }
}
