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
    // El usuario al que se le está cargando o corrigiendo el mail.
    var editando by remember { mutableStateOf<PerfilACargo?>(null) }

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
                    FilaPerfilACargo(perfil, alCambiarMail = { editando = perfil }, habilitado = !cargando)
                }
            }
        }
    }

    editando?.let { perfil ->
        DialogoMail(
            perfil = perfil,
            alGuardar = { viewModel.cambiarMail(perfil, it) },
            alCerrar = { editando = null },
        )
    }
}

/** Un usuario creado para otro: su nombre, el mail para el que está reservado y cómo cambiarlo. */
@Composable
private fun FilaPerfilACargo(perfil: PerfilACargo, alCambiarMail: () -> Unit, habilitado: Boolean) {
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
            onClick = alCambiarMail,
            modifier = Modifier.testTag("btnMail_${perfil.jugador.id}"),
            enabled = habilitado,
        ) {
            Text(
                stringResource(if (perfil.mail == null) R.string.accion_cargar_mail else R.string.accion_cambiar_mail),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun DialogoMail(perfil: PerfilACargo, alGuardar: (mail: String) -> Unit, alCerrar: () -> Unit) {
    var mail by rememberSaveable { mutableStateOf(perfil.mail.orEmpty()) }
    val guardar = {
        alCerrar()
        alGuardar(mail)
    }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.dialogo_mail_titulo, perfil.jugador.nombre)) },
        text = {
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
        },
        confirmButton = {
            TextButton(onClick = guardar, modifier = Modifier.testTag("btnGuardarMail")) {
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
