package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.example.puntajeburaco20.domain.service.ValidadorCredenciales
import javax.inject.Inject

/** Crea la cuenta de otra persona y la agrega directamente como amiga del usuario actual. */
class CrearUsuarioAmigoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
    private val validador: ValidadorCredenciales,
) {
    suspend operator fun invoke(nombre: String, password: String) {
        validador.validar(nombre, password)
        val actual = obtenerUsuarioActual()
        val id = Jugador.idDesdeNombre(nombre)
        if (id == actual.id) throw ErrorUsuario.EsElUsuarioActual
        if (usuarios.obtener(id) != null) throw ErrorUsuario.NombreEnUso

        val nuevo = usuarios.crear(nombre, password)
        usuarios.agregarAmistad(actual.jugador, nuevo.jugador)
    }
}
