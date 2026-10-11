package com.example.puntajeburaco20.data.local

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

/** Los datos guardados; si el archivo no se puede leer, se sigue como si estuviera vacío. */
internal fun DataStore<Preferences>.datosSeguros(): Flow<Preferences> = data.catch { error ->
    if (error !is IOException) throw error
    Log.w("DataStore", "No se pudieron leer las preferencias", error)
    emit(emptyPreferences())
}
