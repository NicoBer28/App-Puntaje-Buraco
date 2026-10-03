package com.example.puntajeburaco20.ui.puntaje

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.databinding.FragmentPuntajeBinding
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.confirmar
import com.example.puntajeburaco20.ui.common.mostrarMensaje
import com.example.puntajeburaco20.ui.common.recolectar
import com.example.puntajeburaco20.ui.puntaje.camara.CamaraFichas
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class PuntajeFragment : Fragment(R.layout.fragment_puntaje) {

    private val viewModel: PuntajeViewModel by viewModels()

    private val pedirPermisoCamara =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) {
                viewModel.abrirCamara()
            } else {
                mostrarMensaje(UiText.de(R.string.error_permiso_camara))
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPuntajeBinding.bind(view)
        val camara = CamaraFichas(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            vistaPrevia = binding.viewFinder,
            alAnalizar = viewModel::analizarImagen,
        )

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            confirmarSalida()
        }
        binding.btnAtras.setOnClickListener { confirmarSalida() }
        binding.btnSumar.setOnClickListener { viewModel.sumarRonda(binding.rondaIngresada()) }
        binding.btnFin.setOnClickListener { elegirGanador() }
        binding.btnCamara1.setOnClickListener { abrirCamara() }
        binding.btnSumarEq1.setOnClickListener { viewModel.sumarFichasDetectadas(LadoEquipo.UNO) }
        binding.btnSumarEq2.setOnClickListener { viewModel.sumarFichasDetectadas(LadoEquipo.DOS) }
        binding.btnCerrarCamara.setOnClickListener { viewModel.cancelarCamara() }

        recolectar(viewModel.partida.filterNotNull()) { mostrarPartida(binding, it) }
        recolectar(viewModel.camaraActiva) { activa ->
            binding.cameraContainer.isVisible = activa
            if (activa) camara.iniciar(viewModel::informarErrorCamara) else camara.detener()
        }
        recolectar(viewModel.fichasEnPantalla) { binding.overlay.mostrar(it) }
        recolectar(viewModel.eventos) { evento ->
            when (evento) {
                is PuntajeViewModel.Evento.Mensaje -> mostrarMensaje(evento.texto)
                PuntajeViewModel.Evento.LimpiarRonda -> binding.limpiarRonda()
                is PuntajeViewModel.Evento.SumarPuntosDetectados ->
                    binding.campoPuntos(evento.lado).sumar(evento.puntos)
                PuntajeViewModel.Evento.Salir -> findNavController().popBackStack()
            }
        }
    }

    private fun mostrarPartida(binding: FragmentPuntajeBinding, partida: Partida) = with(binding) {
        val nombreUno = partida.equipoUno.nombreVisible()
        val nombreDos = partida.equipoDos.nombreVisible()
        equipoUno.text = nombreUno
        equipoDos.text = nombreDos
        btnSumarEq1.text = getString(R.string.accion_sumar_a_equipo, nombreUno)
        btnSumarEq2.text = getString(R.string.accion_sumar_a_equipo, nombreDos)

        baseAntUno.text = partida.ultimaRondaUno.base.toString()
        puntosAntUno.text = partida.ultimaRondaUno.puntos.toString()
        baseAntDos.text = partida.ultimaRondaDos.base.toString()
        puntosAntDos.text = partida.ultimaRondaDos.puntos.toString()
        totalUno.text = partida.totalUno.toString()
        totalDos.text = partida.totalDos.toString()
        empiezaJug.text = partida.empieza.nombre

        val enJuego = !partida.terminada
        listOf(btnSumar, btnFin, baseUno, puntosUno, baseDos, puntosDos).forEach {
            it.isEnabled = enJuego
        }
    }

    private fun elegirGanador() {
        val partida = viewModel.partida.value ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialogo_fin_titulo)
            .setMessage(R.string.dialogo_fin_mensaje)
            .setPositiveButton(partida.equipoDos.nombreVisible()) { _, _ ->
                viewModel.finalizar(LadoEquipo.DOS)
            }
            .setNegativeButton(partida.equipoUno.nombreVisible()) { _, _ ->
                viewModel.finalizar(LadoEquipo.UNO)
            }
            .setNeutralButton(R.string.cancelar, null)
            .show()
    }

    private fun confirmarSalida() {
        val terminada = viewModel.partida.value?.terminada == true
        val mensaje = if (terminada) R.string.dialogo_salir_partida_terminada else R.string.dialogo_salir_partida
        confirmar(mensaje) { viewModel.salir() }
    }

    private fun abrirCamara() {
        val permiso = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        if (permiso == PackageManager.PERMISSION_GRANTED) {
            viewModel.abrirCamara()
        } else {
            pedirPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun Equipo.nombreVisible(): String {
        val nombres = jugadores.map { it.nombre }
        return if (esPareja) getString(R.string.equipo_pareja, nombres[0], nombres[1]) else nombres[0]
    }

    private fun FragmentPuntajeBinding.rondaIngresada() = PuntajeViewModel.RondaIngresada(
        baseUno = baseUno.text.toString(),
        puntosUno = puntosUno.text.toString(),
        baseDos = baseDos.text.toString(),
        puntosDos = puntosDos.text.toString(),
    )

    private fun FragmentPuntajeBinding.limpiarRonda() {
        listOf(baseUno, puntosUno, baseDos, puntosDos).forEach { it.text.clear() }
    }

    private fun FragmentPuntajeBinding.campoPuntos(lado: LadoEquipo): EditText = when (lado) {
        LadoEquipo.UNO -> puntosUno
        LadoEquipo.DOS -> puntosDos
    }

    private fun EditText.sumar(puntos: Int) {
        val actuales = text.toString().toIntOrNull() ?: 0
        setText((actuales + puntos).toString())
    }
}
