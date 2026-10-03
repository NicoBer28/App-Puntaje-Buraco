package com.example.puntajeburaco20.domain.service

import com.example.puntajeburaco20.domain.model.FichaDetectada
import javax.inject.Inject

/** Valor de cada ficha del Buraco según la etiqueta que le asigna el detector. */
class CalculadoraPuntosFichas @Inject constructor() {

    fun puntosDe(ficha: FichaDetectada): Int = when (ficha.etiqueta) {
        "3", "4", "5", "6", "7" -> 5
        "8", "9", "10", "11", "12", "13" -> 10
        "1" -> 15
        "2" -> 20
        "C" -> 50
        else -> 0
    }

    fun sumar(fichas: List<FichaDetectada>): Int = fichas.sumOf(::puntosDe)
}
