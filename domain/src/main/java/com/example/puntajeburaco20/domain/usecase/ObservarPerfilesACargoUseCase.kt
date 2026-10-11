package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Sigue los perfiles que el usuario actual creó para otros y que todavía nadie aceptó. */
class ObservarPerfilesACargoUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val observarSesion: ObservarSesionUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<PerfilACargo>> = observarSesion().flatMapLatest { sesion ->
        if (sesion is EstadoSesion.Completa) usuarios.observarPerfilesACargo(sesion.cuenta) else flowOf(emptyList())
    }
}
