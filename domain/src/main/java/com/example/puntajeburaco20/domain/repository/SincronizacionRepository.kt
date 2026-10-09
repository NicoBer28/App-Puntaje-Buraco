package com.example.puntajeburaco20.domain.repository

import kotlinx.coroutines.flow.Flow

/** Cambios guardados en el dispositivo que todavía no llegaron al servidor (ej. sin conexión). */
interface SincronizacionRepository {

    /** Emite `true` mientras haya cambios pendientes de subir. */
    val hayCambiosPendientes: Flow<Boolean>
}
