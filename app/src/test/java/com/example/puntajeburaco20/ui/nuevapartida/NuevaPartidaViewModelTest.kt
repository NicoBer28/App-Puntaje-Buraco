package com.example.puntajeburaco20.ui.nuevapartida

import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.usecase.ObservarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarUsuarioActualUseCase
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakeSincronizacionRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import com.example.puntajeburaco20.ui.nuevapartida.NuevaPartidaViewModel.Evento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NuevaPartidaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val usuarios = FakeUsuarioRepository().apply {
        registrar("Ana")
        registrar("Beto")
    }
    private val auth = FakeAuthRepository(cuentaDe("Ana"))
    private val partidaEnCurso = FakePartidaEnCursoRepository()
    private val sincronizacion = FakeSincronizacionRepository()

    private fun crearViewModel() = NuevaPartidaViewModel(
        partidaEnCurso = partidaEnCurso,
        observarUsuarioActual = ObservarUsuarioActualUseCase(usuarios, ObservarSesionUseCase(auth, usuarios)),
        sincronizacion = sincronizacion,
    )

    private fun TestScope.eventosDe(viewModel: NuevaPartidaViewModel): List<Evento> {
        val eventos = mutableListOf<Evento>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventos.toList(eventos) }
        return eventos
    }

    @Test
    fun `muestra al usuario de la sesion y lo ofrece como jugador junto a sus amigos`() = runTest {
        usuarios.agregarAmistad(jugador("Ana"), jugador("Beto"))

        val viewModel = crearViewModel()

        assertEquals("Ana", viewModel.estado.value.nombreUsuario)
        assertEquals(listOf(jugador("Ana"), jugador("Beto")), viewModel.estado.value.seleccion.disponibles)
    }

    @Test
    fun `al cerrarse la sesion se vacia la seleccion`() = runTest {
        val viewModel = crearViewModel()
        viewModel.elegirJugador(0, jugador("Ana"))

        auth.cerrarSesion()

        assertNull(viewModel.estado.value.seleccion.elegido(0))
    }

    @Test
    fun `si quedo una partida en curso se vuelve a ella`() = runTest {
        partidaEnCurso.partida = Partida.nueva(listOf(jugador("Ana"), jugador("Beto")))
        val viewModel = crearViewModel()

        assertEquals(listOf(Evento.IrAPartida), eventosDe(viewModel))
    }

    @Test
    fun `solo se avisa de cambios pendientes si tardan en subirse`() = runTest {
        val viewModel = crearViewModel()

        sincronizacion.hayCambiosPendientes.value = true
        advanceTimeBy(1_000)
        assertFalse(viewModel.estado.value.cambiosPendientes)

        advanceTimeBy(1_000)
        assertTrue(viewModel.estado.value.cambiosPendientes)

        sincronizacion.hayCambiosPendientes.value = false
        runCurrent()
        assertFalse(viewModel.estado.value.cambiosPendientes)
    }
}
