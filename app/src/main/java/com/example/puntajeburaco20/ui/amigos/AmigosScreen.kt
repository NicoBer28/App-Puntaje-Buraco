package com.example.puntajeburaco20.ui.amigos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.BotonSecundario
import com.example.puntajeburaco20.ui.common.CampoTexto
import com.example.puntajeburaco20.ui.common.Encabezado
import com.example.puntajeburaco20.ui.common.PantallaBuraco
import com.example.puntajeburaco20.ui.common.RecolectarEventos
import com.example.puntajeburaco20.ui.common.Tarjeta
import com.example.puntajeburaco20.ui.common.TituloSeccion
import com.example.puntajeburaco20.ui.common.rememberMensajero

@Composable
fun AmigosScreen(
    alVolver: () -> Unit,
    viewModel: AmigosViewModel = hiltViewModel(),
) {
    val cargando by viewModel.cargando.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()
    var amigo by rememberSaveable { mutableStateOf("") }
    var usuarioNuevo by rememberSaveable { mutableStateOf("") }

    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is AmigosViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
            AmigosViewModel.Evento.LimpiarAmigo -> amigo = ""
            AmigosViewModel.Evento.LimpiarNuevoUsuario -> usuarioNuevo = ""
        }
    }

    PantallaBuraco(
        mensajero = mensajero,
        encabezado = {
            Encabezado(
                titulo = stringResource(R.string.titulo_amigos),
                subtitulo = stringResource(R.string.subtitulo_amigos),
                alVolver = alVolver,
            )
        },
    ) {
        Tarjeta {
            TituloSeccion(
                stringResource(R.string.seccion_agregar_amigo),
                stringResource(R.string.seccion_agregar_amigo_detalle),
            )
            CampoTexto(
                valor = amigo,
                alCambiar = { amigo = it },
                etiqueta = stringResource(R.string.hint_usuario_amigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("usuarioAmigo"),
                teclado = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                acciones = KeyboardActions(onDone = { viewModel.agregar(amigo) }),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonSecundario(
                    texto = stringResource(R.string.accion_eliminar_amigo),
                    alTocar = { viewModel.eliminar(amigo) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btnEliminar"),
                    icono = R.drawable.ic_person_remove,
                    habilitado = !cargando,
                    color = MaterialTheme.colorScheme.error,
                )
                BotonPrincipal(
                    texto = stringResource(R.string.accion_agregar_amigo),
                    alTocar = { viewModel.agregar(amigo) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btnAgregar"),
                    icono = R.drawable.ic_person_add,
                    habilitado = !cargando,
                )
            }
        }
        Tarjeta {
            TituloSeccion(
                stringResource(R.string.seccion_crear_cuenta),
                stringResource(R.string.seccion_crear_cuenta_detalle),
            )
            CampoTexto(
                valor = usuarioNuevo,
                alCambiar = { usuarioNuevo = it },
                etiqueta = stringResource(R.string.hint_usuario),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("usuarioCrear"),
                teclado = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                acciones = KeyboardActions(onDone = { viewModel.crearUsuario(usuarioNuevo) }),
            )
            BotonPrincipal(
                texto = stringResource(R.string.accion_crear_usuario),
                alTocar = { viewModel.crearUsuario(usuarioNuevo) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btnCrearUsuario"),
                icono = R.drawable.ic_group_add,
                cargando = cargando,
            )
        }
    }
}
