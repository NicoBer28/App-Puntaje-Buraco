package com.example.puntajeburaco20.ui.configuracion

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.LimitePuntos
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.ui.common.CampoTexto
import com.example.puntajeburaco20.ui.common.Encabezado
import com.example.puntajeburaco20.ui.common.PantallaBuraco
import com.example.puntajeburaco20.ui.common.RecolectarEventos
import com.example.puntajeburaco20.ui.common.SelectorSegmentado
import com.example.puntajeburaco20.ui.common.Tarjeta
import com.example.puntajeburaco20.ui.common.TituloSeccion
import com.example.puntajeburaco20.ui.common.rememberMensajero
import com.example.puntajeburaco20.ui.tema.numerico

/** Ajustes de la app. Cada cambio se guarda en el momento, en este dispositivo. */
@Composable
fun ConfiguracionScreen(
    alVolver: () -> Unit,
    viewModel: ConfiguracionViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()
    val foco = LocalFocusManager.current
    // Dónde quedaron la pantalla y el campo del límite, para saber si un toque cayó fuera del campo.
    val pantalla = remember { Ref<LayoutCoordinates>() }
    val campoLimite = remember { Ref<LayoutCoordinates>() }

    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is ConfiguracionViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
        }
    }

    PantallaBuraco(
        mensajero = mensajero,
        modifier = Modifier
            .onGloballyPositioned { pantalla.value = it }
            // Tocar cualquier cosa que no sea el campo del límite lo suelta y cierra el teclado.
            // Se mira antes de que el toque llegue a los controles, sin quitárselo.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val toque = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    // Si no se levantó el dedo en el lugar fue un desplazamiento: el campo sigue elegido.
                    waitForUpOrCancellation(PointerEventPass.Initial) ?: return@awaitEachGesture
                    val campo = campoLimite.value?.takeIf { it.isAttached }
                    val enElCampo = campo != null &&
                        pantalla.value?.localBoundingBoxOf(campo)?.contains(toque.position) == true
                    if (!enElCampo) foco.clearFocus()
                }
            },
        encabezado = {
            Encabezado(
                titulo = stringResource(R.string.opcion_configuracion),
                subtitulo = stringResource(R.string.subtitulo_configuracion),
                alVolver = alVolver,
            )
        },
    ) {
        // Hasta leer lo guardado no se dibuja nada: así ningún control arranca en un valor y salta a otro.
        estado?.let { actual ->
            Tarjeta {
                TituloSeccion(
                    stringResource(R.string.seccion_apariencia),
                    stringResource(R.string.seccion_apariencia_detalle),
                )
                SelectorSegmentado(
                    // El orden de R.array.modos_tema coincide con el de ModoTema.
                    opciones = stringArrayResource(R.array.modos_tema).asList(),
                    elegida = actual.modoTema.ordinal,
                    alElegir = { viewModel.cambiarTema(ModoTema.entries[it]) },
                    tag = { "tema_${ModoTema.entries[it].name}" },
                )
            }
            Tarjeta {
                TituloSeccion(stringResource(R.string.seccion_partida))
                FilaInterruptor(
                    titulo = stringResource(R.string.opcion_limite),
                    detalle = stringResource(R.string.opcion_limite_detalle),
                    activo = actual.limite.activo,
                    alCambiar = viewModel::activarLimite,
                    tag = "interruptorLimite",
                )
                CampoLimite(
                    guardado = actual.limite.puntos,
                    alCambiar = viewModel::cambiarLimite,
                    modifier = Modifier.onGloballyPositioned { campoLimite.value = it },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FilaInterruptor(
                    titulo = stringResource(R.string.opcion_pantalla_encendida),
                    detalle = stringResource(R.string.opcion_pantalla_encendida_detalle),
                    activo = actual.pantallaEncendida,
                    alCambiar = viewModel::cambiarPantallaEncendida,
                    tag = "interruptorPantalla",
                )
            }
        }
    }
}

/**
 * A cuántos puntos se juega. Lo que se escribe se guarda apenas es un valor que sirve; mientras
 * no lo sea sigue rigiendo el [guardado], que vuelve al campo al salir de él.
 */
@Composable
private fun CampoLimite(guardado: Int, alCambiar: (Int) -> Unit, modifier: Modifier = Modifier) {
    var texto by rememberSaveable { mutableStateOf(guardado.toString()) }
    val sirve = texto.toIntOrNull() in LimitePuntos.RANGO
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CampoTexto(
            valor = texto,
            alCambiar = { escrito ->
                texto = escrito.filter { it in '0'..'9' }.take(CIFRAS_DEL_LIMITE)
                texto.toIntOrNull()?.let(alCambiar)
            },
            etiqueta = stringResource(R.string.hint_limite_puntos),
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged { if (!it.isFocused && !sirve) texto = guardado.toString() }
                .testTag("limitePuntos"),
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            estiloTexto = MaterialTheme.typography.titleLarge.numerico,
        )
        Text(
            stringResource(R.string.limite_rango, LimitePuntos.RANGO.first, LimitePuntos.RANGO.last),
            style = MaterialTheme.typography.bodySmall,
            color = if (sirve) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
    }
}

private val CIFRAS_DEL_LIMITE = LimitePuntos.RANGO.last.toString().length

/** Un ajuste que está activo o no: toda la fila responde al toque, no solo el interruptor. */
@Composable
private fun FilaInterruptor(
    titulo: String,
    detalle: String,
    activo: Boolean,
    alCambiar: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = activo, role = Role.Switch, onValueChange = alCambiar)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // El toque lo maneja la fila: así los lectores de pantalla la anuncian como un solo control.
        Switch(checked = activo, onCheckedChange = null)
    }
}
