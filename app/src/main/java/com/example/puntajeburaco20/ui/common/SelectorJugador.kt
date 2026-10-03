package com.example.puntajeburaco20.ui.common

import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Jugador

/**
 * Adapta un [Spinner] para elegir un jugador. La primera opción ([textoVacio]) significa
 * "ninguno". El estado vive en el ViewModel: esta clase solo lo refleja y avisa los cambios.
 */
class SelectorJugador(
    private val spinner: Spinner,
    private val textoVacio: String,
    alElegir: (Jugador?) -> Unit,
) {
    private var opciones: List<Jugador> = emptyList()

    private val adaptador = ArrayAdapter(
        spinner.context,
        R.layout.item_spinner_seleccionado,
        mutableListOf(textoVacio),
    ).apply {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    init {
        spinner.adapter = adaptador
        spinner.alSeleccionar { posicion -> alElegir(opciones.getOrNull(posicion - 1)) }
    }

    fun mostrar(opciones: List<Jugador>, elegido: Jugador?) {
        if (opciones != this.opciones) {
            this.opciones = opciones
            adaptador.setNotifyOnChange(false)
            adaptador.clear()
            adaptador.add(textoVacio)
            adaptador.addAll(opciones.map { it.nombre })
            adaptador.notifyDataSetChanged()
        }
        val posicion = elegido?.let { e -> opciones.indexOfFirst { it.esMismaPersona(e) } + 1 } ?: 0
        spinner.seleccionarSiCambia(posicion)
    }
}
