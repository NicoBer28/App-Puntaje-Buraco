package com.example.puntajeburaco20.data.local

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.puntajeburaco20.di.IoDispatcher
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Guarda la partida en curso como JSON dentro de las preferencias de la app. */
@Singleton
class PreferenciasPartidaEnCursoRepository @Inject constructor(
    private val preferencias: SharedPreferences,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PartidaEnCursoRepository {

    private val escrituras = Mutex()

    override suspend fun obtener(): Partida? = withContext(io) {
        val guardada = preferencias.getString(CLAVE_PARTIDA, null) ?: return@withContext null
        try {
            json.decodeFromString(PartidaGuardada.serializer(), guardada).aDominio()
        } catch (e: IllegalArgumentException) {
            // Incluye SerializationException: JSON corrupto o de un formato anterior.
            Log.w(TAG, "Partida guardada inválida; se descarta", e)
            null
        }
    }

    override suspend fun guardar(partida: Partida) = escribir {
        val serializada = json.encodeToString(PartidaGuardada.serializer(), PartidaGuardada.desde(partida))
        putString(CLAVE_PARTIDA, serializada)
    }

    override suspend fun eliminar() = escribir { remove(CLAVE_PARTIDA) }

    /** Las escrituras se aplican en el orden en que se pidieron, aunque corran en otro hilo. */
    private suspend fun escribir(cambio: SharedPreferences.Editor.() -> Unit) = escrituras.withLock {
        withContext(io) { preferencias.edit(commit = true, action = cambio) }
    }

    private companion object {
        const val TAG = "PartidaEnCurso"
        const val CLAVE_PARTIDA = "partidaEnCurso"
    }
}
