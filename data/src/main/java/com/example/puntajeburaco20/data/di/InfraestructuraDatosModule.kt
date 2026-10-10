package com.example.puntajeburaco20.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.puntajeburaco20.data.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provee los SDKs que usa la capa de datos. Viven en este módulo para que el resto de la app no
 * dependa de Firestore ni de DataStore.
 */
@Module
@InstallIn(SingletonComponent::class)
object InfraestructuraDatosModule {

    private const val ARCHIVO_PREFERENCIAS = "preferencias"

    /** Archivo de SharedPreferences de versiones anteriores; se migra a DataStore una sola vez. */
    private const val ARCHIVO_PREFERENCIAS_ANTERIOR = "PuntajeBuracoPreferences"

    @Provides
    @Singleton
    fun proveerPreferencias(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(SharedPreferencesMigration(context, ARCHIVO_PREFERENCIAS_ANTERIOR)),
            produceFile = { context.preferencesDataStoreFile(ARCHIVO_PREFERENCIAS) },
        )

    /** Host de los emuladores de Firebase, o `null` para usar el proyecto real. Solo en debug. */
    private val hostEmuladores: String? = BuildConfig.HOST_EMULADORES

    private const val PUERTO_EMULADOR_FIRESTORE = 8080
    private const val PUERTO_EMULADOR_AUTH = 9099

    // useEmulator() solo se puede llamar antes del primer uso de cada SDK: por eso va acá, donde
    // se crea la única instancia.
    @Provides
    @Singleton
    fun proveerFirestore(): FirebaseFirestore = Firebase.firestore.apply {
        hostEmuladores?.let { useEmulator(it, PUERTO_EMULADOR_FIRESTORE) }
    }

    @Provides
    @Singleton
    fun proveerAuth(): FirebaseAuth = Firebase.auth.apply {
        hostEmuladores?.let { useEmulator(it, PUERTO_EMULADOR_AUTH) }
    }

    @Provides
    @Singleton
    fun proveerJson(): Json = Json { ignoreUnknownKeys = true }
}
