package com.example.puntajeburaco20.ui.perfil

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.ui.common.Avatar
import com.example.puntajeburaco20.ui.common.BotonSecundario
import com.example.puntajeburaco20.ui.common.DialogoConfirmacion
import com.example.puntajeburaco20.ui.common.Encabezado
import com.example.puntajeburaco20.ui.common.PantallaBuraco
import com.example.puntajeburaco20.ui.common.RecolectarEventos
import com.example.puntajeburaco20.ui.common.SelectorSegmentado
import com.example.puntajeburaco20.ui.common.Tarjeta
import com.example.puntajeburaco20.ui.common.TituloSeccion
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.rememberMensajero
import com.example.puntajeburaco20.ui.tema.TemaBuraco

@Composable
fun PerfilScreen(
    alVolver: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()
    var confirmarCierre by remember { mutableStateOf(false) }

    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is PerfilViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
        }
    }

    PantallaBuraco(
        mensajero = mensajero,
        encabezado = {
            Encabezado(
                titulo = stringResource(R.string.titulo_perfil),
                subtitulo = stringResource(R.string.subtitulo_perfil),
                alVolver = alVolver,
            ) {
                Identidad(estado.nombreUsuario, estado.cantidadAmigos)
            }
        },
    ) {
        Tarjeta {
            TituloSeccion(
                stringResource(R.string.seccion_apariencia),
                stringResource(R.string.seccion_apariencia_detalle),
            )
            SelectorSegmentado(
                // El orden de R.array.modos_tema coincide con el de ModoTema.
                opciones = stringArrayResource(R.array.modos_tema).asList(),
                elegida = estado.modoTema.ordinal,
                alElegir = { viewModel.cambiarTema(ModoTema.entries[it]) },
                tag = { "tema_${ModoTema.entries[it].name}" },
            )
        }
        // Estas opciones todavía no tienen pantalla: se muestran marcadas como "Pronto" y avisan al tocarlas.
        GrupoOpciones(stringResource(R.string.seccion_cuenta)) {
            OpcionProximamente(
                titulo = R.string.opcion_cambiar_contrasena,
                detalle = R.string.opcion_cambiar_contrasena_detalle,
                icono = R.drawable.ic_lock,
                tag = "opcionCambiarContrasena",
                avisar = mensajero::mostrar,
            )
        }
        GrupoOpciones(stringResource(R.string.seccion_aplicacion)) {
            OpcionProximamente(
                titulo = R.string.opcion_configuracion,
                detalle = R.string.opcion_configuracion_detalle,
                icono = R.drawable.ic_settings,
                tag = "opcionConfiguracion",
                avisar = mensajero::mostrar,
            )
            HorizontalDivider(
                Modifier.padding(start = 74.dp, end = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            OpcionProximamente(
                titulo = R.string.opcion_ayuda,
                detalle = R.string.opcion_ayuda_detalle,
                icono = R.drawable.ic_help,
                tag = "opcionAyuda",
                avisar = mensajero::mostrar,
            )
        }
        BotonSecundario(
            texto = stringResource(R.string.accion_cerrar_sesion),
            alTocar = { confirmarCierre = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btnCerrarSesion"),
            icono = R.drawable.ic_logout,
            habilitado = !estado.cerrandoSesion,
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (confirmarCierre) {
        DialogoConfirmacion(
            mensaje = R.string.dialogo_cerrar_sesion,
            alConfirmar = viewModel::cerrarSesion,
            alCerrar = { confirmarCierre = false },
        )
    }
}

/** Quién tiene la sesión iniciada, dentro del encabezado. */
@Composable
private fun Identidad(nombre: String, cantidadAmigos: Int) {
    val colores = TemaBuraco.colores
    Row(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            nombre = nombre.ifEmpty { null },
            color = colores.equipoUnoEnEncabezado,
            fondoVacio = Color.White.copy(alpha = 0.12f),
            tamano = 52.dp,
            estiloInicial = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                nombre,
                modifier = Modifier.testTag("nombrePerfil"),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                pluralStringResource(R.plurals.perfil_amigos, cantidadAmigos, cantidadAmigos),
                style = MaterialTheme.typography.bodySmall,
                color = colores.sobreEncabezadoSuave,
            )
        }
    }
}

/** Tarjeta con un título y, debajo, filas tocables que llegan hasta sus bordes. */
@Composable
private fun GrupoOpciones(titulo: String, opciones: @Composable ColumnScope.() -> Unit) {
    Tarjeta(relleno = PaddingValues(top = 20.dp, bottom = 8.dp)) {
        Column {
            Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)) { TituloSeccion(titulo) }
            opciones()
        }
    }
}

/** Opción que todavía no está implementada: al tocarla avisa que va a estar disponible más adelante. */
@Composable
private fun OpcionProximamente(
    @StringRes titulo: Int,
    @StringRes detalle: Int,
    @DrawableRes icono: Int,
    tag: String,
    avisar: (UiText) -> Unit,
) {
    val nombre = stringResource(titulo)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { avisar(UiText.de(R.string.mensaje_proximamente, nombre)) }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icono),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(detalle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(R.string.etiqueta_proximamente),
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
