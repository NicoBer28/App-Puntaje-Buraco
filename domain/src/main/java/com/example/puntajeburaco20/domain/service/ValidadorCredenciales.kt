package com.example.puntajeburaco20.domain.service

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import javax.inject.Inject

/** Reglas que deben cumplir el nombre y la contraseña de una cuenta nueva. */
class ValidadorCredenciales @Inject constructor() {

    /** @throws ErrorUsuario con la primera regla que no se cumpla. */
    fun validar(nombre: String, password: String) {
        val campos = listOf(nombre, password)
        when {
            campos.any { it.isEmpty() } -> throw ErrorUsuario.CamposIncompletos
            campos.any { it.length < LONGITUD_MINIMA } ->
                throw ErrorUsuario.LongitudInsuficiente(LONGITUD_MINIMA)
            campos.any { it.length > LONGITUD_MAXIMA } ->
                throw ErrorUsuario.LongitudExcedida(LONGITUD_MAXIMA)
            campos.any { !ALFANUMERICO.matches(it) } -> throw ErrorUsuario.CaracteresInvalidos
        }
    }

    companion object {
        const val LONGITUD_MINIMA = 3
        const val LONGITUD_MAXIMA = 8
        private val ALFANUMERICO = Regex("[a-zA-Z0-9]+")
    }
}
