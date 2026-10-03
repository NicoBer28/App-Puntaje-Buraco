package com.example.puntajeburaco20.di

import com.example.puntajeburaco20.data.firestore.FirestoreEstadisticasRepository
import com.example.puntajeburaco20.data.firestore.FirestoreUsuarioRepository
import com.example.puntajeburaco20.data.local.PreferenciasPartidaEnCursoRepository
import com.example.puntajeburaco20.data.local.PreferenciasSesionRepository
import com.example.puntajeburaco20.data.vision.DetectorFichas
import com.example.puntajeburaco20.data.vision.TfliteDetectorFichas
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.SesionRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Único lugar donde se elige qué implementación usa cada interfaz. Para cambiar de base de datos
 * alcanza con escribir nuevas implementaciones de los repositorios y enlazarlas acá.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun usuarioRepository(impl: FirestoreUsuarioRepository): UsuarioRepository

    @Binds
    abstract fun estadisticasRepository(impl: FirestoreEstadisticasRepository): EstadisticasRepository

    @Binds
    abstract fun sesionRepository(impl: PreferenciasSesionRepository): SesionRepository

    @Binds
    abstract fun partidaEnCursoRepository(impl: PreferenciasPartidaEnCursoRepository): PartidaEnCursoRepository

    @Binds
    abstract fun detectorFichas(impl: TfliteDetectorFichas): DetectorFichas
}
