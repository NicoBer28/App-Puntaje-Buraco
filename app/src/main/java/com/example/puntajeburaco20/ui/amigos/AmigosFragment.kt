package com.example.puntajeburaco20.ui.amigos

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.databinding.FragmentAmigosBinding
import com.example.puntajeburaco20.ui.common.mostrarMensaje
import com.example.puntajeburaco20.ui.common.recolectar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AmigosFragment : Fragment(R.layout.fragment_amigos) {

    private val viewModel: AmigosViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAmigosBinding.bind(view)

        binding.btnAgregar.setOnClickListener {
            viewModel.agregar(binding.usuarioAmigo.text.toString())
        }
        binding.btnEliminar.setOnClickListener {
            viewModel.eliminar(binding.usuarioAmigo.text.toString())
        }
        binding.btnCrearUsuario.setOnClickListener {
            viewModel.crearUsuario(binding.usuarioCrear.text.toString(), binding.passwordCrear.text.toString())
        }
        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }

        recolectar(viewModel.cargando) { cargando ->
            listOf(binding.btnAgregar, binding.btnEliminar, binding.btnCrearUsuario).forEach {
                it.isEnabled = !cargando
            }
        }
        recolectar(viewModel.eventos) { evento ->
            when (evento) {
                is AmigosViewModel.Evento.Mensaje -> mostrarMensaje(evento.texto)
                AmigosViewModel.Evento.LimpiarAmigo -> binding.usuarioAmigo.text.clear()
                AmigosViewModel.Evento.LimpiarNuevoUsuario -> {
                    binding.usuarioCrear.text.clear()
                    binding.passwordCrear.text.clear()
                }
            }
        }
    }
}
