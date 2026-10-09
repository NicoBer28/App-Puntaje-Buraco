package com.example.puntajeburaco20.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStorePreferenciasRepository @Inject constructor(
    private val preferencias: DataStore<Preferences>,
) : PreferenciasRepository {

    /** Sin nada guardado, o con un valor desconocido, se sigue al sistema. */
    override val modoTema: Flow<ModoTema> = preferencias.datosSeguros()
        .map { guardadas -> ModoTema.entries.firstOrNull { it.name == guardadas[CLAVE_MODO_TEMA] } ?: ModoTema.SISTEMA }
        .distinctUntilChanged()

    override suspend fun cambiarModoTema(modo: ModoTema) {
        preferencias.edit { it[CLAVE_MODO_TEMA] = modo.name }
    }

    private companion object {
        val CLAVE_MODO_TEMA = stringPreferencesKey("modoTema")
    }
}
