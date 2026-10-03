package com.example.puntajeburaco20.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.puntajeburaco20.domain.repository.SesionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSesionRepository @Inject constructor(
    private val preferencias: DataStore<Preferences>,
) : SesionRepository {

    override val usuarioActualId: Flow<String?> =
        preferencias.datosSeguros().map { it[CLAVE_USUARIO_ACTUAL] }.distinctUntilChanged()

    override suspend fun iniciar(usuarioId: String) {
        preferencias.edit { it[CLAVE_USUARIO_ACTUAL] = usuarioId }
    }

    override suspend fun cerrar() {
        preferencias.edit { it.remove(CLAVE_USUARIO_ACTUAL) }
    }

    private companion object {
        /** Misma clave que la versión anterior, para no cerrar la sesión al actualizar la app. */
        val CLAVE_USUARIO_ACTUAL = stringPreferencesKey("usuarioActual")
    }
}
