package com.example.puntajeburaco20.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.puntajeburaco20.domain.repository.SesionAnteriorRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Las versiones anteriores a las cuentas con mail guardaban acá el usuario con la sesión
 * iniciada. La app ya no lo escribe: solo lo lee para ofrecerle a esa persona su perfil, y lo
 * borra cuando lo vincula.
 */
@Singleton
class DataStoreSesionAnteriorRepository @Inject constructor(
    private val preferencias: DataStore<Preferences>,
) : SesionAnteriorRepository {

    override suspend fun nombreDeUsuario(): String? = preferencias.datosSeguros().first()[CLAVE_USUARIO_ACTUAL]

    override suspend fun olvidar() {
        preferencias.edit { it.remove(CLAVE_USUARIO_ACTUAL) }
    }

    private companion object {
        val CLAVE_USUARIO_ACTUAL = stringPreferencesKey("usuarioActual")
    }
}
