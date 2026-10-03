package com.example.puntajeburaco20.data.vision

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.example.puntajeburaco20.domain.model.CajaNormalizada
import com.example.puntajeburaco20.domain.model.FichaDetectada
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detector basado en un modelo YOLO exportado a LiteRT (ex TensorFlow Lite). Espera en `assets/`
 * el modelo ([ARCHIVO_MODELO]) y sus etiquetas ([ARCHIVO_ETIQUETAS]), una por línea.
 */
@Singleton
class TfliteDetectorFichas @Inject constructor(
    @ApplicationContext private val context: Context,
) : DetectorFichas {

    /** Modelo cargado junto con los buffers de entrada/salida, que se reutilizan en cada cuadro. */
    private class Modelo(
        val interprete: Interpreter,
        val etiquetas: List<String>,
        val anchoEntrada: Int,
        val altoEntrada: Int,
        val canales: Int,
        val elementos: Int,
    ) {
        val pixeles = IntArray(anchoEntrada * altoEntrada)
        val entrada: ByteBuffer = bufferDeFloats(pixeles.size * CANALES_COLOR)
        val salida: ByteBuffer = bufferDeFloats(canales * elementos)
        val valoresSalida = FloatArray(canales * elementos)
    }

    @Volatile
    private var modelo: Modelo? = null

    @Synchronized
    override fun preparar() {
        if (modelo != null) return

        val interprete = Interpreter(
            cargarModelo(),
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

        cargarEntrada(modelo, imagen)
        modelo.salida.rewind()
        modelo.interprete.run(modelo.entrada, modelo.salida)
        modelo.salida.rewind()
        modelo.salida.asFloatBuffer().get(modelo.valoresSalida)

        return supresionNoMaxima(candidatas(modelo, modelo.valoresSalida))
    }

    /** El modelo se mapea en memoria (sin copiarlo) directamente desde el APK. */
    private fun cargarModelo(): MappedByteBuffer =
        context.assets.openFd(ARCHIVO_MODELO).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { flujo ->
                flujo.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength,
                )
            }
        }

    /** Escala la imagen al tamaño del modelo y la vuelca como RGB normalizado (0 a 1). */
    private fun cargarEntrada(modelo: Modelo, imagen: Bitmap) {
        val escalada = imagen.scale(modelo.anchoEntrada, modelo.altoEntrada, filter = false)
        escalada.getPixels(modelo.pixeles, 0, modelo.anchoEntrada, 0, 0, modelo.anchoEntrada, modelo.altoEntrada)

        modelo.entrada.rewind()
        for (pixel in modelo.pixeles) {
            modelo.entrada.putFloat((pixel shr 16 and 0xFF) / MAXIMO_COLOR)
            modelo.entrada.putFloat((pixel shr 8 and 0xFF) / MAXIMO_COLOR)
            modelo.entrada.putFloat((pixel and 0xFF) / MAXIMO_COLOR)
        }
        modelo.entrada.rewind()
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
        const val CANALES_COLOR = 3
        const val MAXIMO_COLOR = 255f
        const val CANALES_CAJA = 4
        const val UMBRAL_CONFIANZA = 0.3f
        const val UMBRAL_IOU = 0.7f

        fun bufferDeFloats(cantidad: Int): ByteBuffer =
            ByteBuffer.allocateDirect(cantidad * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
    }
}
