package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import javax.inject.Inject

/**
 * Cambia el nombre de usuario de un perfil: el del usuario actual, o el de uno sin cuenta que él
 * creó para otra persona. Las partidas ya jugadas conservan el nombre anterior.
 */
class RenombrarPerfilUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(perfil: Jugador, nombreNuevo: String): Jugador {
        validador.validarNombre(nombreNuevo)
        if (nombreNuevo == perfil.nombre) return perfil
        // Falla con NombreEnUso si el nombre ya es de otro perfil.
        return usuarios.renombrar(perfil, nombreNuevo)
    }
}
