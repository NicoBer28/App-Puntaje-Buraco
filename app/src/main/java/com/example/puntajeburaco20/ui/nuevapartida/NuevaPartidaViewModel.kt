package com.example.puntajeburaco20.ui.nuevapartida

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.ModoJuego
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.SincronizacionRepository
import com.example.puntajeburaco20.domain.usecase.ObservarUsuarioActualUseCase
import com.example.puntajeburaco20.ui.common.SeleccionJugadores
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pantalla principal: elegir jugadores y empezar una partida. */
@OptIn(FlowPreview::class)
@HiltViewModel
class NuevaPartidaViewModel @Inject constructor(
    private val partidaEnCurso: PartidaEnCursoRepository,
    observarUsuarioActual: ObservarUsuarioActualUseCase,
    sincronizacion: SincronizacionRepository,
) : ViewModel() {

    data class Estado(
        val nombreUsuario: String = "",
        val seleccion: SeleccionJugadores = SeleccionJugadores(),
        val iniciando: Boolean = false,
        /** Hay datos guardados en el dispositivo que todavía no se subieron (ej. sin conexión). */
        val cambiosPendientes: Boolean = false,
    )

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
        data object IrAPartida : Evento
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
                    val disponibles = usuario?.jugadoresDisponibles().orEmpty()
                    _estado.update {
                        it.copy(
                            nombreUsuario = usuario?.nombre.orEmpty(),
                            seleccion = it.seleccion.conDisponibles(disponibles),
                        )
                    }
                }
        }
        viewModelScope.launch {
            sincronizacion.hayCambiosPendientes
                // Con conexión, una escritura se confirma enseguida: no vale la pena avisar.
                .debounce { pendientes -> if (pendientes) DEMORA_AVISO_PENDIENTES_MS else 0L }
                .collect { pendientes -> _estado.update { it.copy(cambiosPendientes = pendientes) } }
        }
        retomarPartidaEnCurso()
    }

    fun cambiarModo(modo: ModoJuego) {
        _estado.update { it.copy(seleccion = it.seleccion.conModo(modo)) }
    }

    fun elegirJugador(posicion: Int, jugador: Jugador?) {
        _estado.update { it.copy(seleccion = it.seleccion.elegir(posicion, jugador)) }
    }

    fun iniciarPartida() {
        val estadoActual = _estado.value
        if (estadoActual.iniciando) return
        val jugadores = estadoActual.seleccion.visibles
        if (jugadores.any { it == null }) {
            _eventos.trySend(Evento.Mensaje(UiText.de(R.string.error_elija_jugador)))
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(iniciando = true) }
            intentar { partidaEnCurso.guardar(Partida.nueva(jugadores.filterNotNull())) }
                .onSuccess { _eventos.send(Evento.IrAPartida) }
                .onFailure { _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_inesperado))) }
            _estado.update { it.copy(iniciando = false) }
        }
    }

    /** Si la app se cerró en medio de una partida, se vuelve directo a ella. */
    private fun retomarPartidaEnCurso() {
        viewModelScope.launch {
            if (partidaEnCurso.obtener() != null) {
                _eventos.send(Evento.IrAPartida)
            }
        }
    }

    private companion object {
        const val DEMORA_AVISO_PENDIENTES_MS = 1_500L
    }
}
