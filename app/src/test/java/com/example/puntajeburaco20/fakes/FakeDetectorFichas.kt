package com.example.puntajeburaco20.fakes

import android.graphics.Bitmap
import com.example.puntajeburaco20.data.vision.DetectorFichas
import com.example.puntajeburaco20.domain.model.FichaDetectada

class FakeDetectorFichas(var disponible: Boolean = true) : DetectorFichas {
    var fichas: List<FichaDetectada> = emptyList()

    override fun preparar() {
        if (!disponible) throw java.io.IOException("Modelo no encontrado")
    }

    override fun detectar(imagen: Bitmap, rotacion: Int): List<FichaDetectada> = fichas
}
