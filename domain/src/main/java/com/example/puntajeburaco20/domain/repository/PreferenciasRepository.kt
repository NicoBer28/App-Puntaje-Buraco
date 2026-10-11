package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.model.Recorrido
import kotlinx.coroutines.flow.Flow

/** Preferencias de la app en este dispositivo. No dependen de quién tenga la sesión iniciada. */
interface PreferenciasRepository {

    /** Emite de nuevo cada vez que cambia. */
    val modoTema: Flow<ModoTema>

    suspend fun cambiarModoTema(modo: ModoTema)

    /** Los recorridos de ayuda que ya se mostraron. Emite de nuevo cada vez que cambia. */
    val recorridosVistos: Flow<Set<Recorrido>>

    suspend fun marcarRecorridoVisto(recorrido: Recorrido)

    /** Deja todos los recorridos como si no se hubieran visto, para mostrarlos otra vez. */
    suspend fun reiniciarRecorridos()
}
