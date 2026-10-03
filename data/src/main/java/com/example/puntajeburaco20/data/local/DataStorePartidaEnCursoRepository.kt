package com.example.puntajeburaco20.data.local

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Guarda la partida en curso como JSON. DataStore aplica las escrituras en orden y fuera del hilo principal. */
@Singleton
class DataStorePartidaEnCursoRepository @Inject constructor(
    private val preferencias: DataStore<Preferences>,
    private val json: Json,
) : PartidaEnCursoRepository {

    override suspend fun obtener(): Partida? {
        val guardada = preferencias.datosSeguros().first()[CLAVE_PARTIDA] ?: return null
        return try {
            json.decodeFromString(PartidaGuardada.serializer(), guardada).aDominio()
        } catch (e: IllegalArgumentException) {
            // Incluye SerializationException: JSON corrupto o de un formato desconocido.
            Log.w(TAG, "Partida guardada inválida; se descarta", e)
            null
        }
    }

    override suspend fun guardar(partida: Partida) {
        val serializada = json.encodeToString(PartidaGuardada.serializer(), PartidaGuardada.desde(partida))
        preferencias.edit { it[CLAVE_PARTIDA] = serializada }
    }

    override suspend fun eliminar() {
        preferencias.edit { it.remove(CLAVE_PARTIDA) }
    }

    private companion object {
        const val TAG = "PartidaEnCurso"

        /** Misma clave que la versión anterior, para retomar una partida empezada antes de actualizar. */
        val CLAVE_PARTIDA = stringPreferencesKey("partidaEnCurso")
    }
}
