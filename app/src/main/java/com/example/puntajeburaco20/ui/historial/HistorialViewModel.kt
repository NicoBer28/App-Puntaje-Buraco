package com.example.puntajeburaco20.ui.historial

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.ModoJuego
import com.example.puntajeburaco20.domain.usecase.ConsultarEstadisticasUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarUsuarioActualUseCase
import com.example.puntajeburaco20.ui.common.SeleccionJugadores
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
 * Consulta de estadísticas. Las posiciones 0 y 2 forman el equipo de la izquierda y las 1 y 3 el
 * de la derecha (en partidas de 2 solo se usan las posiciones 0 y 1). Si no se elige rival, se
 * muestran los totales generales del equipo de la izquierda.
 */
@HiltViewModel
class HistorialViewModel @Inject constructor(
    observarUsuarioActual: ObservarUsuarioActualUseCase,
    private val consultarEstadisticas: ConsultarEstadisticasUseCase,
) : ViewModel() {

    data class Estado(
        val seleccion: SeleccionJugadores = SeleccionJugadores(),
        val equipoUno: Estadisticas = Estadisticas.VACIAS,
        val equipoDos: Estadisticas = Estadisticas.VACIAS,
        val buscando: Boolean = false,
    )

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento
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
                    cambiarSeleccion { it.conDisponibles(usuario?.jugadoresDisponibles().orEmpty()) }
                }
        }
    }

    fun cambiarModo(modo: ModoJuego) = cambiarSeleccion { it.conModo(modo) }

    fun elegirJugador(posicion: Int, jugador: Jugador?) = cambiarSeleccion { it.elegir(posicion, jugador) }

    fun buscar() {
        val seleccion = _estado.value.seleccion
        val elegidos = seleccion.visibles
        when (seleccion.modo) {
            ModoJuego.INDIVIDUAL -> {
                val jugador = elegidos[0] ?: return avisar(R.string.error_complete_primer_campo)
                val rival = elegidos[1]
                consultar(
                    equipo = Equipo(listOf(jugador)),
                    rival = rival?.let { Equipo(listOf(it)) },
                    sinPartidas = if (rival == null) null else R.string.mensaje_nunca_jugaron_entre_ellos,
                )
            }
            ModoJuego.PAREJAS -> {
                val (izquierda1, derecha1, izquierda2, derecha2) = elegidos
                if (izquierda1 == null || izquierda2 == null) {
                    return avisar(R.string.error_complete_primer_campo)
                }
                if ((derecha1 == null) != (derecha2 == null)) {
                    return avisar(R.string.error_complete_segundo_campo)
                }
                val rival = if (derecha1 != null && derecha2 != null) Equipo(listOf(derecha1, derecha2)) else null
                consultar(
                    equipo = Equipo(listOf(izquierda1, izquierda2)),
                    rival = rival,
                    sinPartidas = if (rival == null) {
                        R.string.mensaje_nunca_jugaron_juntos
                    } else {
                        R.string.mensaje_nunca_jugaron_entre_ellos
                    },
                )
            }
        }
    }

    private fun consultar(equipo: Equipo, rival: Equipo?, @StringRes sinPartidas: Int?) {
        if (_estado.value.buscando) return
        viewModelScope.launch {
            _estado.update { it.copy(buscando = true) }
            intentar { consultarEstadisticas(equipo, rival) }
                .onSuccess { estadisticas ->
                    _estado.update {
                        it.copy(
                            equipoUno = estadisticas ?: Estadisticas.VACIAS,
                            equipoDos = if (rival == null) {
                                Estadisticas.VACIAS
                            } else {
                                estadisticas?.desdeElRival() ?: Estadisticas.VACIAS
                            },
                        )
                    }
                    if (estadisticas == null && sinPartidas != null) avisar(sinPartidas)
                }
                .onFailure {
                    _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_consultar_estadisticas)))
                }
            _estado.update { it.copy(buscando = false) }
        }
    }

    /** Cualquier cambio en la selección invalida el resultado mostrado. */
    private fun cambiarSeleccion(cambio: (SeleccionJugadores) -> SeleccionJugadores) {
        _estado.update { estado ->
            val nueva = cambio(estado.seleccion)
            if (nueva == estado.seleccion) {
                estado
            } else {
                estado.copy(
                    seleccion = nueva,
                    equipoUno = Estadisticas.VACIAS,
                    equipoDos = Estadisticas.VACIAS,
                )
            }
        }
    }

    private fun avisar(@StringRes mensaje: Int) {
        _eventos.trySend(Evento.Mensaje(UiText.de(mensaje)))
    }
}
