package com.example.puntajeburaco20.domain.repository

import kotlinx.coroutines.flow.Flow

/** Usuario que tiene la sesión iniciada en este dispositivo. */
interface SesionRepository {

    /** Id del usuario logueado, o `null` si no hay sesión. Emite de nuevo cada vez que cambia. */
    val usuarioActualId: Flow<String?>

    suspend fun iniciar(usuarioId: String)

    suspend fun cerrar()
}
