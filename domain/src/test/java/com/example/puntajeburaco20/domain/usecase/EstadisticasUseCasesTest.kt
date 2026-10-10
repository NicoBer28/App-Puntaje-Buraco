package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.fakes.FakeEstadisticasRepository
import com.example.puntajeburaco20.fakes.FakePartidasJugadasRepository
import com.example.puntajeburaco20.fakes.jugador
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EstadisticasUseCasesTest {

    private val partidasJugadas = FakePartidasJugadasRepository()
    private val repositorio = FakeEstadisticasRepository(partidasJugadas)
    private val registrarResultado = RegistrarResultadoPartidaUseCase(partidasJugadas)
    private val consultar = ConsultarEstadisticasUseCase(repositorio)
    private val ana = jugador("Ana")
    private val beto = jugador("Beto")
    private val caro = jugador("Caro")
    private val dani = jugador("Dani")

    @Test
    fun `una partida registrada cuenta en las estadisticas de los dos equipos`() = runTest {
        registrarResultado(Partida.nueva(listOf(ana, beto)).finalizar(LadoEquipo.DOS))
        registrarResultado(Partida.nueva(listOf(beto, ana)).finalizar(LadoEquipo.DOS))
        registrarResultado(Partida.nueva(listOf(ana, caro)).finalizar(LadoEquipo.UNO))

        assertEquals(Estadisticas(jugadas = 3, ganadas = 2), consultar(Equipo(listOf(ana)), rival = null))
        assertEquals(Estadisticas(jugadas = 2, ganadas = 1), consultar(Equipo(listOf(ana)), Equipo(listOf(beto))))
        assertEquals(Estadisticas(jugadas = 2, ganadas = 1), consultar(Equipo(listOf(beto)), Equipo(listOf(ana))))
        assertNull(consultar(Equipo(listOf(beto)), Equipo(listOf(caro))))
    }

    @Test
    fun `las partidas en pareja no cuentan para cada jugador por separado`() = runTest {
        registrarResultado(Partida.nueva(listOf(ana, beto, caro, dani)).finalizar(LadoEquipo.UNO))

        // El orden de los integrantes no cambia de qué pareja se trata.
        assertEquals(Estadisticas(jugadas = 1, ganadas = 1), consultar(Equipo(listOf(beto, ana)), rival = null))
        assertEquals(
            Estadisticas(jugadas = 1, ganadas = 0),
            consultar(Equipo(listOf(caro, dani)), Equipo(listOf(ana, beto))),
        )
        assertNull(consultar(Equipo(listOf(ana)), rival = null))
        assertNull(consultar(Equipo(listOf(ana, caro)), rival = null))
    }

    @Test
    fun `a las partidas registradas se les suman las estadisticas previas`() = runTest {
        repositorio.generales["ana"] = Estadisticas(jugadas = 10, ganadas = 4)
        repositorio.enfrentamientos["ana" to "beto"] = Estadisticas(jugadas = 3, ganadas = 1)
        registrarResultado(Partida.nueva(listOf(ana, beto)).finalizar(LadoEquipo.UNO))

        assertEquals(Estadisticas(jugadas = 11, ganadas = 5), consultar(Equipo(listOf(ana)), rival = null))
        assertEquals(Estadisticas(jugadas = 4, ganadas = 2), consultar(Equipo(listOf(ana)), Equipo(listOf(beto))))
    }

    @Test
    fun `registrar el resultado guarda la partida completa en el historial`() = runTest {
        val partida = Partida.nueva(listOf(ana, beto))
            .registrarRonda(PuntajeRonda(100, 20), PuntajeRonda(0, 5))
            .finalizar(LadoEquipo.UNO)

        registrarResultado(partida)

        assertEquals(partida, partidasJugadas.guardadas.single().partida)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se registra una partida sin terminar`() = runTest {
        registrarResultado(Partida.nueva(listOf(ana, beto)))
    }

    @Test
    fun `sin rival se consultan los totales generales y con rival el enfrentamiento`() = runTest {
        val equipoAna = Equipo(listOf(ana))
        val equipoBeto = Equipo(listOf(beto))
        repositorio.generales["ana"] = Estadisticas(jugadas = 10, ganadas = 4)
        repositorio.enfrentamientos["ana" to "beto"] = Estadisticas(jugadas = 3, ganadas = 1)

        assertEquals(Estadisticas(10, 4), consultar(equipoAna, rival = null))
        assertEquals(Estadisticas(3, 1), consultar(equipoAna, rival = equipoBeto))
        assertNull(consultar(equipoBeto, rival = equipoAna))
    }
}
