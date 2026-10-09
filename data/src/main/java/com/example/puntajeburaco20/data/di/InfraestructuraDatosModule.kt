package com.example.puntajeburaco20.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.firebase.Firebase
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

    @Provides
    @Singleton
    fun proveerFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun proveerJson(): Json = Json { ignoreUnknownKeys = true }
}
