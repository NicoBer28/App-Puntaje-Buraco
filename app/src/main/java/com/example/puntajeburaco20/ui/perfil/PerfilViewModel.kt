package com.example.puntajeburaco20.ui.perfil

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.domain.usecase.BorrarCuentaUseCase
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarUsuarioActualUseCase
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

/** Perfil del usuario: sus datos, el tema de la app, cambiar de nombre, cerrar sesión y borrar la cuenta. */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val preferencias: PreferenciasRepository,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    private val renombrarPerfil: RenombrarPerfilUseCase,
    private val borrarCuentaUseCase: BorrarCuentaUseCase,
    observarUsuarioActual: ObservarUsuarioActualUseCase,
) : ViewModel() {

    data class Estado(
        val nombreUsuario: String = "",
        val cantidadAmigos: Int = 0,
        val modoTema: ModoTema = ModoTema.SISTEMA,
        val cerrandoSesion: Boolean = false,
        /** `true` mientras se cambia el nombre o se borra la cuenta. */
        val ocupado: Boolean = false,
    )

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
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
        viewModelScope.launch {
            preferencias.modoTema.collect { modo -> _estado.update { it.copy(modoTema = modo) } }
        }
    }

    fun cambiarTema(modo: ModoTema) {
        viewModelScope.launch {
            intentar { preferencias.cambiarModoTema(modo) }.onFailure {
                _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_inesperado)))
            }
        }
    }

    fun cambiarNombre(nombre: String) = ejecutar(exito = R.string.mensaje_nombre_cambiado) {
        renombrarPerfil(jugador ?: throw ErrorUsuario.SinSesion, nombre)
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
