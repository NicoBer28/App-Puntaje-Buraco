package com.example.puntajeburaco20.ui.puntaje.camara

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomFotoTest {

    /** Foto de 900x1200 centrada en un lienzo más ancho: sobran 50 px a cada lado. */
    private val base = Rect(Offset(50f, 0f), Size(900f, 1200f))
    private val centro = Offset(500f, 600f)

    private fun ZoomFoto.pellizcar(zoom: Float, en: Offset = centro) = transformar(base, en, Offset.Zero, zoom)
    private fun ZoomFoto.arrastrar(pan: Offset) = transformar(base, centro, pan, 1f)

    @Test
    fun `la escala queda entre 1x y el maximo`() {
        assertEquals(1f, ZoomFoto().pellizcar(0.2f).escala, 0f)
        assertEquals(ZoomFoto.ESCALA_MAXIMA, ZoomFoto().pellizcar(3f).pellizcar(3f).escala, 0f)
        assertEquals(2f, ZoomFoto().pellizcar(2f).escala, 0f)
    }

    @Test
    fun `sin acercar la foto no se puede mover`() {
        val movida = ZoomFoto().arrastrar(Offset(120f, -80f))

        assertEquals(base, movida.area(base))
    }

    @Test
    fun `al volver a 1x la foto queda en su lugar`() {
        val vuelta = ZoomFoto().pellizcar(3f, en = Offset(100f, 100f)).arrastrar(Offset(-40f, 60f)).pellizcar(0.1f)

        assertEquals(base, vuelta.area(base))
    }

    @Test
    fun `el punto bajo los dedos no se mueve al acercar`() {
        val dedos = Offset(300f, 900f)
        fun puntoDeLaFoto(zoom: ZoomFoto) = zoom.area(base).let { area ->
            Offset((dedos.x - area.left) / area.width, (dedos.y - area.top) / area.height)
        }

        val antes = ZoomFoto().pellizcar(1.5f)
        val despues = antes.pellizcar(2f, en = dedos)

        assertEquals(puntoDeLaFoto(antes).x, puntoDeLaFoto(despues).x, 1e-4f)
        assertEquals(puntoDeLaFoto(antes).y, puntoDeLaFoto(despues).y, 1e-4f)
    }

    @Test
    fun `al arrastrar con la foto acercada se mueve lo mismo que los dedos`() {
        val acercada = ZoomFoto().pellizcar(3f)
        val movida = acercada.arrastrar(Offset(-30f, 45f))

        assertEquals(acercada.area(base).left - 30f, movida.area(base).left, 1e-3f)
        assertEquals(acercada.area(base).top + 45f, movida.area(base).top, 1e-3f)
    }

    @Test
    fun `la foto acercada nunca deja huecos por mas que se la arrastre`() {
        val acercada = ZoomFoto().pellizcar(2.5f)
        val extremos = listOf(Offset(5000f, 5000f), Offset(-5000f, -5000f), Offset(5000f, -5000f), Offset(-5000f, 5000f))

        extremos.forEach { pan ->
            val area = acercada.arrastrar(pan).area(base)
            assertTrue("deja un hueco al arrastrar $pan: $area", area.left <= base.left && area.top <= base.top)
            assertTrue("deja un hueco al arrastrar $pan: $area", area.right >= base.right && area.bottom >= base.bottom)
        }
    }
}
