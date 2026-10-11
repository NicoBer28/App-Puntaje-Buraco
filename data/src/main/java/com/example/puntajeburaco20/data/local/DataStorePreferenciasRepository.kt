package com.example.puntajeburaco20.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.puntajeburaco20.domain.model.LimitePuntos
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.model.Recorrido
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

    /** Sin nada guardado, o con un valor fuera de rango, queda el límite de fábrica. */
    override val limitePuntos: Flow<LimitePuntos> = preferencias.datosSeguros()
        .map { guardadas ->
            LimitePuntos(
                puntos = guardadas[CLAVE_LIMITE_PUNTOS]?.takeIf { it in LimitePuntos.RANGO } ?: LimitePuntos.PREDETERMINADO,
                activo = guardadas[CLAVE_LIMITE_ACTIVO] ?: false,
            )
        }
        .distinctUntilChanged()

    override suspend fun cambiarLimitePuntos(limite: LimitePuntos) {
        preferencias.edit {
            it[CLAVE_LIMITE_PUNTOS] = limite.puntos
            it[CLAVE_LIMITE_ACTIVO] = limite.activo
        }
    }

    override val pantallaEncendida: Flow<Boolean> = preferencias.datosSeguros()
        .map { guardadas -> guardadas[CLAVE_PANTALLA_ENCENDIDA] ?: false }
        .distinctUntilChanged()

    override suspend fun cambiarPantallaEncendida(encendida: Boolean) {
        preferencias.edit { it[CLAVE_PANTALLA_ENCENDIDA] = encendida }
    }

    /** Un nombre desconocido (de otra versión de la app) se ignora. */
    override val recorridosVistos: Flow<Set<Recorrido>> = preferencias.datosSeguros()
        .map { guardadas ->
            val nombres = guardadas[CLAVE_RECORRIDOS_VISTOS].orEmpty()
            Recorrido.entries.filterTo(mutableSetOf()) { it.name in nombres }
        }
        .distinctUntilChanged()

    override suspend fun marcarRecorridoVisto(recorrido: Recorrido) {
        preferencias.edit { it[CLAVE_RECORRIDOS_VISTOS] = it[CLAVE_RECORRIDOS_VISTOS].orEmpty() + recorrido.name }
    }

    override suspend fun reiniciarRecorridos() {
        preferencias.edit { it.remove(CLAVE_RECORRIDOS_VISTOS) }
    }

    private companion object {
        val CLAVE_MODO_TEMA = stringPreferencesKey("modoTema")
        val CLAVE_LIMITE_PUNTOS = intPreferencesKey("limitePuntos")
        val CLAVE_LIMITE_ACTIVO = booleanPreferencesKey("limiteActivo")
        val CLAVE_PANTALLA_ENCENDIDA = booleanPreferencesKey("pantallaEncendida")
        val CLAVE_RECORRIDOS_VISTOS = stringSetPreferencesKey("recorridosVistos")
    }
}
