package com.example.puntajeburaco20.domain.repository

import com.example.puntajeburaco20.domain.model.Partida

/** Guarda la partida que se está jugando para poder retomarla si se cierra la app. */
interface PartidaEnCursoRepository {

    suspend fun obtener(): Partida?

    suspend fun guardar(partida: Partida)

    suspend fun eliminar()
}
