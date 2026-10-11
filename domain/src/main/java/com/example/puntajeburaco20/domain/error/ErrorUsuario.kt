package com.example.puntajeburaco20.domain.error

/** Reglas de negocio de cuentas, perfiles y amistades que una operación puede violar. */
sealed class ErrorUsuario : Exception() {
    data object CamposIncompletos : ErrorUsuario()

    // Nombre de usuario
    data class LongitudInsuficiente(val minimo: Int) : ErrorUsuario()
    data class LongitudExcedida(val maximo: Int) : ErrorUsuario()
    data object CaracteresInvalidos : ErrorUsuario()
    data object NombreEnUso : ErrorUsuario()
    data object UsuarioInexistente : ErrorUsuario()

    // Cuenta
    data object MailInvalido : ErrorUsuario()
    data object MailEnUso : ErrorUsuario()
    data object MailSinVerificar : ErrorUsuario()
    data class ContrasenaCorta(val minimo: Int) : ErrorUsuario()

    /** El mail no tiene cuenta o la contraseña no coincide: no se distingue cuál de las dos. */
    data object CredencialesIncorrectas : ErrorUsuario()

    /** La contraseña con la que la cuenta confirma una operación delicada no es la suya. */
    data object ContrasenaIncorrecta : ErrorUsuario()
    data object DemasiadosIntentos : ErrorUsuario()
    data object SinConexion : ErrorUsuario()
    data object SinSesion : ErrorUsuario()

    // Perfil anterior al cambio a cuentas con mail
    data object PerfilYaVinculado : ErrorUsuario()
    data object PerfilCreadoPorOtro : ErrorUsuario()
    data object ContrasenaAnteriorIncorrecta : ErrorUsuario()

    // Perfil que otra persona creó y dejó reservado para un mail
    /** Ese mail ya tiene una cuenta, o ya hay un perfil reservado para él. */
    data object MailConPerfil : ErrorUsuario()

    /** La cuenta tiene un perfil reservado: antes de hacer otra cosa tiene que aceptarlo o rechazarlo. */
    data object ReclamoPendiente : ErrorUsuario()
    data object ReclamoNoDisponible : ErrorUsuario()

    // Amistades
    data object EsElUsuarioActual : ErrorUsuario()
    data object YaEsAmigo : ErrorUsuario()
    data object NoEsAmigo : ErrorUsuario()
}
