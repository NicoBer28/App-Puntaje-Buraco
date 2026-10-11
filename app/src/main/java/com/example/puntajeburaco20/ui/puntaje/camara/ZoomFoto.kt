package com.example.puntajeburaco20.ui.puntaje.camara

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Acercamiento de la foto analizada: cuánto se agrandó ([escala]) y cuánto se corrió su esquina
 * superior izquierda ([desplazamiento], en píxeles de pantalla) respecto de la foto sin acercar.
 */
data class ZoomFoto(val escala: Float = 1f, val desplazamiento: Offset = Offset.Zero) {

    /** Rectángulo que ocupa la foto acercada, si sin acercar ocupa [base]. */
    fun area(base: Rect): Rect = Rect(base.topLeft + desplazamiento, base.size * escala)

    /**
     * Aplica un gesto de pellizco o arrastre: [centro] es el punto medio entre los dedos, [pan] lo
     * que se movió ese punto y [zoom] cuánto se separaron los dedos (1 si no cambió). La foto nunca
     * queda más chica que [base] ni deja huecos dentro de él.
     */
    fun transformar(base: Rect, centro: Offset, pan: Offset, zoom: Float): ZoomFoto {
        val nueva = (escala * zoom).coerceIn(1f, ESCALA_MAXIMA)
        // El punto de la foto que estaba bajo los dedos tiene que seguir bajo los dedos.
        val bajoLosDedos = centro - pan - base.topLeft - desplazamiento
        val corrido = centro - base.topLeft - bajoLosDedos * (nueva / escala)
        return ZoomFoto(
            escala = nueva,
            desplazamiento = Offset(
                corrido.x.coerceIn(base.width * (1f - nueva), 0f),
                corrido.y.coerceIn(base.height * (1f - nueva), 0f),
            ),
        )
    }

    companion object {
        const val ESCALA_MAXIMA = 5f
    }
}
