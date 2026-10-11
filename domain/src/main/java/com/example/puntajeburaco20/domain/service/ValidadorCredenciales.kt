package com.example.puntajeburaco20.domain.service

import com.example.puntajeburaco20.domain.error.ErrorUsuario
import javax.inject.Inject

/** Reglas que deben cumplir el nombre de usuario, el mail y la contraseña. */
class ValidadorCredenciales @Inject constructor() {

    /** @throws ErrorUsuario con la primera regla que no se cumpla. */
    fun validarNombre(nombre: String) {
        when {
            nombre.isEmpty() -> throw ErrorUsuario.CamposIncompletos
            nombre.length < LONGITUD_MINIMA_NOMBRE ->
                throw ErrorUsuario.LongitudInsuficiente(LONGITUD_MINIMA_NOMBRE)
            nombre.length > LONGITUD_MAXIMA_NOMBRE ->
                throw ErrorUsuario.LongitudExcedida(LONGITUD_MAXIMA_NOMBRE)
            !ALFANUMERICO.matches(nombre) -> throw ErrorUsuario.CaracteresInvalidos
        }
    }

    /** @throws ErrorUsuario.MailInvalido si no tiene forma de dirección de correo. */
    fun validarMail(mail: String) {
        when {
            mail.isEmpty() -> throw ErrorUsuario.CamposIncompletos
            !MAIL.matches(mail) -> throw ErrorUsuario.MailInvalido
        }
    }

    /** Valida los datos de una cuenta nueva. Si falta alguno, avisa eso antes que el formato. */
    fun validarCuenta(mail: String, password: String) {
        if (mail.isEmpty() || password.isEmpty()) throw ErrorUsuario.CamposIncompletos
        validarMail(mail)
        if (password.length < LONGITUD_MINIMA_PASSWORD) {
            throw ErrorUsuario.ContrasenaCorta(LONGITUD_MINIMA_PASSWORD)
        }
    }

    companion object {
        const val LONGITUD_MINIMA_NOMBRE = 3
        const val LONGITUD_MAXIMA_NOMBRE = 8

        /** El mínimo que acepta el servicio de cuentas. */
        const val LONGITUD_MINIMA_PASSWORD = 6

        private val ALFANUMERICO = Regex("[a-zA-Z0-9]+")

        // Solo descarta errores de tipeo evidentes: que el mail exista lo prueba la verificación.
        // La barra se excluye porque el mail se usa como id de un documento.
        private val MAIL = Regex("[^@\\s/]+@[^@\\s/]+\\.[^@\\s/]+")
    }
}
