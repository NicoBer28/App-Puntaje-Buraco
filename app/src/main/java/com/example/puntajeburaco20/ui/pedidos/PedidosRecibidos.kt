package com.example.puntajeburaco20.ui.pedidos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R

/**
 * Aviso, sobre cualquier pantalla, de que alguien pide un perfil que el usuario creó para otra
 * persona. Puede confirmarlo, negarlo o dejarlo para la próxima vez que abra la app.
 */
@Composable
fun PedidosRecibidos(viewModel: PedidosRecibidosViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val pedido = estado.pedido ?: return
    val contexto = LocalContext.current

    AlertDialog(
        onDismissRequest = viewModel::posponer,
        title = { Text(stringResource(R.string.pedido_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.pedido_detalle, pedido.mail, pedido.perfil.nombre),
                    style = MaterialTheme.typography.bodyLarge,
                )
                estado.error?.let {
                    Text(
                        it.resolver(contexto),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::aceptar,
                modifier = Modifier.testTag("btnAceptarPedido"),
                enabled = !estado.cargando,
            ) { Text(stringResource(R.string.accion_aceptar_pedido)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = viewModel::posponer, enabled = !estado.cargando) {
                    Text(stringResource(R.string.accion_despues))
                }
                TextButton(
                    onClick = viewModel::rechazar,
                    modifier = Modifier.testTag("btnRechazarPedido"),
                    enabled = !estado.cargando,
                ) { Text(stringResource(R.string.accion_rechazar_pedido)) }
            }
        },
    )
}
