package com.example.puntajeburaco20.di

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Provee las dependencias externas (SDKs, frameworks) que no se pueden construir con @Inject. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val ARCHIVO_PREFERENCIAS = "PuntajeBuracoPreferences"

    @Provides
    @Singleton
    fun proveerPreferencias(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(ARCHIVO_PREFERENCIAS, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun proveerFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun proveerJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @IoDispatcher
    fun proveerIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
