package com.example.puntajeburaco20.ui.puntaje.camara

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException

/**
 * Encapsula CameraX: muestra la vista previa, entrega cada cuadro a [alAnalizar] en un hilo
 * secundario (junto con los grados que hay que girarlo para verlo derecho) y saca fotos a
 * resolución completa con [sacarFoto]. Se libera sola cuando se destruye el [lifecycleOwner].
 */
class CamaraFichas(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val alAnalizar: (imagen: Bitmap, rotacion: Int) -> Unit,
) : DefaultLifecycleObserver {

    private val ejecutorAnalisis: ExecutorService = Executors.newSingleThreadExecutor()
    private var proveedor: ProcessCameraProvider? = null
    private var captura: ImageCapture? = null
    private var activa = false

    /** Se reutiliza en cada cuadro (solo desde el hilo de análisis) para no crear un bitmap por cuadro. */
    private var cuadroReutilizable: Bitmap? = null

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /** La [vistaPrevia] se recibe acá porque solo existe mientras la cámara está abierta. */
    fun iniciar(vistaPrevia: PreviewView, alFallar: (Throwable) -> Unit) {
        activa = true
        lifecycleOwner.lifecycleScope.launch {
            try {
                val proveedor = ProcessCameraProvider.awaitInstance(context).also { proveedor = it }
                if (!activa) return@launch
                proveedor.unbindAll()
                val captura = crearCaptura()
                proveedor.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    crearVistaPrevia(vistaPrevia),
                    crearAnalisis(),
                    captura,
                )
                this@CamaraFichas.captura = captura
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                activa = false
                alFallar(e)
            }
        }
    }

    fun detener() {
        activa = false
        captura = null
        proveedor?.unbindAll()
    }

    /**
     * Saca una foto a la máxima resolución del sensor y la entrega derecha y reducida a
     * [LADO_MAXIMO_FOTO] píxeles en [alTerminar], desde un hilo secundario.
     */
    fun sacarFoto(alTerminar: (Bitmap) -> Unit, alFallar: (Throwable) -> Unit) {
        val captura = captura ?: return alFallar(IllegalStateException("La cámara todavía no está lista"))
        captura.takePicture(
            ejecutorAnalisis,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imagen: ImageProxy) {
                    val foto = try {
                        imagen.use { enderezar(it.toBitmap(), it.imageInfo.rotationDegrees) }
                    } catch (e: Exception) {
                        alFallar(e)
                        return
                    }
                    alTerminar(foto)
                }

                override fun onError(error: ImageCaptureException) = alFallar(error)
            },
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        detener()
        ejecutorAnalisis.shutdown()
    }

    private fun crearVistaPrevia(vistaPrevia: PreviewView) = Preview.Builder().build().apply {
        setSurfaceProvider(vistaPrevia.surfaceProvider)
    }

    private fun crearAnalisis() = ImageAnalysis.Builder()
        // Si el modelo es más lento que la cámara, se descartan los cuadros viejos.
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .build()
        .apply {
            setAnalyzer(ejecutorAnalisis) { cuadro ->
                cuadro.use { alAnalizar(aBitmap(it), it.imageInfo.rotationDegrees) }
            }
        }

    // Por defecto ImageCapture usa la mayor resolución 4:3 del sensor, igual que la vista previa.
    private fun crearCaptura() = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    /**
     * Copia los píxeles del cuadro a un bitmap reutilizado. Si las filas traen relleno (algunos
     * dispositivos alinean la memoria), se usa la conversión de CameraX, que crea un bitmap nuevo.
     */
    private fun aBitmap(cuadro: ImageProxy): Bitmap {
        val plano = cuadro.planes[0]
        if (plano.rowStride != cuadro.width * plano.pixelStride) return cuadro.toBitmap()

        val reutilizable = cuadroReutilizable
            ?.takeIf { it.width == cuadro.width && it.height == cuadro.height }
            ?: createBitmap(cuadro.width, cuadro.height).also { cuadroReutilizable = it }
        plano.buffer.rewind()
        reutilizable.copyPixelsFromBuffer(plano.buffer)
        return reutilizable
    }

    /** Gira la foto para verla derecha y la achica si supera [LADO_MAXIMO_FOTO], en un solo paso. */
    private fun enderezar(foto: Bitmap, rotacion: Int): Bitmap {
        val escala = minOf(1f, LADO_MAXIMO_FOTO.toFloat() / maxOf(foto.width, foto.height))
        if (rotacion % 360 == 0 && escala == 1f) return foto
        val matriz = Matrix().apply {
            postRotate(rotacion.toFloat())
            postScale(escala, escala)
        }
        val derecha = Bitmap.createBitmap(foto, 0, 0, foto.width, foto.height, matriz, true)
        if (derecha !== foto) foto.recycle()
        return derecha
    }

    private companion object {
        /**
         * Más resolución no mejora la detección (cada recorte se reduce a 640 píxeles igual) y
         * una foto de 12 MP ocupa 48 MB en memoria.
         */
        const val LADO_MAXIMO_FOTO = 3072
    }
}
