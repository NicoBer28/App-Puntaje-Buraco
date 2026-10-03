package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.SesionRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Sigue al usuario logueado: emite de nuevo si cambia la sesión o sus datos (ej. sus amigos). */
class ObservarUsuarioActualUseCase @Inject constructor(
    private val usuarios: UsuarioRepository,
    private val sesion: SesionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Usuario?> = sesion.usuarioActualId.flatMapLatest { id ->
        if (id == null) flowOf(null) else usuarios.observar(id)
    }
}
