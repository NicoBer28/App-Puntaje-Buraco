package com.example.puntajeburaco20 // Asegurate que sea tu package

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.LinkedList

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = LinkedList<YOLODetector.BoundingBox>()
    private val boxPaint = Paint()
    private val textPaint = Paint()
    private val textBackgroundPaint = Paint()
    private var bounds = RectF()

    init {
        // Configuración del borde del cuadro
        boxPaint.color = Color.CYAN
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 8f
        boxPaint.strokeCap = Paint.Cap.ROUND
        boxPaint.strokeJoin = Paint.Join.ROUND

        // Configuración del texto (Nombre de la ficha)
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f
        textPaint.isAntiAlias = true

        // Fondo negro para el texto (para que se lea bien)
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.alpha = 160
    }

    // Esta función la vas a llamar desde el Fragment cuando tengas resultados
    fun setResults(boundingBoxes: List<YOLODetector.BoundingBox>) {
        results.clear()
        results.addAll(boundingBoxes)
        invalidate() // Fuerza a redibujar la vista
    }

    @SuppressLint("DefaultLocale", "DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (result in results) {
            // YOLO devuelve coordenadas normalizadas (0 a 1).
            // Las multiplicamos por el ancho/alto de ESTA vista.
            val left = result.x1 * width
            val top = result.y1 * height
            val right = result.x2 * width
            val bottom = result.y2 * height

            // Dibujamos el rectangulo
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Dibujamos el texto
            //val drawableText = "${result.clsName} (${String.format("%.2f", result.cnf)})"
            val drawableText = result.clsName

            // Calculamos fondo del texto
            textPaint.getTextBounds(drawableText, 0, drawableText.length, android.graphics.Rect())
            val textWidth = textPaint.measureText(drawableText)
            val textHeight = textPaint.textSize

            canvas.drawRect(
                left,
                top - textHeight - 10,
                left + textWidth + 20,
                top,
                textBackgroundPaint
            )

            canvas.drawText(drawableText, left + 10, top - 10, textPaint)
        }
    }
}