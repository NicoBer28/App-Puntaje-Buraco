package com.example.puntajeburaco20.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.domain.usecase.CerrarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarUsuarioActualUseCase
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

/** Perfil del usuario: sus datos, el tema de la app y cerrar sesión. */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val preferencias: PreferenciasRepository,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    observarUsuarioActual: ObservarUsuarioActualUseCase,
) : ViewModel() {

    data class Estado(
        val nombreUsuario: String = "",
        val cantidadAmigos: Int = 0,
        val modoTema: ModoTema = ModoTema.SISTEMA,
        val cerrandoSesion: Boolean = false,
    )

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
        data object SesionCerrada : Evento
    }

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private val _eventos = Channel<Evento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            observarUsuarioActual()
                .catch { _eventos.send(Evento.Mensaje(it.aMensaje())) }
                .collect { usuario ->
                    // Al cerrar sesión deja de haber usuario: se conservan los datos mientras se sale.
                    if (usuario != null) {
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

    fun cerrarSesion() {
        if (_estado.value.cerrandoSesion) return
        viewModelScope.launch {
            _estado.update { it.copy(cerrandoSesion = true) }
            intentar { cerrarSesionUseCase() }
                .onSuccess { _eventos.send(Evento.SesionCerrada) }
                .onFailure { error ->
                    _estado.update { it.copy(cerrandoSesion = false) }
                    _eventos.send(Evento.Mensaje(error.aMensaje(R.string.error_inesperado)))
                }
        }
    }
}
