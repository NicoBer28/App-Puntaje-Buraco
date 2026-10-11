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

    /**
     * Detección rápida, para los cuadros de la vista previa. Requiere haber llamado a [preparar].
     * La [imagen] puede venir sin rotar: [rotacion] (en grados, múltiplo de 90) indica cuánto hay
     * que girarla para verla derecha. Las cajas devueltas son relativas a la imagen ya girada.
     */
    fun detectar(imagen: Bitmap, rotacion: Int = 0): List<FichaDetectada>

    /**
     * Detección precisa, para una foto a resolución completa que ya viene derecha. Recorre la
     * imagen por partes, así que tarda varias veces más que [detectar], pero reconoce filas largas
     * de fichas chicas que en una sola pasada se pierden. Requiere haber llamado a [preparar].
     */
    fun detectarFoto(imagen: Bitmap): List<FichaDetectada> = detectar(imagen)
}
