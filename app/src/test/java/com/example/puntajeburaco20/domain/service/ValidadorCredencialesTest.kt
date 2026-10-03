package com.example.puntajeburaco20.domain.service

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidadorCredencialesTest {

    private val validador = ValidadorCredenciales()

    @Test
    fun `acepta nombres y contraseñas alfanumericos de 3 a 8 caracteres`() {
        validador.validar("Ana", "abc")
        validador.validar("Usuario8", "Clave123")
    }

    @Test
    fun `rechaza cada regla con su error, en orden`() {
        assertThrows(ErrorUsuario.CamposIncompletos::class.java) { validador.validar("", "abc") }
        assertThrows(ErrorUsuario.LongitudInsuficiente::class.java) { validador.validar("ab", "abc") }
        assertThrows(ErrorUsuario.LongitudExcedida::class.java) { validador.validar("Ana", "123456789") }
        assertThrows(ErrorUsuario.CaracteresInvalidos::class.java) { validador.validar("Ana B", "abc") }
        assertThrows(ErrorUsuario.CaracteresInvalidos::class.java) { validador.validar("Ana", "ñandú") }
    }
}
