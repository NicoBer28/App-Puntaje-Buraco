package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import javax.inject.Inject

class EliminarAmigoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
) {
    suspend operator fun invoke(nombreAmigo: String) {
        if (nombreAmigo.isEmpty()) throw ErrorUsuario.CamposIncompletos
        val actual = obtenerUsuarioActual()
        val idAmigo = Jugador.idDesdeNombre(nombreAmigo)
        if (idAmigo == actual.id) throw ErrorUsuario.EsElUsuarioActual

        val amigo = usuarios.obtener(idAmigo) ?: throw ErrorUsuario.UsuarioInexistente
        if (!amigo.esAmigoDe(actual.jugador)) throw ErrorUsuario.NoEsAmigo

        usuarios.eliminarAmistad(actual.jugador, amigo.jugador)
    }
}
