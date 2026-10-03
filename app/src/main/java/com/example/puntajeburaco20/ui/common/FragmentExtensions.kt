package com.example.puntajeburaco20.ui.common

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.puntajeburaco20.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Recolecta el flujo solo mientras la vista del fragmento está visible. */
fun <T> Fragment.recolectar(flujo: Flow<T>, accion: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flujo.collect { accion(it) }
        }
    }
}

fun Fragment.mostrarMensaje(texto: UiText) {
    Toast.makeText(requireContext(), texto.resolver(requireContext()), Toast.LENGTH_SHORT).show()
}

/** Diálogo de confirmación "Sí / No". */
fun Fragment.confirmar(@StringRes mensaje: Int, alConfirmar: () -> Unit) {
    AlertDialog.Builder(requireContext())
        .setTitle(R.string.dialogo_confirmacion_titulo)
        .setMessage(mensaje)
        .setPositiveButton(R.string.si) { _, _ -> alConfirmar() }
        .setNegativeButton(R.string.no, null)
        .show()
}
