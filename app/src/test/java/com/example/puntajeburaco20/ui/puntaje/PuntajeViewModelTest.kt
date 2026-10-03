package com.example.puntajeburaco20.ui.puntaje

import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.domain.service.CalculadoraPuntosFichas
import com.example.puntajeburaco20.domain.usecase.RegistrarResultadoPartidaUseCase
import com.example.puntajeburaco20.fakes.FakeDetectorFichas
import com.example.puntajeburaco20.fakes.FakeEstadisticasRepository
import com.example.puntajeburaco20.fakes.FakePartidaEnCursoRepository
import com.example.puntajeburaco20.fakes.FakePartidasJugadasRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.puntaje.PuntajeViewModel.Evento
import com.example.puntajeburaco20.ui.puntaje.PuntajeViewModel.RondaIngresada
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PuntajeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val ana = Jugador("Ana")
    private val beto = Jugador("Beto")
    private val repositorio = FakePartidaEnCursoRepository(Partida.nueva(listOf(ana, beto)))
    private val estadisticas = FakeEstadisticasRepository()
    private val partidasJugadas = FakePartidasJugadasRepository()
    private val detector = FakeDetectorFichas()

    private fun crearViewModel() = PuntajeViewModel(
        partidaEnCurso = repositorio,
        registrarResultado = RegistrarResultadoPartidaUseCase(estadisticas, partidasJugadas),
        calculadora = CalculadoraPuntosFichas(),
        detector = detector,
        io = mainDispatcherRule.dispatcher,
    )

    private fun TestScope.eventosDe(viewModel: PuntajeViewModel): List<Evento> {
        val eventos = mutableListOf<Evento>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventos.toList(eventos) }
        return eventos
    }

    private fun comienza(jugador: Jugador) = Evento.Mensaje(UiText.de(R.string.mensaje_comienza, jugador.nombre))

    @Test
    fun `al entrar se carga la partida guardada y se avisa quien empieza`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        assertEquals(repositorio.partida, viewModel.partida.value)
        assertEquals(listOf(comienza(ana)), eventos)
    }

    @Test
    fun `si no hay partida guardada se sale de la pantalla`() = runTest {
        repositorio.partida = null
        val viewModel = crearViewModel()

        assertEquals(listOf(Evento.Salir), eventosDe(viewModel))
    }

    @Test
    fun `sumar una ronda completa actualiza, guarda y limpia los campos`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.sumarRonda(RondaIngresada("100", "30", "-50", " 20 "))

        val partida = viewModel.partida.value!!
        assertEquals(130, partida.totalUno)
        assertEquals(-30, partida.totalDos)
        assertEquals(PuntajeRonda(-50, 20), partida.ultimaRonda.equipoDos)
        assertEquals(partida, repositorio.partida)
        assertEquals(listOf(comienza(ana), Evento.LimpiarRonda, comienza(beto)), eventos)
    }

    @Test
    fun `una ronda incompleta o invalida no se suma`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)
        val completeCampos = Evento.Mensaje(UiText.de(R.string.error_complete_campos))

        viewModel.sumarRonda(RondaIngresada("100", "", "0", "0"))
        viewModel.sumarRonda(RondaIngresada("100", "-", "0", "0"))

        assertEquals(0, viewModel.partida.value!!.totalUno)
        assertEquals(listOf(comienza(ana), completeCampos, completeCampos), eventos)
    }

    @Test
    fun `una ronda vacia solo recuerda quien empieza`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.sumarRonda(RondaIngresada("", "", "", ""))

        assertEquals(listOf(comienza(ana), comienza(ana)), eventos)
    }

    @Test
    fun `deshacer la ultima ronda la quita, guarda y avisa quien empieza`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)
        viewModel.sumarRonda(RondaIngresada("100", "30", "0", "0"))

        viewModel.deshacerRonda()

        val partida = viewModel.partida.value!!
        assertEquals(0, partida.totalUno)
        assertTrue(partida.rondas.isEmpty())
        assertEquals(partida, repositorio.partida)
        assertEquals(
            listOf(Evento.Mensaje(UiText.de(R.string.mensaje_ronda_deshecha)), comienza(ana)),
            eventos.takeLast(2),
        )
    }

    @Test
    fun `sin rondas no hay nada que deshacer`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.deshacerRonda()

        assertEquals(listOf(comienza(ana)), eventos)
    }

    @Test
    fun `finalizar guarda la partida terminada y registra el resultado`() = runTest {
        val viewModel = crearViewModel()

        viewModel.finalizar(LadoEquipo.UNO)

        assertTrue(viewModel.partida.value!!.terminada)
        assertTrue(repositorio.partida!!.terminada)
        assertEquals(ana, estadisticas.resultados.single().first.jugadores.single())
        assertEquals(viewModel.partida.value, partidasJugadas.guardadas.single().partida)
    }

    @Test
    fun `si no se puede registrar el resultado se avisa pero la partida queda terminada`() = runTest {
        estadisticas.error = IllegalStateException("sin conexión")
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.finalizar(LadoEquipo.DOS)

        assertTrue(viewModel.partida.value!!.terminada)
        assertEquals(
            Evento.Mensaje(UiText.de(R.string.error_guardar_resultado, "sin conexión")),
            eventos.last(),
        )
    }

    @Test
    fun `una partida terminada no acepta rondas al volver a abrirla`() = runTest {
        repositorio.partida = repositorio.partida!!.finalizar(LadoEquipo.UNO)
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.sumarRonda(RondaIngresada("1", "1", "1", "1"))

        assertEquals(0, viewModel.partida.value!!.totalUno)
        assertTrue(eventos.isEmpty())
    }

    @Test
    fun `salir descarta la partida`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.salir()

        assertNull(repositorio.partida)
        assertEquals(Evento.Salir, eventos.last())
    }

    @Test
    fun `la camara se abre solo si el detector esta disponible`() = runTest {
        detector.disponible = false
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.abrirCamara()
        assertFalse(viewModel.camaraActiva.value)
        assertTrue((eventos.last() as Evento.Mensaje).texto.let {
            it is UiText.Recurso && it.id == R.string.error_detector_no_disponible
        })

        detector.disponible = true
        viewModel.abrirCamara()
        assertTrue(viewModel.camaraActiva.value)
    }

    @Test
    fun `sumar fichas sin detecciones cierra la camara y avisa`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)
        viewModel.abrirCamara()

        viewModel.sumarFichasDetectadas(LadoEquipo.UNO)

        assertFalse(viewModel.camaraActiva.value)
        assertEquals(Evento.Mensaje(UiText.de(R.string.mensaje_sin_detecciones)), eventos.last())
    }
}
