package com.example.puntajeburaco20.di

import android.graphics.Bitmap
import com.example.puntajeburaco20.data.vision.DetectorFichas
import com.example.puntajeburaco20.domain.model.FichaDetectada
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.domain.repository.SincronizacionRepository
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakeEstadisticasRepository
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakePartidasJugadasRepository
import com.example.puntajeburaco20.fakes.FakePreferenciasRepository
import com.example.puntajeburaco20.fakes.FakeSincronizacionRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.IOException
import javax.inject.Singleton

/**
 * Reemplaza a [DataModule] en los tests de UI: la app usa repositorios en memoria, así que los
 * tests no tocan Firestore ni dependen de la red. Cada test arranca con repositorios vacíos.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
object RepositoriosEnMemoriaModule {

    @Provides
    @Singleton
    fun usuarios() = FakeUsuarioRepository()

    @Provides
    @Singleton
    fun auth() = FakeAuthRepository()

    @Provides
    @Singleton
    fun partidaEnCurso() = FakePartidaEnCursoRepository()

    @Provides
    @Singleton
    fun estadisticas(partidasJugadas: FakePartidasJugadasRepository) = FakeEstadisticasRepository(partidasJugadas)

    @Provides
    @Singleton
    fun partidasJugadas() = FakePartidasJugadasRepository()

    @Provides
    @Singleton
    fun preferencias() = FakePreferenciasRepository()

    @Provides
    fun usuarioRepository(fake: FakeUsuarioRepository): UsuarioRepository = fake

    @Provides
    fun authRepository(fake: FakeAuthRepository): AuthRepository = fake

    @Provides
    fun partidaEnCursoRepository(fake: FakePartidaEnCursoRepository): PartidaEnCursoRepository = fake

    @Provides
    fun estadisticasRepository(fake: FakeEstadisticasRepository): EstadisticasRepository = fake

    @Provides
    fun partidasJugadasRepository(fake: FakePartidasJugadasRepository): PartidasJugadasRepository = fake

    @Provides
    fun preferenciasRepository(fake: FakePreferenciasRepository): PreferenciasRepository = fake

    @Provides
    @Singleton
    fun sincronizacionRepository(): SincronizacionRepository = FakeSincronizacionRepository()

    /** Los tests no ejercitan la cámara: el detector se comporta como si faltara el modelo. */
    @Provides
    fun detectorFichas(): DetectorFichas = object : DetectorFichas {
        override fun preparar() = throw IOException("Sin modelo en los tests")
        override fun detectar(imagen: Bitmap, rotacion: Int): List<FichaDetectada> = emptyList()
    }
}
