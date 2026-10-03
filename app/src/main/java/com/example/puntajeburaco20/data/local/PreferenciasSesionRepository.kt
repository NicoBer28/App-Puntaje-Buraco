package com.example.puntajeburaco20.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.puntajeburaco20.domain.repository.SesionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenciasSesionRepository @Inject constructor(
    private val preferencias: SharedPreferences,
) : SesionRepository {

    private val usuarioActual = MutableStateFlow(preferencias.getString(CLAVE_USUARIO_ACTUAL, null))

    override val usuarioActualId: StateFlow<String?> = usuarioActual.asStateFlow()

    override fun iniciar(usuarioId: String) {
        preferencias.edit { putString(CLAVE_USUARIO_ACTUAL, usuarioId) }
        usuarioActual.value = usuarioId
    }

    private companion object {
        /** Misma clave que la versión anterior, para no cerrar la sesión al actualizar la app. */
        const val CLAVE_USUARIO_ACTUAL = "usuarioActual"
    }
}
