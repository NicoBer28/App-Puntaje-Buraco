package com.example.puntajeburaco20.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Provee las dependencias de la app que no se pueden construir con @Inject. Las de la capa de
 * datos (Firestore, DataStore) las provee el módulo :data.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @IoDispatcher
    fun proveerIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
