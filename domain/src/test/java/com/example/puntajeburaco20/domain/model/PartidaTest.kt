package com.example.puntajeburaco20.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals(Ronda(PuntajeRonda(200, 15), PuntajeRonda(100, 40)), partida.ultimaRonda)
        assertEquals(2, partida.rondas.size)
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

    @Test
    fun `deshacer la ultima ronda resta sus puntos y devuelve el turno de empezar`() {
        val antes = Partida.nueva(listOf(ana, beto, caro, dani))
            .registrarRonda(PuntajeRonda(100, 30), PuntajeRonda(0, -20))
        val despues = antes.registrarRonda(PuntajeRonda(200, 15), PuntajeRonda(100, 40))

        val deshecha = despues.deshacerUltimaRonda()

        assertEquals(antes, deshecha)
        assertEquals(130, deshecha.totalUno)
        assertEquals(dani, deshecha.empieza)
    }

    @Test
    fun `deshacer en la primera ronda vuelve al jugador que empezo la partida`() {
        val nueva = Partida.nueva(listOf(ana, beto))

        val deshecha = nueva.registrarRonda(PuntajeRonda(50, 0), PuntajeRonda.CERO).deshacerUltimaRonda()

        assertEquals(nueva, deshecha)
        assertEquals(Ronda.CERO, deshecha.ultimaRonda)
    }

    @Test
    fun `no se puede deshacer sin rondas ni en una partida terminada`() {
        val nueva = Partida.nueva(listOf(ana, beto))
        val terminada = nueva.registrarRonda(PuntajeRonda.CERO, PuntajeRonda.CERO).finalizar(LadoEquipo.UNO)

        assertFalse(nueva.sePuedeDeshacer)
        assertFalse(terminada.sePuedeDeshacer)
        assertThrows(IllegalStateException::class.java) { nueva.deshacerUltimaRonda() }
        assertThrows(IllegalStateException::class.java) { terminada.deshacerUltimaRonda() }
    }

    @Test
    fun `se sabe en que lado juega cada jugador`() {
        val partida = Partida.nueva(listOf(ana, beto, caro, dani))

        assertEquals(LadoEquipo.UNO, partida.ladoDe(Jugador("BETO")))
        assertEquals(LadoEquipo.DOS, partida.ladoDe(caro))
        assertNull(partida.ladoDe(Jugador("Eva")))
    }
}
