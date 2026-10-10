package com.example.puntajeburaco20.ui.common

import com.example.puntajeburaco20.domain.model.ModoJuego
import com.example.puntajeburaco20.fakes.jugador
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeleccionJugadoresTest {

    private val ana = jugador("Ana")
    private val beto = jugador("Beto")
    private val caro = jugador("Caro")
    private val dani = jugador("Dani")
    private val todos = listOf(ana, beto, caro, dani)

    @Test
    fun `un jugador elegido no se ofrece en las demas posiciones`() {
        val seleccion = SeleccionJugadores(todos).elegir(0, ana)

        assertEquals(todos, seleccion.opcionesPara(0))
        assertEquals(listOf(beto, caro, dani), seleccion.opcionesPara(1))
    }

    @Test
    fun `en modo individual solo cuentan las dos primeras posiciones`() {
        val seleccion = SeleccionJugadores(todos).elegir(0, ana).elegir(1, beto)

        assertEquals(listOf(ana, beto), seleccion.visibles)
        assertTrue(seleccion.completa)
    }

    @Test
    fun `en parejas hacen falta las cuatro posiciones`() {
        val seleccion = SeleccionJugadores(todos, ModoJuego.PAREJAS).elegir(0, ana).elegir(1, beto)

        assertFalse(seleccion.completa)
        assertTrue(seleccion.elegir(2, caro).elegir(3, dani).completa)
    }

    @Test
    fun `pasar a modo individual vacia las posiciones ocultas`() {
        val seleccion = SeleccionJugadores(todos, ModoJuego.PAREJAS)
            .elegir(0, ana)
            .elegir(3, dani)
            .conModo(ModoJuego.INDIVIDUAL)

        assertEquals(ana, seleccion.elegido(0))
        assertNull(seleccion.elegido(3))
        assertTrue(dani in seleccion.opcionesPara(1))
    }

    @Test
    fun `si un jugador deja de estar disponible se descarta su eleccion`() {
        val seleccion = SeleccionJugadores(todos).elegir(0, ana).elegir(1, beto)
            .conDisponibles(listOf(ana, caro))

        assertEquals(ana, seleccion.elegido(0))
        assertNull(seleccion.elegido(1))
    }
}
