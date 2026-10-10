package com.example.puntajeburaco20.ui.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.SesionAnteriorRepository
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.CrearPerfilUseCase
import com.example.puntajeburaco20.domain.usecase.IniciarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.RecuperarContrasenaUseCase
import com.example.puntajeburaco20.domain.usecase.RegistrarCuentaUseCase
import com.example.puntajeburaco20.domain.usecase.VincularPerfilAnteriorUseCase
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
 * Acceso a la app: ingresar o crear una cuenta, verificar el mail y elegir el nombre de usuario
 * (o recuperar el perfil que ya se tenía).
 * No decide qué paso se muestra: eso sale del estado de la sesión, que cambia solo cuando cada
 * operación termina bien.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesionUseCase,
    private val registrarCuenta: RegistrarCuentaUseCase,
    private val recuperarContrasenaUseCase: RecuperarContrasenaUseCase,
    private val crearPerfil: CrearPerfilUseCase,
    private val vincularPerfilAnterior: VincularPerfilAnteriorUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val auth: AuthRepository,
    sesionAnterior: SesionAnteriorRepository,
) : ViewModel() {

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
    }

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _eventos = Channel<Evento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    private val _usuarioAnterior = MutableStateFlow<String?>(null)

    /**
     * Usuario con el que este dispositivo entraba antes de que la app tuviera cuentas con mail, o
     * `null` si no había ninguno. Deja de estar una vez que esa persona vincula su perfil.
     */
    val usuarioAnterior: StateFlow<String?> = _usuarioAnterior.asStateFlow()

    init {
        viewModelScope.launch { _usuarioAnterior.value = sesionAnterior.nombreDeUsuario() }
    }

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

    /** En lugar de elegir un nombre nuevo, se queda con el perfil que ya usaba. */
    fun vincularPerfil(nombre: String, passwordAnterior: String) = ejecutar {
        vincularPerfilAnterior(nombre, passwordAnterior)
        _usuarioAnterior.value = null
    }

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
