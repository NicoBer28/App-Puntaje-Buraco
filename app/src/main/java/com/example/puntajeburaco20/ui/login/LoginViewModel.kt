package com.example.puntajeburaco20.ui.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.model.Reclamo
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.SesionAnteriorRepository
import com.example.puntajeburaco20.domain.usecase.AceptarReclamoUseCase
import com.example.puntajeburaco20.domain.usecase.CancelarPedidoUseCase
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ConsultarPedidoPropioUseCase
import com.example.puntajeburaco20.domain.usecase.ConsultarReclamoUseCase
import com.example.puntajeburaco20.domain.usecase.CrearPerfilUseCase
import com.example.puntajeburaco20.domain.usecase.IniciarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarPedidoPropioUseCase
import com.example.puntajeburaco20.domain.usecase.RecuperarContrasenaUseCase
import com.example.puntajeburaco20.domain.usecase.RechazarReclamoUseCase
import com.example.puntajeburaco20.domain.usecase.RegistrarCuentaUseCase
import com.example.puntajeburaco20.domain.usecase.VincularPerfilAnteriorUseCase
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val consultarReclamo: ConsultarReclamoUseCase,
    private val aceptarReclamoUseCase: AceptarReclamoUseCase,
    private val rechazarReclamoUseCase: RechazarReclamoUseCase,
    private val consultarPedidoPropio: ConsultarPedidoPropioUseCase,
    private val observarPedidoPropio: ObservarPedidoPropioUseCase,
    private val cancelarPedidoUseCase: CancelarPedidoUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val auth: AuthRepository,
    sesionAnterior: SesionAnteriorRepository,
) : ViewModel() {

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
    }

    /** Qué tiene por delante la cuenta que todavía no tiene perfil, antes de elegir uno. */
    sealed interface EstadoReclamo {
        data object Buscando : EstadoReclamo

        /** Nada: elige un nombre o recupera el perfil que ya tenía. */
        data object Ninguno : EstadoReclamo

        /** Alguien le dejó un perfil reservado para su mail: tiene que decir si es suyo. */
        data class Pendiente(val reclamo: Reclamo) : EstadoReclamo

        /** Pidió un perfil que creó otra persona y espera que esa persona lo confirme. */
        data class Esperando(val pedido: PedidoDeReclamo) : EstadoReclamo
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

    private val _reclamo = MutableStateFlow<EstadoReclamo>(EstadoReclamo.Buscando)
    val reclamo: StateFlow<EstadoReclamo> = _reclamo.asStateFlow()

    /** Uid de la cuenta para la que ya se buscó un perfil reservado. */
    private var cuentaConsultada: String? = null

    /** Mientras hay un pedido sin resolver, espera a que quien creó el perfil responda. */
    private var esperaDeRespuesta: Job? = null

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

    /**
     * Antes de que la [cuenta] elija su perfil, busca si alguien le dejó uno reservado. Lo hace
     * una sola vez por cuenta, así que se puede llamar cada vez que se muestra ese paso.
     */
    fun prepararEleccionDePerfil(cuenta: Cuenta) {
        if (cuentaConsultada == cuenta.uid) return
        cuentaConsultada = cuenta.uid
        buscarReclamo()
    }

    /** @param pedidoResuelto `true` si se busca porque el pedido que esperaba dejó de existir. */
    private fun buscarReclamo(pedidoResuelto: Boolean = false) {
        _reclamo.value = EstadoReclamo.Buscando
        esperaDeRespuesta?.cancel()
        viewModelScope.launch {
            // Si la búsqueda falla se sigue como si no hubiera nada: crear o recuperar un perfil
            // lo vuelve a comprobar, y avisa con ReclamoPendiente.
            val reclamo = intentar { consultarReclamo() }.getOrNull()
            val pedido = if (reclamo == null) intentar { consultarPedidoPropio() }.getOrNull() else null
            when {
                reclamo != null -> _reclamo.value = EstadoReclamo.Pendiente(reclamo)
                pedido != null -> esperar(pedido)
                else -> {
                    _reclamo.value = EstadoReclamo.Ninguno
                    // El pedido se fue sin que le reservaran el perfil: se lo rechazaron.
                    if (pedidoResuelto) _eventos.send(Evento.Mensaje(UiText.de(R.string.mensaje_pedido_rechazado)))
                }
            }
        }
    }

    /**
     * Queda a la espera de la respuesta al [pedido]. Cuando deja de existir vuelve a buscar: si
     * lo aceptaron, ahora tiene el perfil reservado para su mail.
     */
    private fun esperar(pedido: PedidoDeReclamo) {
        _reclamo.value = EstadoReclamo.Esperando(pedido)
        esperaDeRespuesta?.cancel()
        esperaDeRespuesta = viewModelScope.launch {
            observarPedidoPropio().first { it == null }
            buscarReclamo(pedidoResuelto = true)
        }
    }

    /** Desiste del perfil que había pedido y pasa a elegir un nombre como cualquier cuenta nueva. */
    fun cancelarPedido() = ejecutar {
        val pedido = (_reclamo.value as? EstadoReclamo.Esperando)?.pedido ?: return@ejecutar
        // Se deja de esperar antes de borrarlo: que desaparezca no es que se lo hayan rechazado.
        esperaDeRespuesta?.cancel()
        try {
            cancelarPedidoUseCase()
        } catch (e: Exception) {
            esperar(pedido)
            throw e
        }
        _reclamo.value = EstadoReclamo.Ninguno
    }

    fun aceptarReclamo() = ejecutar { aceptarReclamoUseCase() }

    /** El perfil reservado no era suyo: pasa a elegir un nombre como cualquier cuenta nueva. */
    fun rechazarReclamo() = ejecutar {
        rechazarReclamoUseCase()
        _reclamo.value = EstadoReclamo.Ninguno
    }

    fun elegirNombre(nombre: String) = ejecutar { crearPerfil(nombre) }

    /**
     * En lugar de elegir un nombre nuevo, se queda con el perfil que ya usaba. Si ese perfil se
     * lo creó otra persona, queda esperando a que lo confirme.
     */
    fun vincularPerfil(nombre: String, passwordAnterior: String) = ejecutar {
        val pedido = vincularPerfilAnterior(nombre, passwordAnterior)
        if (pedido == null) _usuarioAnterior.value = null else esperar(pedido)
    }

    /** Cierra la sesión para poder entrar con otra cuenta. */
    fun salir() = ejecutar {
        esperaDeRespuesta?.cancel()
        cerrarSesion()
        cuentaConsultada = null
    }

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
                .onFailure {
                    // El perfil reservado apareció o dejó de estar después de la búsqueda.
                    if (it is ErrorUsuario.ReclamoPendiente || it is ErrorUsuario.ReclamoNoDisponible) buscarReclamo()
                    if (avisarErrores) _eventos.send(Evento.Mensaje(it.aMensaje()))
                }
            _cargando.value = false
        }
    }
}
