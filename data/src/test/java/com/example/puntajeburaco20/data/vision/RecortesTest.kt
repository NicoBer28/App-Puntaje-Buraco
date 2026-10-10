package com.example.puntajeburaco20.data.vision

import com.example.puntajeburaco20.domain.model.CajaNormalizada
import com.example.puntajeburaco20.domain.model.FichaDetectada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecortesTest {

    private fun ficha(etiqueta: String, confianza: Float, izq: Float, arr: Float, der: Float, aba: Float) =
        FichaDetectada(etiqueta, confianza, CajaNormalizada(izq, arr, der, aba))

    @Test
    fun `los recortes cubren toda la imagen repartidos de forma pareja`() {
        val recortes = Recortes.cubrir(ancho = 3024, alto = 4032, lado = 1512, solape = 0.25f)

        assertEquals(listOf(0, 756, 1512), recortes.map { it.x }.distinct())
        assertEquals(listOf(0, 840, 1680, 2520), recortes.map { it.y }.distinct())
        assertEquals(12, recortes.size)
        assertTrue(recortes.all { it.x + it.lado <= 3024 && it.y + it.lado <= 4032 })
    }

    @Test
    fun `el recorte se elige para que las fichas lleguen al modelo del ancho buscado`() {
        fun lado(vararg anchos: Float) = Recortes.ladoParaFichas(
            anchos.toList(), ancho = 2304, alto = 3072, entradaModelo = 640, anchoObjetivo = 96f, fraccionMinima = 0.33f,
        )

        // Fichas de 120 px: recortes de 800 px, que se reducen a 640 y dejan la ficha en 96 px.
        assertEquals(800, lado(110f, 120f, 130f))
        // Sin fichas vistas (o muy chicas), el recorte más chico permitido: un tercio del lado menor.
        assertEquals(760, lado())
        assertEquals(760, lado(30f))
        // Fichas enormes: no hace falta recortar más que el lado menor.
        assertEquals(2304, lado(900f))
    }

    @Test
    fun `una imagen mas chica que el recorte da un solo recorte del lado menor`() {
        assertEquals(listOf(Recorte(0, 0, 480)), Recortes.cubrir(ancho = 480, alto = 480, lado = 1000, solape = 0.25f))
    }

    @Test
    fun `una caja del recorte se pasa a coordenadas de la imagen`() {
        val caja = Recortes.aImagen(CajaNormalizada(0f, 0f, 0.5f, 1f), Recorte(100, 200, 400), ancho = 1000, alto = 800)

        assertEquals(CajaNormalizada(0.1f, 0.25f, 0.3f, 0.75f), caja)
    }

    @Test
    fun `solo se descartan las fichas cortadas por un borde interior del recorte`() {
        val alBordeIzquierdo = CajaNormalizada(0f, 0.4f, 0.1f, 0.6f)

        // El recorte empieza en el borde de la imagen: la ficha no está cortada.
        assertFalse(Recortes.cortadaPorElBorde(alBordeIzquierdo, Recorte(0, 0, 500), 2000, 1500, 0.01f))
        // El recorte empieza en el medio: la ficha sigue a la izquierda del recorte.
        assertTrue(Recortes.cortadaPorElBorde(alBordeIzquierdo, Recorte(500, 0, 500), 2000, 1500, 0.01f))
    }

    @Test
    fun `fusionar deja la mejor de las que se superponen y descarta las contenidas`() {
        val entera = ficha("7", 0.9f, 0.10f, 0.10f, 0.20f, 0.30f)
        val repetida = ficha("1", 0.6f, 0.11f, 0.10f, 0.21f, 0.30f)
        val mitad = ficha("4", 0.8f, 0.10f, 0.10f, 0.15f, 0.30f)
        val vecina = ficha("8", 0.7f, 0.20f, 0.10f, 0.30f, 0.30f)

        val resultado = Recortes.fusionar(listOf(repetida, mitad, vecina, entera), umbralIou = 0.5f, umbralContencion = 0.75f)

        assertEquals(listOf(entera, vecina), resultado)
    }

    @Test
    fun `sin umbral de contencion una caja contenida chica se conserva`() {
        val grande = ficha("7", 0.9f, 0f, 0f, 1f, 1f)
        val chica = ficha("8", 0.5f, 0.1f, 0.1f, 0.2f, 0.2f)

        assertEquals(listOf(grande, chica), Recortes.fusionar(listOf(chica, grande), umbralIou = 0.7f))
    }
}
