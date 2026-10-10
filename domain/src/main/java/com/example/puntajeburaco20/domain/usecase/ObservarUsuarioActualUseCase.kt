package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Sigue al usuario logueado: emite de nuevo si cambia la sesión o sus datos (ej. sus amigos). */
class ObservarUsuarioActualUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val observarSesion: ObservarSesionUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Usuario?> = observarSesion().flatMapLatest { sesion ->
        if (sesion is EstadoSesion.Completa) usuarios.observar(sesion.idPerfil) else flowOf(null)
    }
}
