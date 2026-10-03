package com.example.puntajeburaco20.ui.puntaje.camara

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
 * Encapsula CameraX: muestra la vista previa y entrega cada cuadro a [alAnalizar] en un hilo
 * secundario, junto con los grados que hay que girarlo para verlo derecho. Se libera sola cuando
 * se destruye el [lifecycleOwner].
 */
class CamaraFichas(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val vistaPrevia: PreviewView,
    private val alAnalizar: (imagen: Bitmap, rotacion: Int) -> Unit,
) : DefaultLifecycleObserver {

    private val ejecutorAnalisis: ExecutorService = Executors.newSingleThreadExecutor()
    private var proveedor: ProcessCameraProvider? = null
    private var activa = false

    /** Se reutiliza en cada cuadro (solo desde el hilo de análisis) para no crear un bitmap por cuadro. */
    private var cuadroReutilizable: Bitmap? = null

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun iniciar(alFallar: (Throwable) -> Unit) {
        activa = true
        lifecycleOwner.lifecycleScope.launch {
            try {
                val proveedor = ProcessCameraProvider.awaitInstance(context).also { proveedor = it }
                if (!activa) return@launch
                proveedor.unbindAll()
                proveedor.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    crearVistaPrevia(),
                    crearAnalisis(),
                )
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
        proveedor?.unbindAll()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        detener()
        ejecutorAnalisis.shutdown()
    }

    private fun crearVistaPrevia() = Preview.Builder().build().apply {
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
}
