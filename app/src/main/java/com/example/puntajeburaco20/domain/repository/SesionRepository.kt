package com.example.puntajeburaco20.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Usuario que tiene la sesión iniciada en este dispositivo. */
interface SesionRepository {

    /** Id del usuario logueado, o `null` si no hay sesión. */
    val usuarioActualId: StateFlow<String?>

    fun iniciar(usuarioId: String)
}
