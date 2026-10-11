package com.example.puntajeburaco20.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.puntajeburaco20.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Muestra los mensajes de los ViewModels como snackbars. Uno nuevo reemplaza al que se esté viendo. */
@Stable
class Mensajero(
    val estado: SnackbarHostState,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    fun mostrar(texto: UiText) {
        scope.launch {
            estado.currentSnackbarData?.dismiss()
            estado.showSnackbar(texto.resolver(context))
        }
    }
}

@Composable
fun rememberMensajero(): Mensajero {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    return remember { Mensajero(SnackbarHostState(), scope, context) }
}

/** Recolecta los eventos de un ViewModel solo mientras la pantalla está visible. */
@Composable
fun <T> RecolectarEventos(eventos: Flow<T>, accion: suspend (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val accionActual by rememberUpdatedState(accion)
    LaunchedEffect(eventos, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            eventos.collect { accionActual(it) }
        }
    }
}

/** Diálogo de confirmación "Sí / No". */
@Composable
fun DialogoConfirmacion(
    @StringRes mensaje: Int,
    alConfirmar: () -> Unit,
    alCerrar: () -> Unit,
) = DialogoConfirmacion(stringResource(mensaje), alConfirmar, alCerrar)

/** Diálogo de confirmación "Sí / No", con un mensaje ya armado. */
@Composable
fun DialogoConfirmacion(
    mensaje: String,
    alConfirmar: () -> Unit,
    alCerrar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.dialogo_confirmacion_titulo)) },
        text = { Text(mensaje, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(
                onClick = {
                    alCerrar()
                    alConfirmar()
                },
            ) { Text(stringResource(R.string.si)) }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.no)) }
        },
    )
}
