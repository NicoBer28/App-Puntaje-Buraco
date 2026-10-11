package com.example.puntajeburaco20.ui.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.LimitePuntos
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** Ajustes de la app en este dispositivo: el tema, el límite de puntos y la pantalla encendida. */
@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val preferencias: PreferenciasRepository,
) : ViewModel() {

    data class Estado(
        val modoTema: ModoTema,
        val limite: LimitePuntos,
        val pantallaEncendida: Boolean,
    )

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
    }

    /** `null` hasta que se leen las preferencias guardadas. */
    val estado: StateFlow<Estado?> =
        combine(preferencias.modoTema, preferencias.limitePuntos, preferencias.pantallaEncendida, ::Estado)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _eventos = Channel<Evento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    /** Los cambios del límite parten de lo guardado: se aplican de a uno para que ninguno pise a otro. */
    private val escritura = Mutex()

    fun cambiarTema(modo: ModoTema) = guardar { preferencias.cambiarModoTema(modo) }

    fun activarLimite(activo: Boolean) = guardar {
        preferencias.cambiarLimitePuntos(preferencias.limitePuntos.first().copy(activo = activo))
    }

    /** Un valor fuera de [LimitePuntos.RANGO] se ignora: queda el que estaba. */
    fun cambiarLimite(puntos: Int) {
        if (puntos !in LimitePuntos.RANGO) return
        guardar { preferencias.cambiarLimitePuntos(preferencias.limitePuntos.first().copy(puntos = puntos)) }
    }

    fun cambiarPantallaEncendida(encendida: Boolean) = guardar { preferencias.cambiarPantallaEncendida(encendida) }

    private fun guardar(cambio: suspend () -> Unit) {
        viewModelScope.launch {
            intentar { escritura.withLock { cambio() } }.onFailure {
                _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_inesperado)))
            }
        }
    }
}
