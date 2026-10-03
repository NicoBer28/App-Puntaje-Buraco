package com.example.puntajeburaco20.ui.puntaje

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.data.vision.DetectorFichas
import com.example.puntajeburaco20.di.IoDispatcher
import com.example.puntajeburaco20.domain.model.FichaDetectada
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.service.CalculadoraPuntosFichas
import com.example.puntajeburaco20.domain.usecase.RegistrarResultadoPartidaUseCase
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Anotador de una partida en curso, con suma automática de fichas por cámara. */
@HiltViewModel
class PuntajeViewModel @Inject constructor(
    private val partidaEnCurso: PartidaEnCursoRepository,
    private val registrarResultado: RegistrarResultadoPartidaUseCase,
    private val calculadora: CalculadoraPuntosFichas,
    private val detector: DetectorFichas,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ViewModel() {

    /** Lo que se ingresó en los campos de la ronda, tal cual está escrito. */
    data class RondaIngresada(
        val baseUno: String,
        val puntosUno: String,
        val baseDos: String,
        val puntosDos: String,
    ) {
        private val campos get() = listOf(baseUno, puntosUno, baseDos, puntosDos).map { it.trim() }

        val vacia: Boolean get() = campos.all { it.isEmpty() }

        /** Los puntajes de cada equipo, o `null` si falta algún campo o no es un número. */
        fun aPuntajes(): Pair<PuntajeRonda, PuntajeRonda>? {
            val (bUno, pUno, bDos, pDos) = campos.map { it.toIntOrNull() ?: return null }
            return PuntajeRonda(bUno, pUno) to PuntajeRonda(bDos, pDos)
        }
    }

    sealed interface Evento {
        data class Mensaje(val texto: UiText) : Evento

        /** Recordar quién empieza la próxima ronda (la pantalla lo destaca en el marcador). */
        data class AvisarQuienEmpieza(val jugador: Jugador) : Evento
        data object LimpiarRonda : Evento
        data class SumarPuntosDetectados(val lado: LadoEquipo, val puntos: Int) : Evento
        data object Salir : Evento
    }

    private val _partida = MutableStateFlow<Partida?>(null)
    val partida: StateFlow<Partida?> = _partida.asStateFlow()

    private val _camaraActiva = MutableStateFlow(false)
    val camaraActiva: StateFlow<Boolean> = _camaraActiva.asStateFlow()

    /** Fichas que la cámara ve en este momento (para dibujarlas). */
    private val _fichasEnPantalla = MutableStateFlow<List<FichaDetectada>>(emptyList())
    val fichasEnPantalla: StateFlow<List<FichaDetectada>> = _fichasEnPantalla.asStateFlow()

    /**
     * Última detección no vacía. Se suma esta y no la del cuadro actual para que un cuadro
     * borroso justo al tocar "Sumar" no haga perder la lectura.
     */
    @Volatile
    private var ultimaDeteccion: List<FichaDetectada> = emptyList()

    private val _eventos = Channel<Evento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            val guardada = partidaEnCurso.obtener()
            if (guardada == null) {
                _eventos.send(Evento.Salir)
                return@launch
            }
            _partida.value = guardada
            if (!guardada.terminada) avisarQuienEmpieza(guardada)
        }
    }

    fun sumarRonda(ronda: RondaIngresada) {
        val actual = _partida.value ?: return
        if (actual.terminada) return
        if (ronda.vacia) {
            avisarQuienEmpieza(actual)
            return
        }
        val (rondaUno, rondaDos) = ronda.aPuntajes() ?: run {
            _eventos.trySend(Evento.Mensaje(UiText.de(R.string.error_complete_campos)))
            return
        }
        val nueva = actual.registrarRonda(rondaUno, rondaDos)
        _partida.value = nueva
        _eventos.trySend(Evento.LimpiarRonda)
        avisarQuienEmpieza(nueva)
        guardar(nueva)
    }

    /** Corrige un puntaje mal cargado: quita la última ronda como si no se hubiera jugado. */
    fun deshacerRonda() {
        val actual = _partida.value ?: return
        if (!actual.sePuedeDeshacer) return
        val corregida = actual.deshacerUltimaRonda()
        _partida.value = corregida
        _eventos.trySend(Evento.Mensaje(UiText.de(R.string.mensaje_ronda_deshecha)))
        avisarQuienEmpieza(corregida)
        guardar(corregida)
    }

    fun finalizar(ganador: LadoEquipo) {
        val actual = _partida.value ?: return
        if (actual.terminada) return
        val terminada = actual.finalizar(ganador)
        _partida.value = terminada
        viewModelScope.launch {
            intentar {
                partidaEnCurso.guardar(terminada)
                registrarResultado(terminada)
            }.onFailure {
                _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_guardar_resultado)))
            }
        }
    }

    fun salir() {
        viewModelScope.launch {
            intentar { partidaEnCurso.eliminar() }.onFailure {
                _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_inesperado)))
            }
            _eventos.send(Evento.Salir)
        }
    }

    // --- Cámara ---------------------------------------------------------------------------

    fun abrirCamara() {
        viewModelScope.launch {
            intentar { withContext(io) { detector.preparar() } }
                .onSuccess {
                    ultimaDeteccion = emptyList()
                    _camaraActiva.value = true
                }
                .onFailure {
                    _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_detector_no_disponible)))
                }
        }
    }

    fun cancelarCamara() {
        cerrarCamara()
        _eventos.trySend(Evento.Mensaje(UiText.de(R.string.mensaje_camara_cancelada)))
    }

    fun informarErrorCamara(error: Throwable) {
        cerrarCamara()
        _eventos.trySend(Evento.Mensaje(error.aMensaje(R.string.error_camara)))
    }

    /** Se llama por cada cuadro de la cámara, desde su hilo de análisis. */
    fun analizarImagen(imagen: Bitmap, rotacion: Int) {
        if (!_camaraActiva.value) return
        val fichas = detector.detectar(imagen, rotacion)
        _fichasEnPantalla.value = fichas
        if (fichas.isNotEmpty()) ultimaDeteccion = fichas
    }

    fun sumarFichasDetectadas(lado: LadoEquipo) {
        val fichas = ultimaDeteccion
        cerrarCamara()
        if (fichas.isEmpty()) {
            _eventos.trySend(Evento.Mensaje(UiText.de(R.string.mensaje_sin_detecciones)))
            return
        }
        val puntos = calculadora.sumar(fichas)
        _eventos.trySend(Evento.SumarPuntosDetectados(lado, puntos))
        _eventos.trySend(Evento.Mensaje(UiText.Plural(R.plurals.mensaje_puntos_detectados, puntos)))
    }

    private fun cerrarCamara() {
        _camaraActiva.value = false
        _fichasEnPantalla.value = emptyList()
    }

    private fun avisarQuienEmpieza(partida: Partida) {
        _eventos.trySend(Evento.AvisarQuienEmpieza(partida.empieza))
    }

    private fun guardar(partida: Partida) {
        viewModelScope.launch {
            intentar { partidaEnCurso.guardar(partida) }.onFailure {
                _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_inesperado)))
            }
        }
    }
}
