package com.example.puntajeburaco20.ui.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.CrearPerfilUseCase
import com.example.puntajeburaco20.domain.usecase.IniciarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.RecuperarContrasenaUseCase
import com.example.puntajeburaco20.domain.usecase.RegistrarCuentaUseCase
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Acceso a la app: ingresar o crear una cuenta, verificar el mail y elegir el nombre de usuario.
 * No decide qué paso se muestra: eso sale del estado de la sesión, que cambia solo cuando cada
 * operación termina bien.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesionUseCase,
    private val registrarCuenta: RegistrarCuentaUseCase,
    private val recuperarContrasenaUseCase: RecuperarContrasenaUseCase,
    private val crearPerfil: CrearPerfilUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val auth: AuthRepository,
) : ViewModel() {

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
    }

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _eventos = Channel<Evento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    fun ingresar(mail: String, password: String) = ejecutar { iniciarSesion(mail, password) }

    fun registrarse(mail: String, password: String) = ejecutar(exito = R.string.mensaje_verificacion_enviada) {
        registrarCuenta(mail, password)
    }

    fun recuperarContrasena(mail: String) = ejecutar(exito = R.string.mensaje_recuperacion_enviada) {
        recuperarContrasenaUseCase(mail)
    }

    fun reenviarVerificacion() = ejecutar(exito = R.string.mensaje_verificacion_enviada) {
        auth.enviarVerificacion()
    }

    /**
     * Vuelve a consultar si el mail ya fue verificado. Con [avisar] en `false` no muestra nada si
     * todavía no lo fue ni si la consulta falla: es la comprobación automática al volver a la app.
     */
    fun comprobarVerificacion(avisar: Boolean = true) = ejecutar(avisarErrores = avisar) {
        if (auth.recargar()?.verificada != true) throw ErrorUsuario.MailSinVerificar
    }

    fun elegirNombre(nombre: String) = ejecutar { crearPerfil(nombre) }

    /** Cierra la sesión para poder entrar con otra cuenta. */
    fun salir() = ejecutar { cerrarSesion() }

    private fun ejecutar(
        @StringRes exito: Int? = null,
        avisarErrores: Boolean = true,
        operacion: suspend () -> Unit,
    ) {
        if (_cargando.value) return
        viewModelScope.launch {
            _cargando.value = true
            intentar { operacion() }
                .onSuccess { if (exito != null) _eventos.send(Evento.Mensaje(UiText.de(exito))) }
                .onFailure { if (avisarErrores) _eventos.send(Evento.Mensaje(it.aMensaje())) }
            _cargando.value = false
        }
    }
}
