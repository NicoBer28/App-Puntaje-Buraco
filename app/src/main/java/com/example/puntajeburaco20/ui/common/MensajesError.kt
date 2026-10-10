package com.example.puntajeburaco20.ui.common

import android.util.Log
import androidx.annotation.StringRes
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario

/**
 * Traduce un error a un mensaje para el usuario. Los errores de negocio tienen su propio texto;
 * cualquier otro (red, base de datos) usa [errorInesperado], que recibe el detalle técnico.
 */
fun Throwable.aMensaje(@StringRes errorInesperado: Int = R.string.error_verificar_usuario): UiText =
    when (this) {
        is ErrorUsuario.CamposIncompletos -> UiText.de(R.string.error_complete_campos)
        is ErrorUsuario.LongitudInsuficiente -> UiText.Plural(R.plurals.error_longitud_minima, minimo)
        is ErrorUsuario.LongitudExcedida -> UiText.Plural(R.plurals.error_longitud_maxima, maximo)
        is ErrorUsuario.CaracteresInvalidos -> UiText.de(R.string.error_caracteres_invalidos)
        is ErrorUsuario.NombreEnUso -> UiText.de(R.string.error_nombre_en_uso)
        is ErrorUsuario.UsuarioInexistente -> UiText.de(R.string.error_usuario_inexistente)
        is ErrorUsuario.MailInvalido -> UiText.de(R.string.error_mail_invalido)
        is ErrorUsuario.MailEnUso -> UiText.de(R.string.error_mail_en_uso)
        is ErrorUsuario.MailSinVerificar -> UiText.de(R.string.error_mail_sin_verificar)
        is ErrorUsuario.ContrasenaCorta -> UiText.Plural(R.plurals.error_contrasena_corta, minimo)
        is ErrorUsuario.CredencialesIncorrectas -> UiText.de(R.string.error_credenciales_incorrectas)
        is ErrorUsuario.DemasiadosIntentos -> UiText.de(R.string.error_demasiados_intentos)
        is ErrorUsuario.SinConexion -> UiText.de(R.string.error_sin_conexion)
        is ErrorUsuario.SinSesion -> UiText.de(R.string.error_sin_sesion)
        is ErrorUsuario.PerfilYaVinculado -> UiText.de(R.string.error_perfil_ya_vinculado)
        is ErrorUsuario.PerfilCreadoPorOtro -> UiText.de(R.string.error_perfil_creado_por_otro)
        is ErrorUsuario.ContrasenaAnteriorIncorrecta -> UiText.de(R.string.error_contrasena_anterior_incorrecta)
        is ErrorUsuario.EsElUsuarioActual -> UiText.de(R.string.error_es_usuario_actual)
        is ErrorUsuario.YaEsAmigo -> UiText.de(R.string.error_ya_es_amigo)
        is ErrorUsuario.NoEsAmigo -> UiText.de(R.string.error_no_es_amigo)
        else -> {
            Log.e("PuntajeBuraco", "Error inesperado", this)
            UiText.de(errorInesperado, localizedMessage ?: toString())
        }
    }
