package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import javax.inject.Inject

/** Cierra la sesión. La partida en curso es de quien la empezó, así que también se descarta. */
class CerrarSesionUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val partidaEnCurso: PartidaEnCursoRepository,
) {
    suspend operator fun invoke() {
        partidaEnCurso.eliminar()
        auth.cerrarSesion()
    }
}
