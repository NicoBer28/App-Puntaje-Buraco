package com.example.puntajeburaco20.ui.login

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.databinding.FragmentLoginBinding
import com.example.puntajeburaco20.ui.common.mostrarMensaje
import com.example.puntajeburaco20.ui.common.recolectar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentLoginBinding.bind(view)

        // Sin sesión no hay a dónde volver: atrás cierra la app.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        binding.btnLogin.setOnClickListener {
            viewModel.ingresar(binding.usuario.text.toString(), binding.password.text.toString())
        }
        binding.btnCrear.setOnClickListener {
            viewModel.registrarse(binding.usuario.text.toString(), binding.password.text.toString())
        }

        recolectar(viewModel.cargando) { cargando ->
            binding.btnLogin.isEnabled = !cargando
            binding.btnCrear.isEnabled = !cargando
        }
        recolectar(viewModel.eventos) { evento ->
            when (evento) {
                is LoginViewModel.Evento.Mensaje -> mostrarMensaje(evento.texto)
                LoginViewModel.Evento.SesionIniciada -> findNavController().popBackStack()
            }
        }
    }
}
