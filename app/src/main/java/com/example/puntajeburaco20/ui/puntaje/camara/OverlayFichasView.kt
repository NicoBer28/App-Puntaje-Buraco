package com.example.puntajeburaco20.ui.puntaje.camara

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.puntajeburaco20.domain.model.FichaDetectada

/** Dibuja sobre la vista previa de la cámara un recuadro con la etiqueta de cada ficha. */
class OverlayFichasView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var fichas: List<FichaDetectada> = emptyList()

    private val pinturaCaja = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val pinturaTexto = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 50f
        isAntiAlias = true
    }

    private val pinturaFondoTexto = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        alpha = 160
    }

    fun mostrar(fichas: List<FichaDetectada>) {
        this.fichas = fichas
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (ficha in fichas) {
            // Las coordenadas vienen normalizadas (0 a 1): se escalan al tamaño de esta vista.
            val izquierda = ficha.caja.izquierda * width
            val arriba = ficha.caja.arriba * height
            canvas.drawRect(
                izquierda,
                arriba,
                ficha.caja.derecha * width,
                ficha.caja.abajo * height,
                pinturaCaja,
            )

            val texto = ficha.etiqueta
            canvas.drawRect(
                izquierda,
                arriba - pinturaTexto.textSize - MARGEN_TEXTO,
                izquierda + pinturaTexto.measureText(texto) + 2 * MARGEN_TEXTO,
                arriba,
                pinturaFondoTexto,
            )
            canvas.drawText(texto, izquierda + MARGEN_TEXTO, arriba - MARGEN_TEXTO, pinturaTexto)
        }
    }

    private companion object {
        const val MARGEN_TEXTO = 10f
    }
}
