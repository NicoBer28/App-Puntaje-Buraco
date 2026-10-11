package com.example.puntajeburaco20.ui.puntaje

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.data.vision.DetectorFichas
import com.example.puntajeburaco20.di.IoDispatcher
import com.example.puntajeburaco20.domain.model.CajaNormalizada
import com.example.puntajeburaco20.domain.model.FichaDetectada
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.LimitePuntos
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.domain.repository.PartidaEnCursoRepository
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.domain.service.CalculadoraPuntosFichas
import com.example.puntajeburaco20.domain.usecase.RegistrarResultadoPartidaUseCase
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Anotador de una partida en curso, con suma automática de fichas por cámara. */
@HiltViewModel
class PuntajeViewModel @Inject constructor(
    private val partidaEnCurso: PartidaEnCursoRepository,
    preferencias: PreferenciasRepository,
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
        /** Lo que valen las fichas contadas con la cámara; [puntos] es negativo si descuentan. */
        data class SumarPuntosDetectados(val lado: LadoEquipo, val puntos: Int) : Evento
        data object Salir : Evento
    }

    /**
     * Foto sacada para contar las fichas. Mientras se analiza, [fichas] es `null`. Las fichas se
     * pueden corregir a mano antes de sumarlas.
     */
    data class FotoFichas(
        val imagen: Bitmap,
        val fichas: List<FichaDetectada>? = null,
        val puntos: Int = 0,
    ) {
        val analizando: Boolean get() = fichas == null
    }

    /**
     * Un equipo llegó al límite de puntos con la ronda que se quiso sumar. La [partida], con esa
     * ronda ya incluida, no se anota hasta que los jugadores confirmen que está bien.
     */
    data class GanadorPorLimite(val partida: Partida, val lado: LadoEquipo, val limite: Int)

    private val _partida = MutableStateFlow<Partida?>(null)
    val partida: StateFlow<Partida?> = _partida.asStateFlow()

    /** El límite que rige es el de la configuración al sumar cada ronda, no el del comienzo de la partida. */
    private val limite = preferencias.limitePuntos.stateIn(viewModelScope, SharingStarted.Eagerly, LimitePuntos())

    private val _ganadorPorLimite = MutableStateFlow<GanadorPorLimite?>(null)
    val ganadorPorLimite: StateFlow<GanadorPorLimite?> = _ganadorPorLimite.asStateFlow()

    val pantallaEncendida: StateFlow<Boolean> =
        preferencias.pantallaEncendida.stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    private val _foto = MutableStateFlow<FotoFichas?>(null)
    val foto: StateFlow<FotoFichas?> = _foto.asStateFlow()

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
        val limiteActual = limite.value
        val ganador = if (limiteActual.activo) nueva.ganadorAlLlegarA(limiteActual.puntos) else null
        if (ganador != null) {
            // Terminar no se puede deshacer: antes se pide confirmar, por si la ronda tiene un error.
            _ganadorPorLimite.value = GanadorPorLimite(nueva, ganador, limiteActual.puntos)
            return
        }
        _partida.value = nueva
        _eventos.trySend(Evento.LimpiarRonda)
        avisarQuienEmpieza(nueva)
        guardar(nueva)
    }

    /** Los jugadores confirman la ronda con la que un equipo llegó al límite: se anota y la partida termina. */
    fun confirmarGanador() {
        val pendiente = _ganadorPorLimite.value ?: return
        _ganadorPorLimite.value = null
        _eventos.trySend(Evento.LimpiarRonda)
        terminar(pendiente.partida, pendiente.lado)
    }

    /** La ronda con la que un equipo llegaba al límite tenía un error: no se anota y queda escrita para corregirla. */
    fun corregirRonda() {
        _ganadorPorLimite.value = null
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
        terminar(actual, ganador)
    }

    private fun terminar(partida: Partida, ganador: LadoEquipo) {
        val terminada = partida.finalizar(ganador)
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
        // Con una foto en pantalla la vista previa no se analiza: el detector está ocupado con ella.
        if (!_camaraActiva.value || _foto.value != null) return
        val fichas = detector.detectar(imagen, rotacion)
        _fichasEnPantalla.value = fichas
        if (fichas.isNotEmpty()) ultimaDeteccion = fichas
    }

    /** La cámara entrega la foto ya derecha, desde un hilo secundario. Se analiza por recortes. */
    fun analizarFoto(imagen: Bitmap) {
        if (!_camaraActiva.value) return
        _foto.value = FotoFichas(imagen)
        viewModelScope.launch {
            intentar { withContext(io) { detector.detectarFoto(imagen) } }
                .onSuccess { fichas ->
                    // Si mientras tanto se descartó la foto (o se sacó otra), el resultado ya no sirve.
                    if (_foto.value?.imagen === imagen) _foto.value = conFichas(FotoFichas(imagen), fichas)
                }
                .onFailure {
                    if (_foto.value?.imagen === imagen) _foto.value = null
                    _eventos.send(Evento.Mensaje(it.aMensaje(R.string.error_analizar_foto)))
                }
        }
    }

    fun informarErrorFoto(error: Throwable) {
        _foto.value = null
        _eventos.trySend(Evento.Mensaje(error.aMensaje(R.string.error_analizar_foto)))
    }

    /** Vuelve a la vista previa para sacar otra foto. */
    fun descartarFoto() {
        _foto.value = null
    }

    /** Cambia el valor de una ficha mal leída, o la quita si [etiqueta] es `null`. */
    fun corregirFicha(indice: Int, etiqueta: String?) {
        val foto = _foto.value ?: return
        val fichas = foto.fichas ?: return
        if (indice !in fichas.indices) return
        val corregidas = if (etiqueta == null) {
            fichas.filterIndexed { i, _ -> i != indice }
        } else {
            fichas.mapIndexed { i, ficha -> if (i == indice) ficha.copy(etiqueta = etiqueta, confianza = 1f) else ficha }
        }
        _foto.value = conFichas(foto, corregidas)
    }

    /** Agrega una ficha que el detector no vio, en el lugar que se tocó. */
    fun agregarFicha(caja: CajaNormalizada, etiqueta: String) {
        val foto = _foto.value ?: return
        val fichas = foto.fichas ?: return
        _foto.value = conFichas(foto, fichas + FichaDetectada(etiqueta, 1f, caja))
    }

    private fun conFichas(foto: FotoFichas, fichas: List<FichaDetectada>) =
        foto.copy(fichas = fichas, puntos = calculadora.sumar(fichas))

    /**
     * Pasa a los puntos de la ronda lo que valen las fichas contadas y cierra la cámara.
     *
     * @param restar `true` si esas fichas descuentan, como las que quedaron en la mano.
     */
    fun sumarFichasDetectadas(lado: LadoEquipo, restar: Boolean) {
        // Si hay una foto analizada se suma esa (con las correcciones); si no, lo último que se vio en vivo.
        val fichas = _foto.value?.fichas ?: ultimaDeteccion
        cerrarCamara()
        if (fichas.isEmpty()) {
            _eventos.trySend(Evento.Mensaje(UiText.de(R.string.mensaje_sin_detecciones)))
            return
        }
        val puntos = calculadora.sumar(fichas)
        _eventos.trySend(Evento.SumarPuntosDetectados(lado, if (restar) -puntos else puntos))
        val aviso = if (restar) R.plurals.mensaje_puntos_restados else R.plurals.mensaje_puntos_detectados
        _eventos.trySend(Evento.Mensaje(UiText.Plural(aviso, puntos)))
    }

    private fun cerrarCamara() {
        _camaraActiva.value = false
        _fichasEnPantalla.value = emptyList()
        _foto.value = null
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
