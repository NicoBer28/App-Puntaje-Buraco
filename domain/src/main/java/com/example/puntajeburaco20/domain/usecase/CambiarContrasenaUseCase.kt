package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import javax.inject.Inject

/**
 * Cambia la contraseña de la cuenta actual, que sigue con la sesión iniciada.
 *
 * Pide la contraseña actual: demuestra que quien tiene el teléfono en la mano es el dueño de la
 * cuenta, y el servicio de cuentas solo deja cambiarla a quien la comprobó hace poco.
 */
class CambiarContrasenaUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(actual: String, nueva: String) {
        if (actual.isEmpty()) throw ErrorUsuario.CamposIncompletos
        // La nueva se valida antes de consultar nada: no hace falta conexión para rechazarla.
        validador.validarContrasena(nueva)
        try {
            auth.confirmarIdentidad(actual)
        } catch (_: ErrorUsuario.CredencialesIncorrectas) {
            throw ErrorUsuario.ContrasenaIncorrecta
        }
        auth.cambiarContrasena(nueva)
    }
}
