package com.example.puntajeburaco20.ui.nuevapartida

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.databinding.FragmentNuevaPartidaBinding
import com.example.puntajeburaco20.domain.model.ModoJuego
import com.example.puntajeburaco20.ui.common.SelectorJugador
import com.example.puntajeburaco20.ui.common.configurarOpciones
import com.example.puntajeburaco20.ui.common.confirmar
import com.example.puntajeburaco20.ui.common.mostrarMensaje
import com.example.puntajeburaco20.ui.common.recolectar
import com.example.puntajeburaco20.ui.common.seleccionarSiCambia
import dagger.hilt.android.AndroidEntryPoint

/**
 * Destino inicial de la navegación. Redirige al login si no hay sesión, y a la partida en curso
 * si la app se cerró en medio de una.
 */
@AndroidEntryPoint
class NuevaPartidaFragment : Fragment(R.layout.fragment_nueva_partida) {

    private val viewModel: NuevaPartidaViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNuevaPartidaBinding.bind(view)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            confirmar(R.string.dialogo_salir_app) { requireActivity().finish() }
        }

        // El orden de R.array.modos_juego coincide con el de ModoJuego.
        binding.spinnerJugadores.configurarOpciones(R.array.modos_juego) { posicion ->
            viewModel.cambiarModo(ModoJuego.entries[posicion])
        }
        val textoVacio = getString(R.string.placeholder_jugador)
        val selectores = listOf(binding.spinner1, binding.spinner2, binding.spinner3, binding.spinner4)
            .mapIndexed { posicion, spinner ->
                SelectorJugador(spinner, textoVacio) { viewModel.elegirJugador(posicion, it) }
            }

        binding.btnNuevaPartida.setOnClickListener { viewModel.iniciarPartida() }
        binding.btnUsuario.setOnClickListener {
            findNavController().navigate(R.id.action_nuevaPartida_to_amigos)
        }
        binding.btnHistoriales.setOnClickListener {
            findNavController().navigate(R.id.action_nuevaPartida_to_historial)
        }
        binding.btnCerrarSesion.setOnClickListener {
            confirmar(R.string.dialogo_cerrar_sesion) { viewModel.cerrarSesion() }
        }

        recolectar(viewModel.estado) { estado ->
            if (estado.sinSesion) {
                irALogin()
                return@recolectar
            }
            val seleccion = estado.seleccion
            val esParejas = seleccion.modo == ModoJuego.PAREJAS

            binding.nombreUsuario.text = estado.nombreUsuario
            binding.spinnerJugadores.seleccionarSiCambia(seleccion.modo.ordinal)
            binding.spinner3.isVisible = esParejas
            binding.spinner4.isVisible = esParejas
            // En partidas de 2 el segundo jugador es el rival; en las de 4, el compañero.
            binding.spinner2.setBackgroundResource(
                if (esParejas) R.drawable.spinner_equipo_uno else R.drawable.spinner_equipo_dos
            )
            selectores.forEachIndexed { posicion, selector ->
                selector.mostrar(seleccion.opcionesPara(posicion), seleccion.elegido(posicion))
            }
            binding.btnNuevaPartida.isEnabled = !estado.iniciando
            binding.txtSincronizacion.isVisible = estado.cambiosPendientes
        }
        recolectar(viewModel.eventos) { evento ->
            when (evento) {
                is NuevaPartidaViewModel.Evento.Mensaje -> mostrarMensaje(evento.texto)
                NuevaPartidaViewModel.Evento.IrAPartida ->
                    findNavController().navigate(R.id.action_nuevaPartida_to_puntaje)
            }
        }
    }

    private fun irALogin() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.nuevaPartidaFragment) {
            navController.navigate(R.id.action_nuevaPartida_to_login)
        }
    }
}
