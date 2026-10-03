package com.example.puntajeburaco20.data.vision

import android.graphics.Bitmap
import com.example.puntajeburaco20.domain.model.FichaDetectada

/** Reconoce fichas de Buraco en una imagen. */
interface DetectorFichas {

    /**
     * Carga el modelo si todavía no está cargado. Es una operación lenta: llamar fuera del hilo
     * principal.
     *
     * @throws java.io.IOException si el modelo no está disponible.
     */
    fun preparar()

    /** Requiere haber llamado a [preparar]. */
    fun detectar(imagen: Bitmap): List<FichaDetectada>
}
