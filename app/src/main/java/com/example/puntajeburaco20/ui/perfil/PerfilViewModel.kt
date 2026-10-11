package com.example.puntajeburaco20.ui.perfil

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.domain.usecase.BorrarCuentaUseCase
import com.example.puntajeburaco20.domain.usecase.CambiarContrasenaUseCase
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarUsuarioActualUseCase
import com.example.puntajeburaco20.domain.usecase.RecuperarContrasenaUseCase
import com.example.puntajeburaco20.domain.usecase.RenombrarPerfilUseCase
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Perfil del usuario: sus datos, cambiar de nombre o de contraseña, volver a ver la ayuda, cerrar
 * sesión y borrar la cuenta.
 */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val preferencias: PreferenciasRepository,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    private val renombrarPerfil: RenombrarPerfilUseCase,
    private val cambiarContrasenaUseCase: CambiarContrasenaUseCase,
    private val recuperarContrasenaUseCase: RecuperarContrasenaUseCase,
    private val borrarCuentaUseCase: BorrarCuentaUseCase,
    observarUsuarioActual: ObservarUsuarioActualUseCase,
) : ViewModel() {

    data class Estado(
        val nombreUsuario: String = "",
        val cantidadAmigos: Int = 0,
        val cerrandoSesion: Boolean = false,
        /** `true` mientras se cambia el nombre o la contraseña, o se borra la cuenta. */
        val ocupado: Boolean = false,
    )

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento

        /** Los recorridos de ayuda quedaron pendientes: hay que volver al inicio, donde arranca el primero. */
        data object VerRecorrido : Evento
    }

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private val _eventos = Channel<Evento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    /** El perfil de quien tiene la sesión iniciada. */
    private var jugador: Jugador? = null

    init {
        viewModelScope.launch {
            observarUsuarioActual()
                .catch { _eventos.send(Evento.Mensaje(it.aMensaje())) }
                .collect { usuario ->
                    // Al cerrar sesión deja de haber usuario: se conservan los datos mientras se sale.
                    if (usuario != null) {
                        jugador = usuario.jugador
                        _estado.update {
                            it.copy(nombreUsuario = usuario.nombre, cantidadAmigos = usuario.amigos.size)
                        }
                    }
                }
        }
    }

    /** La ayuda es volver a pasar por los recorridos que se muestran la primera vez. */
    fun verAyuda() {
        viewModelScope.launch {
            intentar { preferencias.reiniciarRecorridos() }
                .onSuccess { _eventos.send(Evento.VerRecorrido) }
                .onFailure { _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_inesperado))) }
        }
    }

    fun cambiarNombre(nombre: String) = ejecutar(exito = R.string.mensaje_nombre_cambiado) {
        renombrarPerfil(jugador ?: throw ErrorUsuario.SinSesion, nombre)
    }

    fun cambiarContrasena(actual: String, nueva: String) = ejecutar(exito = R.string.mensaje_contrasena_cambiada) {
        cambiarContrasenaUseCase(actual, nueva)
    }

    /** Para quien no se acuerda de la actual: recibe por mail un enlace para elegir otra. */
    fun recuperarContrasena() = ejecutar {
        val mail = recuperarContrasenaUseCase.aLaCuentaActual()
        _eventos.send(Evento.Mensaje(UiText.de(R.string.mensaje_recuperacion_enviada_a, mail)))
    }

    /** Si sale bien no hay nada más que hacer: sin cuenta, la app pasa sola al acceso. */
    fun borrarCuenta(password: String) = ejecutar { borrarCuentaUseCase(password) }

    private fun ejecutar(@StringRes exito: Int? = null, operacion: suspend () -> Unit) {
        if (_estado.value.ocupado) return
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true) }
            intentar { operacion() }
                .onSuccess { if (exito != null) _eventos.send(Evento.Mensaje(UiText.de(exito))) }
                .onFailure { _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_inesperado))) }
            _estado.update { it.copy(ocupado = false) }
        }
    }

    fun cerrarSesion() {
        if (_estado.value.cerrandoSesion) return
        viewModelScope.launch {
            _estado.update { it.copy(cerrandoSesion = true) }
            // Si sale bien no hay nada más que hacer: sin sesión, la app pasa sola al acceso.
            intentar { cerrarSesionUseCase() }.onFailure { error ->
                _estado.update { it.copy(cerrandoSesion = false) }
                _eventos.send(Evento.Mensaje(error.aMensaje(R.string.error_inesperado)))
            }
        }
    }
}
