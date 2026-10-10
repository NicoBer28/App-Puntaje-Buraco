package com.example.puntajeburaco20.domain.service

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidadorCredencialesTest {

    private val validador = ValidadorCredenciales()

    @Test
    fun `acepta nombres alfanumericos de 3 a 8 caracteres`() {
        validador.validarNombre("Ana")
        validador.validarNombre("Usuario8")
    }

    @Test
    fun `rechaza cada regla del nombre con su error, en orden`() {
        assertThrows(ErrorUsuario.CamposIncompletos::class.java) { validador.validarNombre("") }
        assertThrows(ErrorUsuario.LongitudInsuficiente::class.java) { validador.validarNombre("ab") }
        assertThrows(ErrorUsuario.LongitudExcedida::class.java) { validador.validarNombre("123456789") }
        assertThrows(ErrorUsuario.CaracteresInvalidos::class.java) { validador.validarNombre("Ana B") }
        assertThrows(ErrorUsuario.CaracteresInvalidos::class.java) { validador.validarNombre("ñandú") }
    }

    @Test
    fun `acepta mails con forma de direccion de correo`() {
        validador.validarMail("ana@test.com")
        validador.validarMail("ana.perez+buraco@mail.com.ar")
    }

    @Test
    fun `rechaza mails vacios o mal formados`() {
        assertThrows(ErrorUsuario.CamposIncompletos::class.java) { validador.validarMail("") }
        listOf("ana", "ana@", "@test.com", "ana@test", "ana perez@test.com", "ana@@test.com").forEach { mail ->
            assertThrows(mail, ErrorUsuario.MailInvalido::class.java) { validador.validarMail(mail) }
        }
    }

    @Test
    fun `una cuenta nueva necesita mail valido y contraseña de 6 caracteres o mas`() {
        validador.validarCuenta("ana@test.com", "123456")
        validador.validarCuenta("ana@test.com", "una clave larga, con ñ y símbolos!")

        assertThrows(ErrorUsuario.MailInvalido::class.java) { validador.validarCuenta("ana", "123456") }
        assertThrows(ErrorUsuario.ContrasenaCorta::class.java) { validador.validarCuenta("ana@test.com", "12345") }
    }

    @Test
    fun `si falta un dato de la cuenta se avisa eso antes que el formato`() {
        assertThrows(ErrorUsuario.CamposIncompletos::class.java) { validador.validarCuenta("ana", "") }
        assertThrows(ErrorUsuario.CamposIncompletos::class.java) { validador.validarCuenta("", "12345") }
    }
}
