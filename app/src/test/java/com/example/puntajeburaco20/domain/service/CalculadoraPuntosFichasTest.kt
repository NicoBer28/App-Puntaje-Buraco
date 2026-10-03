package com.example.puntajeburaco20.domain.service

import com.example.puntajeburaco20.domain.model.CajaNormalizada
import com.example.puntajeburaco20.domain.model.FichaDetectada
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculadoraPuntosFichasTest {

    private val calculadora = CalculadoraPuntosFichas()

    private fun ficha(etiqueta: String) = FichaDetectada(etiqueta, 0.9f, CajaNormalizada(0f, 0f, 1f, 1f))

    @Test
    fun `cada ficha vale segun su numero`() {
        assertEquals(5, calculadora.puntosDe(ficha("3")))
        assertEquals(5, calculadora.puntosDe(ficha("7")))
        assertEquals(10, calculadora.puntosDe(ficha("8")))
        assertEquals(10, calculadora.puntosDe(ficha("13")))
        assertEquals(15, calculadora.puntosDe(ficha("1")))
        assertEquals(20, calculadora.puntosDe(ficha("2")))
        assertEquals(50, calculadora.puntosDe(ficha("C")))
        assertEquals(0, calculadora.puntosDe(ficha("desconocida")))
    }

    @Test
    fun `suma todas las fichas`() {
        assertEquals(85, calculadora.sumar(listOf("1", "2", "C").map(::ficha)))
        assertEquals(0, calculadora.sumar(emptyList()))
    }
}
