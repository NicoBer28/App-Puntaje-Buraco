package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.model.ModoTema
import kotlinx.coroutines.flow.Flow

/** Preferencias de la app en este dispositivo. No dependen de quién tenga la sesión iniciada. */
interface PreferenciasRepository {

    /** Emite de nuevo cada vez que cambia. */
    val modoTema: Flow<ModoTema>

    suspend fun cambiarModoTema(modo: ModoTema)
}
