package com.example.puntajeburaco20.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorialJugadorTest {

    private val ana = Jugador("Ana")
    private val beto = Jugador("Beto")

    /** Partida de Ana contra Beto, con un único puntaje final para cada uno. */
    private fun jugada(fecha: Long, puntosAna: Int, puntosBeto: Int, ganador: LadoEquipo) = PartidaJugada(
        partida = Partida.nueva(listOf(ana, beto))
            .registrarRonda(PuntajeRonda(puntosAna, 0), PuntajeRonda(puntosBeto, 0))
            .finalizar(ganador),
        fecha = fecha,
    )

    @Test
    fun `las partidas se ordenan de la mas reciente a la mas vieja`() {
        val historial = HistorialJugador(
            ana,
            listOf(jugada(1, 0, 0, LadoEquipo.UNO), jugada(3, 0, 0, LadoEquipo.UNO), jugada(2, 0, 0, LadoEquipo.UNO)),
        )

        assertEquals(listOf(3L, 2L, 1L), historial.partidas.map { it.fecha })
    }

    @Test
    fun `la racha cuenta las victorias seguidas hasta la ultima partida`() {
        val partidas = listOf(
            jugada(1, 0, 0, LadoEquipo.UNO),
            jugada(2, 0, 0, LadoEquipo.DOS),
            jugada(3, 0, 0, LadoEquipo.UNO),
            jugada(4, 0, 0, LadoEquipo.UNO),
        )

        val deAna = HistorialJugador(ana, partidas)
        val deBeto = HistorialJugador(Jugador("BETO"), partidas)

        assertEquals(4, deAna.jugadas)
        assertEquals(3, deAna.ganadas)
        assertEquals(2, deAna.rachaActual)
        assertEquals(1, deBeto.ganadas)
        assertEquals(0, deBeto.rachaActual)
    }

    @Test
    fun `el promedio es el del puntaje final del equipo del jugador`() {
        val partidas = listOf(jugada(1, 1000, 500, LadoEquipo.UNO), jugada(2, 2001, 3000, LadoEquipo.DOS))

        assertEquals(1501, HistorialJugador(ana, partidas).promedioPuntos)
        assertEquals(1750, HistorialJugador(beto, partidas).promedioPuntos)
    }

    @Test
    fun `sin partidas no hay promedio ni racha`() {
        val historial = HistorialJugador(ana, emptyList())

        assertNull(historial.promedioPuntos)
        assertEquals(0, historial.rachaActual)
        assertTrue(historial.partidas.isEmpty())
    }

    @Test
    fun `solo se guardan partidas terminadas`() {
        assertThrows(IllegalArgumentException::class.java) {
            PartidaJugada(Partida.nueva(listOf(ana, beto)), fecha = 0)
        }
    }
}
