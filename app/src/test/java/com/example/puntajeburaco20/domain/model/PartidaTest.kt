package com.example.puntajeburaco20.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PartidaTest {

    private val ana = Jugador("Ana")
    private val beto = Jugador("Beto")
    private val caro = Jugador("Caro")
    private val dani = Jugador("Dani")

    @Test
    fun `una partida de 2 enfrenta a los dos jugadores y empieza el primero`() {
        val partida = Partida.nueva(listOf(ana, beto))

        assertEquals(ModoJuego.INDIVIDUAL, partida.modo)
        assertEquals(Equipo(listOf(ana)), partida.equipoUno)
        assertEquals(Equipo(listOf(beto)), partida.equipoDos)
        assertEquals(ana, partida.empieza)
        assertFalse(partida.terminada)
    }

    @Test
    fun `en una partida de 4 los dos primeros elegidos son compañeros`() {
        val partida = Partida.nueva(listOf(ana, beto, caro, dani))

        assertEquals(ModoJuego.PAREJAS, partida.modo)
        assertEquals(Equipo(listOf(ana, beto)), partida.equipoUno)
        assertEquals(Equipo(listOf(caro, dani)), partida.equipoDos)
    }

    @Test
    fun `solo se puede jugar de a 2 o de a 4 y sin repetir jugadores`() {
        assertThrows(IllegalArgumentException::class.java) { Partida.nueva(listOf(ana)) }
        assertThrows(IllegalArgumentException::class.java) { Partida.nueva(listOf(ana, beto, caro)) }
        assertThrows(IllegalArgumentException::class.java) { Partida.nueva(listOf(ana, Jugador("ANA"))) }
    }

    @Test
    fun `registrar una ronda acumula los totales y guarda la ronda como anterior`() {
        val partida = Partida.nueva(listOf(ana, beto))
            .registrarRonda(PuntajeRonda(base = 100, puntos = 30), PuntajeRonda(base = 0, puntos = -20))
            .registrarRonda(PuntajeRonda(base = 200, puntos = 15), PuntajeRonda(base = 100, puntos = 40))

        assertEquals(345, partida.totalUno)
        assertEquals(120, partida.totalDos)
        assertEquals(PuntajeRonda(200, 15), partida.ultimaRondaUno)
        assertEquals(PuntajeRonda(100, 40), partida.ultimaRondaDos)
    }

    @Test
    fun `en partidas de 2 se alterna quien empieza`() {
        var partida = Partida.nueva(listOf(ana, beto))
        val quienesEmpiezan = (1..3).map {
            partida = partida.registrarRonda(PuntajeRonda.CERO, PuntajeRonda.CERO)
            partida.empieza
        }
        assertEquals(listOf(beto, ana, beto), quienesEmpiezan)
    }

    @Test
    fun `en partidas de 4 empieza cada jugador por turno alternando equipos`() {
        var partida = Partida.nueva(listOf(ana, beto, caro, dani))
        val quienesEmpiezan = (1..4).map {
            partida = partida.registrarRonda(PuntajeRonda.CERO, PuntajeRonda.CERO)
            partida.empieza
        }
        // Mismo orden que la versión anterior: 1 -> 4 -> 2 -> 3 -> 1
        assertEquals(listOf(dani, beto, caro, ana), quienesEmpiezan)
    }

    @Test
    fun `una partida terminada no admite mas rondas ni otro ganador`() {
        val terminada = Partida.nueva(listOf(ana, beto)).finalizar(LadoEquipo.DOS)

        assertTrue(terminada.terminada)
        assertEquals(LadoEquipo.DOS, terminada.ganador)
        assertThrows(IllegalStateException::class.java) {
            terminada.registrarRonda(PuntajeRonda.CERO, PuntajeRonda.CERO)
        }
        assertThrows(IllegalStateException::class.java) { terminada.finalizar(LadoEquipo.UNO) }
    }
}
