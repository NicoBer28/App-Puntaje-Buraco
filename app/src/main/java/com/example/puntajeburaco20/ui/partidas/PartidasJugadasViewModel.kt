package com.example.puntajeburaco20.ui.partidas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.HistorialJugador
import com.example.puntajeburaco20.domain.usecase.ConsultarHistorialJugadorUseCase
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Últimas partidas del usuario, ronda por ronda, con su racha y puntaje promedio. */
@HiltViewModel
class PartidasJugadasViewModel @Inject constructor(
    private val consultarHistorial: ConsultarHistorialJugadorUseCase,
) : ViewModel() {

    data class Estado(
        val cargando: Boolean = false,
        /** `null` hasta que se cargue por primera vez. */
        val historial: HistorialJugador? = null,
    )

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
    }

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private val _eventos = Channel<Evento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        cargar()
    }

    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            intentar { consultarHistorial() }
                .onSuccess { historial -> _estado.update { it.copy(historial = historial) } }
                .onFailure { _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_cargar_partidas))) }
            _estado.update { it.copy(cargando = false) }
        }
    }
}
