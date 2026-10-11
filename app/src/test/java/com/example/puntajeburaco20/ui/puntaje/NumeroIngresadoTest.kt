package com.example.puntajeburaco20.ui.puntaje

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumeroIngresadoTest {

    @Test
    fun `acepta numeros positivos, negativos y a medio escribir`() {
        assertEquals("150", numeroIngresado("150"))
        assertEquals("-20", numeroIngresado("-20"))
        assertEquals("-", numeroIngresado("-"))
        assertEquals("", numeroIngresado(""))
    }

    @Test
    fun `el menos se puede tocar despues de escribir el numero, y tocarlo otra vez lo saca`() {
        assertEquals("-50", numeroIngresado("50-"))
        assertEquals("-50", numeroIngresado("5-0"))
        assertEquals("50", numeroIngresado("-50-"))
    }

    @Test
    fun `ignora lo que no es una cifra`() {
        assertEquals("120", numeroIngresado("1.2,0 "))
    }

    @Test
    fun `no deja pasar de seis cifras`() {
        assertEquals("-123456", numeroIngresado("-123456"))
        assertNull(numeroIngresado("1234567"))
    }
}
