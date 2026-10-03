package com.example.puntajeburaco20.data.vision

import android.content.Context
import android.graphics.Bitmap
import com.example.puntajeburaco20.domain.model.CajaNormalizada
import com.example.puntajeburaco20.domain.model.FichaDetectada
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detector basado en un modelo YOLO exportado a TensorFlow Lite. Espera en `assets/` el modelo
 * ([ARCHIVO_MODELO]) y sus etiquetas ([ARCHIVO_ETIQUETAS]), una por línea.
 */
@Singleton
class TfliteDetectorFichas @Inject constructor(
    @ApplicationContext private val context: Context,
) : DetectorFichas {

    private class Modelo(
        val interprete: Interpreter,
        val etiquetas: List<String>,
        val anchoEntrada: Int,
        val altoEntrada: Int,
        val canales: Int,
        val elementos: Int,
    )

    @Volatile
    private var modelo: Modelo? = null

    private val procesador = ImageProcessor.Builder()
        .add(NormalizeOp(MEDIA_ENTRADA, DESVIO_ENTRADA))
        .add(CastOp(DataType.FLOAT32))
        .build()

    @Synchronized
    override fun preparar() {
        if (modelo != null) return

        val interprete = Interpreter(
            FileUtil.loadMappedFile(context, ARCHIVO_MODELO),
            Interpreter.Options().apply { numThreads = HILOS },
        )
        val formaEntrada = interprete.getInputTensor(0).shape() // [1, alto, ancho, 3]
        val formaSalida = interprete.getOutputTensor(0).shape() // [1, canales, elementos]
        val etiquetas = context.assets.open(ARCHIVO_ETIQUETAS).bufferedReader().useLines { lineas ->
            lineas.takeWhile { it.isNotEmpty() }.toList()
        }

        modelo = Modelo(
            interprete = interprete,
            etiquetas = etiquetas,
            anchoEntrada = formaEntrada[2],
            altoEntrada = formaEntrada[1],
            canales = formaSalida[1],
            elementos = formaSalida[2],
        )
    }

    override fun detectar(imagen: Bitmap): List<FichaDetectada> {
        val modelo = checkNotNull(modelo) { "Hay que llamar a preparar() antes de detectar" }

        val escalada = Bitmap.createScaledBitmap(imagen, modelo.anchoEntrada, modelo.altoEntrada, false)
        val entrada = procesador.process(TensorImage(DataType.FLOAT32).apply { load(escalada) })
        val salida = TensorBuffer.createFixedSize(
            intArrayOf(1, modelo.canales, modelo.elementos),
            DataType.FLOAT32,
        )
        modelo.interprete.run(entrada.buffer, salida.buffer)

        return supresionNoMaxima(candidatas(modelo, salida.floatArray))
    }

    /**
     * La salida de YOLO es una matriz [canales x elementos]: por cada elemento (caja candidata)
     * los 4 primeros canales son cx, cy, ancho y alto, y el resto la confianza de cada clase.
     */
    private fun candidatas(modelo: Modelo, salida: FloatArray): List<Candidata> {
        val n = modelo.elementos
        val resultado = mutableListOf<Candidata>()

        for (c in 0 until n) {
            var maxConfianza = -1f
            var maxClase = -1
            for (canal in CANALES_CAJA until modelo.canales) {
                val confianza = salida[c + n * canal]
                if (confianza > maxConfianza) {
                    maxConfianza = confianza
                    maxClase = canal - CANALES_CAJA
                }
            }
            if (maxConfianza <= UMBRAL_CONFIANZA) continue

            val cx = salida[c]
            val cy = salida[c + n]
            val ancho = salida[c + n * 2]
            val alto = salida[c + n * 3]
            val caja = CajaNormalizada(
                izquierda = cx - ancho / 2f,
                arriba = cy - alto / 2f,
                derecha = cx + ancho / 2f,
                abajo = cy + alto / 2f,
            )
            if (!caja.estaDentroDeLaImagen()) continue

            val ficha = FichaDetectada(modelo.etiquetas[maxClase], maxConfianza, caja)
            resultado += Candidata(ficha, area = ancho * alto)
        }
        return resultado
    }

    /** Descarta las cajas que se superponen demasiado con otra de mayor confianza. */
    private fun supresionNoMaxima(candidatas: List<Candidata>): List<FichaDetectada> {
        val pendientes = candidatas.sortedByDescending { it.ficha.confianza }.toMutableList()
        val elegidas = mutableListOf<FichaDetectada>()

        while (pendientes.isNotEmpty()) {
            val mejor = pendientes.removeAt(0)
            elegidas += mejor.ficha
            pendientes.removeAll { interseccionSobreUnion(mejor, it) >= UMBRAL_IOU }
        }
        return elegidas
    }

    private fun interseccionSobreUnion(a: Candidata, b: Candidata): Float {
        val izquierda = maxOf(a.ficha.caja.izquierda, b.ficha.caja.izquierda)
        val arriba = maxOf(a.ficha.caja.arriba, b.ficha.caja.arriba)
        val derecha = minOf(a.ficha.caja.derecha, b.ficha.caja.derecha)
        val abajo = minOf(a.ficha.caja.abajo, b.ficha.caja.abajo)
        val interseccion = maxOf(0f, derecha - izquierda) * maxOf(0f, abajo - arriba)
        return interseccion / (a.area + b.area - interseccion)
    }

    private fun CajaNormalizada.estaDentroDeLaImagen(): Boolean =
        listOf(izquierda, arriba, derecha, abajo).all { it in 0f..1f }

    private class Candidata(val ficha: FichaDetectada, val area: Float)

    private companion object {
        const val ARCHIVO_MODELO = "best_float32.tflite"
        const val ARCHIVO_ETIQUETAS = "labels.txt"
        const val HILOS = 4
        const val MEDIA_ENTRADA = 0f
        const val DESVIO_ENTRADA = 255f
        const val CANALES_CAJA = 4
        const val UMBRAL_CONFIANZA = 0.3f
        const val UMBRAL_IOU = 0.7f
    }
}
