package com.example.puntajeburaco20.ui.historial

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.databinding.FragmentHistorialBinding
import com.example.puntajeburaco20.domain.model.ModoJuego
import com.example.puntajeburaco20.ui.common.SelectorJugador
import com.example.puntajeburaco20.ui.common.configurarOpciones
import com.example.puntajeburaco20.ui.common.mostrarMensaje
import com.example.puntajeburaco20.ui.common.recolectar
import com.example.puntajeburaco20.ui.common.seleccionarSiCambia
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistorialFragment : Fragment(R.layout.fragment_historial) {

    private val viewModel: HistorialViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentHistorialBinding.bind(view)

        // El orden de R.array.modos_juego coincide con el de ModoJuego.
        binding.spnCantJug.configurarOpciones(R.array.modos_juego) { posicion ->
            viewModel.cambiarModo(ModoJuego.entries[posicion])
        }
        // Posiciones según HistorialViewModel: 0 y 2 a la izquierda, 1 y 3 a la derecha.
        val textoVacio = getString(R.string.placeholder_historial)
        val selectores = listOf(binding.spnJugUno, binding.spnJugDos, binding.spnJugTres, binding.spnJugCuatro)
            .mapIndexed { posicion, spinner ->
                SelectorJugador(spinner, textoVacio) { viewModel.elegirJugador(posicion, it) }
            }

        binding.btnBuscar.setOnClickListener { viewModel.buscar() }
        binding.btnVolver2.setOnClickListener { findNavController().popBackStack() }

        recolectar(viewModel.estado) { estado ->
            val seleccion = estado.seleccion
            val esParejas = seleccion.modo == ModoJuego.PAREJAS

            binding.spnCantJug.seleccionarSiCambia(seleccion.modo.ordinal)
            binding.spnJugTres.isVisible = esParejas
            binding.spnJugCuatro.isVisible = esParejas
            selectores.forEachIndexed { posicion, selector ->
                selector.mostrar(seleccion.opcionesPara(posicion), seleccion.elegido(posicion))
            }

            binding.jugUno.text = estado.equipoUno.jugadas.toString()
            binding.ganUno.text = estado.equipoUno.ganadas.toString()
            binding.perdUno.text = estado.equipoUno.perdidas.toString()
            binding.jugDos.text = estado.equipoDos.jugadas.toString()
            binding.ganDos.text = estado.equipoDos.ganadas.toString()
            binding.perdDos.text = estado.equipoDos.perdidas.toString()
            binding.btnBuscar.isEnabled = !estado.buscando
        }
        recolectar(viewModel.eventos) { evento ->
            when (evento) {
                is HistorialViewModel.Evento.Mensaje -> mostrarMensaje(evento.texto)
            }
        }
    }
}
