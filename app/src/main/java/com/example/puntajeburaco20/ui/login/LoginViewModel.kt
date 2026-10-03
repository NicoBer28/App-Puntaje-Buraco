package com.example.puntajeburaco20.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.usecase.IniciarSesionUseCase
import com.example.puntajeburaco20.domain.usecase.RegistrarUsuarioUseCase
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

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesionUseCase,
    private val registrarUsuario: RegistrarUsuarioUseCase,
) : ViewModel() {

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
        data object SesionIniciada : Evento
    }

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _eventos = Channel<Evento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    fun ingresar(nombre: String, password: String) = ejecutar {
        iniciarSesion(nombre, password)
    }

    fun registrarse(nombre: String, password: String) = ejecutar {
        registrarUsuario(nombre, password)
        _eventos.send(Evento.Mensaje(UiText.de(R.string.mensaje_usuario_creado)))
    }

    private fun ejecutar(operacion: suspend () -> Unit) {
        if (_cargando.value) return
        viewModelScope.launch {
            _cargando.value = true
            intentar { operacion() }
                .onSuccess { _eventos.send(Evento.SesionIniciada) }
                .onFailure { _eventos.send(Evento.Mensaje(it.aMensaje())) }
            _cargando.value = false
        }
    }
}
