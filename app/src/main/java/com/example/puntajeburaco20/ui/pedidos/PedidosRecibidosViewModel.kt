package com.example.puntajeburaco20.ui.pedidos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.domain.model.PedidoDeReclamo
import com.example.puntajeburaco20.domain.usecase.AceptarPedidoUseCase
import com.example.puntajeburaco20.domain.usecase.ObservarPedidosRecibidosUseCase
import com.example.puntajeburaco20.domain.usecase.RechazarPedidoUseCase
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.aMensaje
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Los pedidos que le hicieron al usuario por perfiles que creó para otros: alguien dice ser el
 * dueño de uno, y él tiene que confirmarlo o negarlo. Se muestran de a uno.
 */
@HiltViewModel
class PedidosRecibidosViewModel @Inject constructor(
    observarPedidosRecibidos: ObservarPedidosRecibidosUseCase,
    private val aceptarPedido: AceptarPedidoUseCase,
    private val rechazarPedido: RechazarPedidoUseCase,
) : ViewModel() {

    /**
     * @param pedido el que hay que responder ahora, o `null` si no queda ninguno.
     * @param error por qué falló la última respuesta a ese pedido.
     */
    data class Estado(val pedido: PedidoDeReclamo? = null, val cargando: Boolean = false, val error: UiText? = null)

    /** Lo que pasó con la última respuesta, y a qué pedido corresponde. */
    private data class Respuesta(val uid: String? = null, val cargando: Boolean = false, val error: UiText? = null)

    /** Los que el usuario dejó para después: no se vuelven a mostrar hasta la próxima vez que abra la app. */
    private val pospuestos = MutableStateFlow(emptySet<String>())
    private val respuesta = MutableStateFlow(Respuesta())

    val estado: StateFlow<Estado> = combine(
        observarPedidosRecibidos().catch { Log.e("PuntajeBuraco", "No se pudieron seguir los pedidos", it) },
        pospuestos,
        respuesta,
    ) { recibidos, pospuestos, respuesta ->
        val pedido = recibidos.firstOrNull { it.uid !in pospuestos }
        val deEste = respuesta.takeIf { pedido != null && it.uid == pedido.uid }
        Estado(pedido, cargando = deEste?.cargando == true, error = deEste?.error)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Estado())

    fun aceptar() = responder { aceptarPedido(it) }

    fun rechazar() = responder { rechazarPedido(it) }

    fun posponer() {
        estado.value.pedido?.let { pedido -> pospuestos.update { it + pedido.uid } }
    }

    private fun responder(operacion: suspend (PedidoDeReclamo) -> Unit) {
        val pedido = estado.value.pedido ?: return
        if (estado.value.cargando) return
        viewModelScope.launch {
            respuesta.value = Respuesta(pedido.uid, cargando = true)
            // Si sale bien, el pedido deja de estar entre los recibidos y se muestra el siguiente.
            val error = intentar { operacion(pedido) }.exceptionOrNull()
            respuesta.value = Respuesta(pedido.uid, error = error?.aMensaje())
        }
    }
}
