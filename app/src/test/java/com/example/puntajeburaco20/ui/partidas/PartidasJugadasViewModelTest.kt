package com.example.puntajeburaco20.ui.partidas

import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.usecase.ConsultarHistorialJugadorUseCase
import com.example.puntajeburaco20.domain.usecase.ObtenerUsuarioActualUseCase
import com.example.puntajeburaco20.fakes.FakePartidasJugadasRepository
import com.example.puntajeburaco20.fakes.FakeSesionRepository
import com.example.puntajeburaco20.fakes.FakeUsuarioRepository
import com.example.puntajeburaco20.fakes.MainDispatcherRule
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.partidas.PartidasJugadasViewModel.Evento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PartidasJugadasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val ana = Jugador("Ana")
    private val beto = Jugador("Beto")
    private val usuarios = FakeUsuarioRepository().apply { registrar("Ana") }
    private val partidasJugadas = FakePartidasJugadasRepository()

    private fun crearViewModel() = PartidasJugadasViewModel(
        ConsultarHistorialJugadorUseCase(
            partidasJugadas,
            ObtenerUsuarioActualUseCase(usuarios, FakeSesionRepository(usuarioInicial = "ana")),
        ),
    )

    @Test
    fun `al entrar se cargan las partidas del usuario, la mas reciente primero`() = runTest {
        partidasJugadas.guardar(Partida.nueva(listOf(ana, beto)).finalizar(LadoEquipo.DOS))
        partidasJugadas.guardar(Partida.nueva(listOf(beto, ana)).finalizar(LadoEquipo.DOS))

        val estado = crearViewModel().estado.value

        assertFalse(estado.cargando)
        val historial = estado.historial!!
        assertEquals(listOf(1L, 0L), historial.partidas.map { it.fecha })
        assertEquals(1, historial.rachaActual)
    }

    @Test
    fun `si falla la consulta se avisa`() = runTest {
        partidasJugadas.error = IllegalStateException("sin conexión")
        val viewModel = crearViewModel()
        val eventos = mutableListOf<Evento>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventos.toList(eventos) }

        assertNull(viewModel.estado.value.historial)
        assertEquals(
            listOf(Evento.Mensaje(UiText.de(R.string.error_cargar_partidas, "sin conexión"))),
            eventos,
        )
    }
}
