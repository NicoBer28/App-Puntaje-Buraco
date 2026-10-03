package com.example.puntajeburaco20.domain.usecase

import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.fakes.FakeEstadisticasRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EstadisticasUseCasesTest {

    private val repositorio = FakeEstadisticasRepository()
    private val ana = Jugador("Ana")
    private val beto = Jugador("Beto")

    @Test
    fun `registrar el resultado pasa ganador y perdedor segun el lado elegido`() = runTest {
        val partida = Partida.nueva(listOf(ana, beto)).finalizar(LadoEquipo.DOS)

        RegistrarResultadoPartidaUseCase(repositorio)(partida)

        assertEquals(listOf(Equipo(listOf(beto)) to Equipo(listOf(ana))), repositorio.resultados)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se registra una partida sin terminar`() = runTest {
        RegistrarResultadoPartidaUseCase(repositorio)(Partida.nueva(listOf(ana, beto)))
    }

    @Test
    fun `sin rival se consultan los totales generales y con rival el enfrentamiento`() = runTest {
        val equipoAna = Equipo(listOf(ana))
        val equipoBeto = Equipo(listOf(beto))
        repositorio.generales["ana"] = Estadisticas(jugadas = 10, ganadas = 4)
        repositorio.enfrentamientos["ana" to "beto"] = Estadisticas(jugadas = 3, ganadas = 1)
        val consultar = ConsultarEstadisticasUseCase(repositorio)

        assertEquals(Estadisticas(10, 4), consultar(equipoAna, rival = null))
        assertEquals(Estadisticas(3, 1), consultar(equipoAna, rival = equipoBeto))
        assertNull(consultar(equipoBeto, rival = equipoAna))
    }
}
