package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.SesionRepository
import javax.inject.Inject

/** Cierra la sesión. La partida en curso es de quien la empezó, así que también se descarta. */
class CerrarSesionUseCase @Inject constructor(
    private val sesion: SesionRepository,
    private val partidaEnCurso: PartidaEnCursoRepository,
) {
    suspend operator fun invoke() {
        partidaEnCurso.eliminar()
        sesion.cerrar()
    }
}
