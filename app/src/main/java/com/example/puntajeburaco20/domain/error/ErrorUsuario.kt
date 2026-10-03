package com.example.puntajeburaco20.domain.error

/** Reglas de negocio de usuarios y amistades que una operación puede violar. */
sealed class ErrorUsuario : Exception() {
    data object CamposIncompletos : ErrorUsuario()
    data class LongitudInsuficiente(val minimo: Int) : ErrorUsuario()
    data class LongitudExcedida(val maximo: Int) : ErrorUsuario()
    data object CaracteresInvalidos : ErrorUsuario()
    data object NombreEnUso : ErrorUsuario()
    data object UsuarioInexistente : ErrorUsuario()
    data object ContrasenaIncorrecta : ErrorUsuario()
    data object SinSesion : ErrorUsuario()
    data object EsElUsuarioActual : ErrorUsuario()
    data object YaEsAmigo : ErrorUsuario()
    data object NoEsAmigo : ErrorUsuario()
}
