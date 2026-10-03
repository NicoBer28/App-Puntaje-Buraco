package com.example.puntajeburaco20.ui.amigos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.usecase.AgregarAmigoUseCase
import com.example.puntajeburaco20.domain.usecase.CrearUsuarioAmigoUseCase
import com.example.puntajeburaco20.domain.usecase.EliminarAmigoUseCase
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

/** Agregar o quitar amigos, y crear cuentas para quienes todavía no tienen una. */
@HiltViewModel
class AmigosViewModel @Inject constructor(
    private val agregarAmigo: AgregarAmigoUseCase,
    private val eliminarAmigo: EliminarAmigoUseCase,
    private val crearUsuarioAmigo: CrearUsuarioAmigoUseCase,
) : ViewModel() {

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
        data object LimpiarAmigo : Evento
        data object LimpiarNuevoUsuario : Evento
    }

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _eventos = Channel<Evento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    fun agregar(nombreAmigo: String) = ejecutar(
        exito = R.string.mensaje_amigo_agregado,
        limpiar = Evento.LimpiarAmigo,
        campoUnico = true,
    ) { agregarAmigo(nombreAmigo) }

    fun eliminar(nombreAmigo: String) = ejecutar(
        exito = R.string.mensaje_amigo_eliminado,
        limpiar = Evento.LimpiarAmigo,
        campoUnico = true,
    ) { eliminarAmigo(nombreAmigo) }

    fun crearUsuario(nombre: String, password: String) = ejecutar(
        exito = R.string.mensaje_usuario_amigo_creado,
        limpiar = Evento.LimpiarNuevoUsuario,
    ) { crearUsuarioAmigo(nombre, password) }

    private fun ejecutar(
        @StringRes exito: Int,
        limpiar: Evento,
        campoUnico: Boolean = false,
        operacion: suspend () -> Unit,
    ) {
        if (_cargando.value) return
        viewModelScope.launch {
            _cargando.value = true
            intentar { operacion() }
                .onSuccess {
                    _eventos.send(Evento.Mensaje(UiText.de(exito)))
                    _eventos.send(limpiar)
                }
                .onFailure { error ->
                    val mensaje = if (campoUnico && error is ErrorUsuario.CamposIncompletos) {
                        UiText.de(R.string.error_campo_vacio)
                    } else {
                        error.aMensaje()
                    }
                    _eventos.send(Evento.Mensaje(mensaje))
                }
            _cargando.value = false
        }
    }
}
