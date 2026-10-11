package com.example.puntajeburaco20.ui.amigos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.PerfilACargo
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.BotonSecundario
import com.example.puntajeburaco20.ui.common.CampoTexto
import com.example.puntajeburaco20.ui.common.DialogoConfirmacion
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
    val perfilesACargo by viewModel.perfilesACargo.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()
    var amigo by rememberSaveable { mutableStateOf("") }
    var usuarioNuevo by rememberSaveable { mutableStateOf("") }
    var mailNuevo by rememberSaveable { mutableStateOf("") }
    // El usuario creado para otro que se está editando, y el que se está por borrar.
    var editando by remember { mutableStateOf<PerfilACargo?>(null) }
    var borrando by remember { mutableStateOf<PerfilACargo?>(null) }

    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is AmigosViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
            AmigosViewModel.Evento.LimpiarAmigo -> amigo = ""
            AmigosViewModel.Evento.LimpiarNuevoUsuario -> {
                usuarioNuevo = ""
                mailNuevo = ""
            }
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
                teclado = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
            )
            CampoTexto(
                valor = mailNuevo,
                alCambiar = { mailNuevo = it },
                etiqueta = stringResource(R.string.hint_mail_amigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mailCrear"),
                teclado = TECLADO_MAIL,
                acciones = KeyboardActions(onDone = { viewModel.crearUsuario(usuarioNuevo, mailNuevo) }),
            )
            BotonPrincipal(
                texto = stringResource(R.string.accion_crear_usuario),
                alTocar = { viewModel.crearUsuario(usuarioNuevo, mailNuevo) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btnCrearUsuario"),
                icono = R.drawable.ic_group_add,
                cargando = cargando,
            )
        }
        if (perfilesACargo.isNotEmpty()) {
            Tarjeta {
                TituloSeccion(
                    stringResource(R.string.seccion_perfiles_a_cargo),
                    stringResource(R.string.seccion_perfiles_a_cargo_detalle),
                )
                perfilesACargo.forEach { perfil ->
                    FilaPerfilACargo(perfil, alEditar = { editando = perfil }, habilitado = !cargando)
                }
            }
        }
    }

    editando?.let { perfil ->
        DialogoPerfilACargo(
            perfil = perfil,
            alGuardar = { nombre, mail -> viewModel.guardarPerfilACargo(perfil, nombre, mail) },
            alBorrar = { borrando = perfil },
            alCerrar = { editando = null },
        )
    }
    borrando?.let { perfil ->
        DialogoConfirmacion(
            mensaje = stringResource(R.string.dialogo_borrar_usuario, perfil.jugador.nombre),
            alConfirmar = { viewModel.borrarPerfilACargo(perfil) },
            alCerrar = { borrando = null },
        )
    }
}

/** Un usuario creado para otro: su nombre, el mail para el que está reservado y cómo editarlo. */
@Composable
private fun FilaPerfilACargo(perfil: PerfilACargo, alEditar: () -> Unit, habilitado: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(perfil.jugador.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                perfil.mail ?: stringResource(R.string.perfil_a_cargo_sin_mail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(
            onClick = alEditar,
            modifier = Modifier.testTag("btnEditar_${perfil.jugador.id}"),
            enabled = habilitado,
        ) {
            Text(stringResource(R.string.accion_editar), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Cambiar el nombre o el mail de un usuario creado para otro, o borrarlo. */
@Composable
private fun DialogoPerfilACargo(
    perfil: PerfilACargo,
    alGuardar: (nombre: String, mail: String) -> Unit,
    alBorrar: () -> Unit,
    alCerrar: () -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf(perfil.jugador.nombre) }
    var mail by rememberSaveable { mutableStateOf(perfil.mail.orEmpty()) }
    val guardar = {
        alCerrar()
        alGuardar(nombre, mail)
    }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.dialogo_usuario_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = nombre,
                    alCambiar = { nombre = it },
                    etiqueta = stringResource(R.string.hint_usuario),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nombreACargo"),
                    teclado = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                )
                CampoTexto(
                    valor = mail,
                    alCambiar = { mail = it },
                    etiqueta = stringResource(R.string.hint_mail),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mailACargo"),
                    teclado = TECLADO_MAIL,
                    acciones = KeyboardActions(onDone = { guardar() }),
                )
                TextButton(
                    onClick = {
                        alCerrar()
                        alBorrar()
                    },
                    modifier = Modifier.testTag("btnBorrarUsuario"),
                ) {
                    Text(stringResource(R.string.accion_borrar_usuario), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = guardar, modifier = Modifier.testTag("btnGuardarUsuario")) {
                Text(stringResource(R.string.accion_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) }
        },
    )
}

private val TECLADO_MAIL = KeyboardOptions(
    keyboardType = KeyboardType.Email,
    autoCorrectEnabled = false,
    imeAction = ImeAction.Done,
)
