package com.example.puntajeburaco20.ui.perfil

import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarUsuarioActualUseCase
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakePreferenciasRepository
import com.example.puntajeburaco20.fakes.FakeSesionRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.ui.perfil.PerfilViewModel.Evento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val usuarios = FakeUsuarioRepository().apply {
        registrar("Ana")
        registrar("Beto")
    }
    private val sesion = FakeSesionRepository(usuarioInicial = "ana")
    private val partidaEnCurso = FakePartidaEnCursoRepository()
    private val preferencias = FakePreferenciasRepository()

    private fun crearViewModel() = PerfilViewModel(
        preferencias = preferencias,
        cerrarSesionUseCase = CerrarSesionUseCase(sesion, partidaEnCurso),
        observarUsuarioActual = ObservarUsuarioActualUseCase(usuarios, sesion),
    )

    private fun TestScope.eventosDe(viewModel: PerfilViewModel): List<Evento> {
        val eventos = mutableListOf<Evento>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventos.toList(eventos) }
        return eventos
    }

    @Test
    fun `muestra al usuario y cuantos amigos tiene`() = runTest {
        val viewModel = crearViewModel()
        assertEquals("Ana", viewModel.estado.value.nombreUsuario)
        assertEquals(0, viewModel.estado.value.cantidadAmigos)

        usuarios.agregarAmistad(Jugador("Ana"), Jugador("Beto"))
        assertEquals(1, viewModel.estado.value.cantidadAmigos)
    }

    @Test
    fun `cambiar el tema lo guarda y lo refleja`() = runTest {
        val viewModel = crearViewModel()
        assertEquals(ModoTema.SISTEMA, viewModel.estado.value.modoTema)

        viewModel.cambiarTema(ModoTema.OSCURO)

        assertEquals(ModoTema.OSCURO, preferencias.modoTema.value)
        assertEquals(ModoTema.OSCURO, viewModel.estado.value.modoTema)
    }

    @Test
    fun `cerrar sesion descarta la partida en curso y avisa a la pantalla`() = runTest {
        partidaEnCurso.partida = Partida.nueva(listOf(Jugador("Ana"), Jugador("Beto")))
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.cerrarSesion()

        assertNull(sesion.usuarioActualId.value)
        assertNull(partidaEnCurso.partida)
        assertEquals(listOf(Evento.SesionCerrada), eventos)
        // Mientras se sale de la pantalla se sigue viendo a quien cerró la sesión.
        assertEquals("Ana", viewModel.estado.value.nombreUsuario)
    }
}
