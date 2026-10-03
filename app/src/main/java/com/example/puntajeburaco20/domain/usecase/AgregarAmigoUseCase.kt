package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import javax.inject.Inject

class AgregarAmigoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
) {
    suspend operator fun invoke(nombreAmigo: String) {
        if (nombreAmigo.isEmpty()) throw ErrorUsuario.CamposIncompletos
        val actual = obtenerUsuarioActual()
        val idAmigo = Jugador.idDesdeNombre(nombreAmigo)
        if (idAmigo == actual.id) throw ErrorUsuario.EsElUsuarioActual

        val amigo = usuarios.obtener(idAmigo) ?: throw ErrorUsuario.UsuarioInexistente
        if (actual.esAmigoDe(amigo.jugador)) throw ErrorUsuario.YaEsAmigo

        usuarios.agregarAmistad(actual.jugador, amigo.jugador)
    }
}
