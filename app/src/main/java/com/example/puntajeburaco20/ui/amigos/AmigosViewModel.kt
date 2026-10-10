package com.example.puntajeburaco20.ui.amigos

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.domain.usecase.AgregarAmigoUseCase
import com.example.puntajeburaco20.domain.usecase.CrearUsuarioAmigoUseCase
import com.example.puntajeburaco20.domain.usecase.EliminarAmigoUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarPerfilesACargoUseCase
import com.example.puntajeburaco20.domain.usecase.ReservarPerfilACargoUseCase
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Agregar o quitar amigos, y crear usuarios para quienes no usan la app. */
@HiltViewModel
class AmigosViewModel @Inject constructor(
    private val agregarAmigo: AgregarAmigoUseCase,
    private val eliminarAmigo: EliminarAmigoUseCase,
    private val crearUsuarioAmigo: CrearUsuarioAmigoUseCase,
    private val reservarPerfilACargo: ReservarPerfilACargoUseCase,
    observarPerfilesACargo: ObservarPerfilesACargoUseCase,
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

    /** Los usuarios que creó para otros y que todavía esperan a su dueño, con el mail de cada uno. */
    val perfilesACargo: StateFlow<List<PerfilACargo>> = observarPerfilesACargo()
        .catch { Log.e("PuntajeBuraco", "No se pudieron seguir los perfiles a cargo", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun crearUsuario(nombre: String, mail: String) = ejecutar(
        exito = R.string.mensaje_usuario_amigo_creado,
        limpiar = Evento.LimpiarNuevoUsuario,
    ) { crearUsuarioAmigo(nombre, mail) }

    /** Carga o corrige el mail para el que queda reservado un usuario que creó para otro. */
    fun cambiarMail(perfil: PerfilACargo, mail: String) = ejecutar(
        exito = R.string.mensaje_mail_guardado,
        campoUnico = true,
    ) { reservarPerfilACargo(perfil, mail) }

    private fun ejecutar(
        @StringRes exito: Int,
        limpiar: Evento? = null,
        campoUnico: Boolean = false,
        operacion: suspend () -> Unit,
    ) {
        if (_cargando.value) return
        viewModelScope.launch {
            _cargando.value = true
            intentar { operacion() }
                .onSuccess {
                    _eventos.send(Evento.Mensaje(UiText.de(exito)))
                    if (limpiar != null) _eventos.send(limpiar)
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
