package com.example.puntajeburaco20.ui.historial

import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.model.ModoJuego
import com.example.puntajeburaco20.domain.usecase.ConsultarEstadisticasUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarUsuarioActualUseCase
import com.example.puntajeburaco20.fakes.FakeAuthRepository
import com.example.puntajeburaco20.fakes.FakeEstadisticasRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.fakes.cuentaDe
import com.example.puntajeburaco20.fakes.jugador
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.historial.HistorialViewModel.Evento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val ana = jugador("Ana")
    private val beto = jugador("Beto")
    private val caro = jugador("Caro")
    private val dani = jugador("Dani")

    private val usuarios = FakeUsuarioRepository()
    private val estadisticas = FakeEstadisticasRepository()

    private fun crearViewModel(): HistorialViewModel {
        listOf(ana, beto, caro, dani).forEach { usuarios.registrar(it.nombre) }
        val sesion = ObservarSesionUseCase(FakeAuthRepository(cuentaDe("Ana")), usuarios)
        return HistorialViewModel(
            observarUsuarioActual = ObservarUsuarioActualUseCase(usuarios, sesion),
            consultarEstadisticas = ConsultarEstadisticasUseCase(estadisticas),
        )
    }

    private fun TestScope.eventosDe(viewModel: HistorialViewModel): List<Evento> {
        val eventos = mutableListOf<Evento>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventos.toList(eventos) }
        return eventos
    }

    private fun mensaje(id: Int) = Evento.Mensaje(UiText.de(id))

    @Test
    fun `sin jugador elegido pide completar el primer campo`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.buscar()

        assertEquals(listOf(mensaje(R.string.error_complete_primer_campo)), eventos)
    }

    @Test
    fun `con un solo jugador muestra sus totales generales`() = runTest {
        estadisticas.generales["ana"] = Estadisticas(jugadas = 8, ganadas = 5)
        val viewModel = crearViewModel()

        viewModel.elegirJugador(0, ana)
        viewModel.buscar()

        assertEquals(Estadisticas(8, 5), viewModel.estado.value.equipoUno)
        assertEquals(Estadisticas.VACIAS, viewModel.estado.value.equipoDos)
    }

    @Test
    fun `con rival muestra el enfrentamiento desde ambos lados`() = runTest {
        estadisticas.enfrentamientos["ana" to "beto"] = Estadisticas(jugadas = 6, ganadas = 4)
        val viewModel = crearViewModel()

        viewModel.elegirJugador(0, ana)
        viewModel.elegirJugador(1, beto)
        viewModel.buscar()

        assertEquals(Estadisticas(6, 4), viewModel.estado.value.equipoUno)
        assertEquals(Estadisticas(6, 2), viewModel.estado.value.equipoDos)
    }

    @Test
    fun `avisa si nunca se enfrentaron`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)

        viewModel.elegirJugador(0, ana)
        viewModel.elegirJugador(1, beto)
        viewModel.buscar()

        assertEquals(listOf(mensaje(R.string.mensaje_nunca_jugaron_entre_ellos)), eventos)
    }

    @Test
    fun `en parejas el equipo de la izquierda son las posiciones 0 y 2`() = runTest {
        estadisticas.enfrentamientos["ana|caro" to "beto|dani"] = Estadisticas(jugadas = 3, ganadas = 3)
        val viewModel = crearViewModel()

        viewModel.cambiarModo(ModoJuego.PAREJAS)
        viewModel.elegirJugador(0, ana)
        viewModel.elegirJugador(1, beto)
        viewModel.elegirJugador(2, caro)
        viewModel.elegirJugador(3, dani)
        viewModel.buscar()

        assertEquals(Estadisticas(3, 3), viewModel.estado.value.equipoUno)
        assertEquals(Estadisticas(3, 0), viewModel.estado.value.equipoDos)
    }

    @Test
    fun `en parejas valida que cada equipo este completo`() = runTest {
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)
        viewModel.cambiarModo(ModoJuego.PAREJAS)
        viewModel.elegirJugador(0, ana)

        viewModel.buscar()
        viewModel.elegirJugador(2, caro)
        viewModel.elegirJugador(1, beto)
        viewModel.buscar()
        viewModel.elegirJugador(1, null)
        viewModel.buscar()

        assertEquals(
            listOf(
                mensaje(R.string.error_complete_primer_campo),
                mensaje(R.string.error_complete_segundo_campo),
                mensaje(R.string.mensaje_nunca_jugaron_juntos),
            ),
            eventos,
        )
    }

    @Test
    fun `cambiar la seleccion borra el resultado anterior`() = runTest {
        estadisticas.generales["ana"] = Estadisticas(jugadas = 8, ganadas = 5)
        val viewModel = crearViewModel()
        viewModel.elegirJugador(0, ana)
        viewModel.buscar()

        viewModel.elegirJugador(1, beto)

        assertEquals(Estadisticas.VACIAS, viewModel.estado.value.equipoUno)
    }

    @Test
    fun `sin conexion avisa que las estadisticas la necesitan y no muestra nada`() = runTest {
        estadisticas.error = ErrorUsuario.SinConexion
        val viewModel = crearViewModel()
        val eventos = eventosDe(viewModel)
        viewModel.elegirJugador(0, ana)

        viewModel.buscar()

        assertEquals(listOf(Evento.Mensaje(UiText.de(R.string.error_estadisticas_sin_conexion))), eventos)
        assertEquals(Estadisticas.VACIAS, viewModel.estado.value.equipoUno)
        assertFalse(viewModel.estado.value.buscando)
    }
}
