package com.example.puntajeburaco20.ui.recorrido

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pantalla de 360 x 800 con 16 de margen a los lados y las barras del sistema arriba y abajo. */
class UbicacionGloboTest {

    private val area = Rect(left = 16f, top = 40f, right = 344f, bottom = 760f)
    private val globo = Size(width = 300f, height = 150f)

    private fun ubicar(hueco: Rect, globo: Size = this.globo) = ubicarGlobo(area, hueco, globo, separacion = 12f)

    @Test
    fun `va debajo del elemento y centrado sobre el`() {
        val lugar = ubicar(Rect(left = 130f, top = 100f, right = 230f, bottom = 150f))

        assertEquals(Offset(x = 30f, y = 162f), lugar)
    }

    @Test
    fun `si abajo no entra va arriba`() {
        val lugar = ubicar(Rect(left = 16f, top = 700f, right = 344f, bottom = 750f))

        // 150 de alto más 12 de separación por encima del elemento.
        assertEquals(538f, lugar.y)
    }

    @Test
    fun `no se sale por los costados aunque el elemento este contra un borde`() {
        assertEquals(44f, ubicar(Rect(left = 300f, top = 100f, right = 340f, bottom = 150f)).x)
        assertEquals(16f, ubicar(Rect(left = 20f, top = 100f, right = 60f, bottom = 150f)).x)
    }

    @Test
    fun `si no entra ni arriba ni abajo va del lado con mas lugar, sin salirse de la pantalla`() {
        // Arriba quedan 160 y abajo 60: ninguno alcanza para los 162 que necesita.
        assertEquals(40f, ubicar(Rect(left = 16f, top = 200f, right = 344f, bottom = 700f)).y)
        // Al revés: abajo quedan 160 y arriba 60.
        assertEquals(610f, ubicar(Rect(left = 16f, top = 100f, right = 344f, bottom = 600f)).y)
    }

    @Test
    fun `un globo mas ancho que la pantalla arranca en el borde izquierdo`() {
        val lugar = ubicar(Rect(left = 130f, top = 100f, right = 230f, bottom = 150f), Size(width = 400f, height = 150f))

        assertEquals(16f, lugar.x)
    }
}
