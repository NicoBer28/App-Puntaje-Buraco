package com.example.puntajeburaco20.domain.model

/** Ficha reconocida por la cámara. */
data class FichaDetectada(
    val etiqueta: String,
    val confianza: Float,
    val caja: CajaNormalizada,
)

/** Rectángulo con coordenadas relativas a la imagen (de 0 a 1). */
data class CajaNormalizada(
    val izquierda: Float,
    val arriba: Float,
    val derecha: Float,
    val abajo: Float,
)
