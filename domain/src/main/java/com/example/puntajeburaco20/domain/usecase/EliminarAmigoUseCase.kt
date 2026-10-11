package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import javax.inject.Inject

class EliminarAmigoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
) {
    suspend operator fun invoke(nombreAmigo: String) {
        if (nombreAmigo.isEmpty()) throw ErrorUsuario.CamposIncompletos
        val actual = obtenerUsuarioActual()

        val amigo = usuarios.buscarPorNombre(nombreAmigo) ?: throw ErrorUsuario.UsuarioInexistente
        if (amigo.id == actual.id) throw ErrorUsuario.EsElUsuarioActual
        // Las amistades son siempre de a dos: alcanza con mirar la lista propia.
        if (!actual.esAmigoDe(amigo)) throw ErrorUsuario.NoEsAmigo

        usuarios.eliminarAmistad(actual.jugador, amigo)
    }
}
