package com.example.puntajeburaco20.data.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
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
 * el modelo ([ARCHIVO_MODELO]) y sus etiquetas ([ARCHIVO_ETIQUETAS]), una por línea y en el mismo
 * orden que las clases del modelo.
 *
 * El modelo y sus buffers se comparten entre [detectar] y [detectarFoto], así que los métodos
 * públicos están sincronizados: el intérprete de LiteRT no se puede usar desde dos hilos a la vez.
 */
@Singleton
class TfliteDetectorFichas @Inject constructor(
    @ApplicationContext private val context: Context,
) : DetectorFichas {

    /**
     * Modelo cargado junto con los buffers de entrada/salida, que se reutilizan en cada cuadro
     * para no generar basura (y pausas del recolector) mientras la cámara está abierta.
     */
    private class Modelo(
        val interprete: Interpreter,
        val etiquetas: List<String>,
        val anchoEntrada: Int,
        val altoEntrada: Int,
        val canales: Int,
        val elementos: Int,
    ) {
        val pixeles = IntArray(anchoEntrada * altoEntrada)
        val imagenEntrada: Bitmap = createBitmap(anchoEntrada, altoEntrada)
        val lienzo = Canvas(imagenEntrada)
        val transformacion = Matrix()
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
            lineas.map { it.trim() }.takeWhile { it.isNotEmpty() }.toList()
        }
        check(etiquetas.size == formaSalida[1] - CANALES_CAJA) {
            "$ARCHIVO_ETIQUETAS tiene ${etiquetas.size} etiquetas y el modelo ${formaSalida[1] - CANALES_CAJA} clases"
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

    @Synchronized
    override fun detectar(imagen: Bitmap, rotacion: Int): List<FichaDetectada> {
        val modelo = modeloPreparado()
        cargarImagenEntera(modelo, imagen, rotacion)
        return Recortes.fusionar(inferir(modelo, UMBRAL_CONFIANZA), UMBRAL_IOU)
    }

    /**
     * Primero pasa la foto entera por el modelo, para ver de qué tamaño son las fichas. Después la
     * recorre en recortes cuadrados superpuestos, del tamaño justo para que cada ficha llegue al
     * modelo con unos [ANCHO_FICHA_OBJETIVO] píxeles, y junta todas las detecciones.
     *
     * Con pocas fichas grandes son 3 o 4 pasadas; con una fila de 50 fichas, unas 25.
     */
    @Synchronized
    override fun detectarFoto(imagen: Bitmap): List<FichaDetectada> {
        val modelo = modeloPreparado()
        val ancho = imagen.width
        val alto = imagen.height

        // Pasada con la foto entera: mide las fichas y ve las grandes, que no entran en un recorte.
        val reducida = achicarAMitad(imagen, vecesAMitad(minOf(ancho, alto), modelo.anchoEntrada))
        cargarImagenEntera(modelo, reducida, rotacion = 0)
        if (reducida !== imagen) reducida.recycle()
        val enteras = inferir(modelo, UMBRAL_CONFIANZA_FOTO)

        val lado = Recortes.ladoParaFichas(
            anchosFichas = enteras.map {
                minOf((it.caja.derecha - it.caja.izquierda) * ancho, (it.caja.abajo - it.caja.arriba) * alto)
            },
            ancho = ancho,
            alto = alto,
            entradaModelo = modelo.anchoEntrada,
            anchoObjetivo = ANCHO_FICHA_OBJETIVO,
            fraccionMinima = FRACCION_RECORTE_MINIMA,
        )

        // Con recortes mucho más grandes que la entrada del modelo (fichas grandes), se recorta de
        // una copia achicada a la mitad, para no saltear píxeles al escalar.
        val veces = vecesAMitad(lado, modelo.anchoEntrada)
        val fuente = achicarAMitad(imagen, veces)
        val candidatas = enteras.toMutableList()
        for (recorte in Recortes.cubrir(ancho, alto, lado, SOLAPE_RECORTES)) {
            val enFuente = Recorte(recorte.x shr veces, recorte.y shr veces, recorte.lado shr veces)
            cargarRecorte(modelo, fuente, enFuente)
            inferir(modelo, UMBRAL_CONFIANZA_FOTO)
                .filterNot { Recortes.cortadaPorElBorde(it.caja, recorte, ancho, alto, MARGEN_BORDE) }
                .mapTo(candidatas) { it.copy(caja = Recortes.aImagen(it.caja, recorte, ancho, alto)) }
        }
        if (fuente !== imagen) fuente.recycle()
        return Recortes.fusionar(candidatas, UMBRAL_IOU_FOTO, UMBRAL_CONTENCION_FOTO)
    }

    private fun modeloPreparado(): Modelo =
        checkNotNull(modelo) { "Hay que llamar a preparar() antes de detectar" }

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

    /**
     * Gira y escala la imagen entera al tamaño del modelo en un solo paso, sin conservar la
     * proporción (así se entrenó el modelo: medido con fotos reales, estirar anduvo mejor que
     * rellenar los costados).
     */
    private fun cargarImagenEntera(modelo: Modelo, imagen: Bitmap, rotacion: Int) {
        val girada = rotacion % 180 != 0
        val anchoGirada = if (girada) imagen.height else imagen.width
        val altoGirada = if (girada) imagen.width else imagen.height
        modelo.transformacion.apply {
            setTranslate(-imagen.width / 2f, -imagen.height / 2f)
            postRotate(rotacion.toFloat())
            postScale(modelo.anchoEntrada.toFloat() / anchoGirada, modelo.altoEntrada.toFloat() / altoGirada)
            postTranslate(modelo.anchoEntrada / 2f, modelo.altoEntrada / 2f)
        }
        volcarEntrada(modelo, imagen)
    }

    /** Escala el [recorte] (cuadrado) de la imagen al tamaño del modelo. */
    private fun cargarRecorte(modelo: Modelo, imagen: Bitmap, recorte: Recorte) {
        modelo.transformacion.apply {
            setTranslate(-recorte.x.toFloat(), -recorte.y.toFloat())
            postScale(modelo.anchoEntrada.toFloat() / recorte.lado, modelo.altoEntrada.toFloat() / recorte.lado)
        }
        volcarEntrada(modelo, imagen)
    }

    /** Dibuja la imagen con la transformación actual y la vuelca como RGB normalizado (0 a 1). */
    private fun volcarEntrada(modelo: Modelo, imagen: Bitmap) {
        modelo.lienzo.drawColor(Color.rgb(GRIS_RELLENO, GRIS_RELLENO, GRIS_RELLENO))
        modelo.lienzo.drawBitmap(imagen, modelo.transformacion, PINTURA_ESCALADO)
        modelo.imagenEntrada.getPixels(
            modelo.pixeles, 0, modelo.anchoEntrada, 0, 0, modelo.anchoEntrada, modelo.altoEntrada,
        )

        modelo.entrada.rewind()
        for (pixel in modelo.pixeles) {
            modelo.entrada.putFloat((pixel shr 16 and 0xFF) / MAXIMO_COLOR)
            modelo.entrada.putFloat((pixel shr 8 and 0xFF) / MAXIMO_COLOR)
            modelo.entrada.putFloat((pixel and 0xFF) / MAXIMO_COLOR)
        }
        modelo.entrada.rewind()
    }

    private fun inferir(modelo: Modelo, umbral: Float): List<FichaDetectada> {
        modelo.salida.rewind()
        modelo.interprete.run(modelo.entrada, modelo.salida)
        modelo.salida.rewind()
        modelo.salida.asFloatBuffer().get(modelo.valoresSalida)
        return candidatas(modelo, modelo.valoresSalida, umbral)
    }

    /**
     * La salida de YOLO es una matriz [canales x elementos]: por cada elemento (caja candidata)
     * los 4 primeros canales son cx, cy, ancho y alto (relativos a la entrada, de 0 a 1) y el
     * resto la confianza de cada clase.
     */
    private fun candidatas(modelo: Modelo, salida: FloatArray, umbral: Float): List<FichaDetectada> {
        val n = modelo.elementos
        val resultado = mutableListOf<FichaDetectada>()

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
            if (maxConfianza <= umbral) continue

            val cx = salida[c]
            val cy = salida[c + n]
            val ancho = salida[c + n * 2]
            val alto = salida[c + n * 3]
            // Las cajas que se salen un poco de la imagen se recortan (antes se descartaban, y
            // con eso se perdían las fichas pegadas al borde).
            val caja = CajaNormalizada(
                izquierda = (cx - ancho / 2f).coerceIn(0f, 1f),
                arriba = (cy - alto / 2f).coerceIn(0f, 1f),
                derecha = (cx + ancho / 2f).coerceIn(0f, 1f),
                abajo = (cy + alto / 2f).coerceIn(0f, 1f),
            )
            if (caja.derecha <= caja.izquierda || caja.abajo <= caja.arriba) continue

            resultado += FichaDetectada(modelo.etiquetas[maxClase], maxConfianza, caja)
        }
        return resultado
    }

    /** Cuántas veces se puede achicar a la mitad [largo] sin quedar por debajo de [minimo]. */
    private fun vecesAMitad(largo: Int, minimo: Int): Int {
        var veces = 0
        while (largo shr (veces + 1) >= minimo) veces++
        return veces
    }

    /**
     * Achica la imagen a la mitad [veces] veces. Escalar de golpe de 3000 a 640 píxeles con filtro
     * bilineal saltea píxeles y los números finos se deforman; de a mitades, cada paso promedia
     * bien los píxeles.
     */
    private fun achicarAMitad(imagen: Bitmap, veces: Int): Bitmap {
        var actual = imagen
        repeat(veces) {
            val siguiente = actual.scale(maxOf(1, actual.width / 2), maxOf(1, actual.height / 2), filter = true)
            if (actual !== imagen) actual.recycle()
            actual = siguiente
        }
        return actual
    }

    private companion object {
        const val ARCHIVO_MODELO = "best_float32.tflite"
        const val ARCHIVO_ETIQUETAS = "labels.txt"
        const val HILOS = 4
        const val CANALES_COLOR = 3
        const val MAXIMO_COLOR = 255f
        const val CANALES_CAJA = 4
        const val GRIS_RELLENO = 114

        // Vista previa en vivo.
        const val UMBRAL_CONFIANZA = 0.3f
        const val UMBRAL_IOU = 0.7f

        // Foto: con muchas pasadas aparecen más falsos positivos de baja confianza (fichas boca
        // abajo, fichas cortadas), por eso el umbral es más alto. Valores medidos con fotos reales
        // y sintéticas (ver docs/DETECCION_FICHAS.md).
        const val ANCHO_FICHA_OBJETIVO = 96f
        const val FRACCION_RECORTE_MINIMA = 0.33f
        const val SOLAPE_RECORTES = 0.25f
        const val MARGEN_BORDE = 0.01f
        const val UMBRAL_CONFIANZA_FOTO = 0.5f
        const val UMBRAL_IOU_FOTO = 0.5f
        const val UMBRAL_CONTENCION_FOTO = 0.75f

        val PINTURA_ESCALADO = Paint(Paint.FILTER_BITMAP_FLAG)

        fun bufferDeFloats(cantidad: Int): ByteBuffer =
            ByteBuffer.allocateDirect(cantidad * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
    }
}
