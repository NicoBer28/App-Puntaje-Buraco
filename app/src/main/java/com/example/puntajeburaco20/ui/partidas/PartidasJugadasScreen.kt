package com.example.puntajeburaco20.ui.partidas

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.HistorialJugador
import com.example.puntajeburaco20.domain.model.PartidaJugada
import com.example.puntajeburaco20.ui.common.Encabezado
import com.example.puntajeburaco20.ui.common.PantallaBuraco
import com.example.puntajeburaco20.ui.common.RecolectarEventos
import com.example.puntajeburaco20.ui.common.TablaRondas
import com.example.puntajeburaco20.ui.common.nombreVisible
import com.example.puntajeburaco20.ui.common.rememberMensajero
import com.example.puntajeburaco20.ui.tema.TemaBuraco
import com.example.puntajeburaco20.ui.tema.numerico

@Composable
fun PartidasJugadasScreen(
    alVolver: () -> Unit,
    viewModel: PartidasJugadasViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()
    var detalle by remember { mutableStateOf<PartidaJugada?>(null) }

    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is PartidasJugadasViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
        }
    }

    val historial = estado.historial
    PantallaBuraco(
        mensajero = mensajero,
        encabezado = {
            Encabezado(
                titulo = stringResource(R.string.titulo_mis_partidas),
                subtitulo = if (historial != null && historial.jugadas == 0) {
                    stringResource(R.string.resumen_sin_partidas)
                } else {
                    null
                },
                alVolver = alVolver,
            ) {
                if (historial != null && historial.jugadas > 0) Resumen(historial)
            }
        },
    ) {
        if (estado.cargando && historial == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        historial?.partidas?.forEach { jugada ->
            TarjetaPartida(
                jugada = jugada,
                gano = jugada.gano(historial.jugador),
                alTocar = { detalle = jugada },
            )
        }
    }

    detalle?.let { DetallePartida(it) { detalle = null } }
}

/** Ganadas, racha y promedio, en tres columnas sobre el encabezado. */
@Composable
private fun Resumen(historial: HistorialJugador) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .height(IntrinsicSize.Min),
    ) {
        DatoResumen(
            stringResource(R.string.resumen_ganadas),
            stringResource(R.string.resumen_ganadas_valor, historial.ganadas, historial.jugadas),
            Modifier.weight(1f),
        )
        Separador()
        DatoResumen(stringResource(R.string.resumen_racha), historial.rachaActual.toString(), Modifier.weight(1f))
        Separador()
        DatoResumen(
            stringResource(R.string.resumen_promedio),
            historial.promedioPuntos?.toString().orEmpty(),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun Separador() {
    Box(
        Modifier
            .padding(vertical = 6.dp)
            .width(1.dp)
            .fillMaxHeight()
            .background(TemaBuraco.colores.sobreEncabezado.copy(alpha = 0.14f)),
    )
}

@Composable
private fun DatoResumen(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.displaySmall.numerico)
        Text(
            etiqueta.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TemaBuraco.colores.sobreEncabezadoSuave,
        )
    }
}

@Composable
private fun TarjetaPartida(jugada: PartidaJugada, gano: Boolean, alTocar: () -> Unit) {
    val partida = jugada.partida
    val colores = TemaBuraco.colores
    val fecha = DateUtils.formatDateTime(
        LocalContext.current,
        jugada.fecha,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
    )
    Surface(
        onClick = alTocar,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("partidaJugada"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (gano) colores.ganador else MaterialTheme.colorScheme.outlineVariant),
            )
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fecha,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    InsigniaResultado(gano)
                }
                Text(
                    stringResource(R.string.partida_equipos, partida.equipoUno.nombreVisible(), partida.equipoDos.nombreVisible()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(R.string.partida_resultado, partida.totalUno, partida.totalDos),
                        style = MaterialTheme.typography.headlineSmall.numerico,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        pluralStringResource(R.plurals.partida_rondas, partida.rondas.size, partida.rondas.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InsigniaResultado(gano: Boolean) {
    val colores = TemaBuraco.colores
    val (fondo, texto) = if (gano) {
        colores.ganadorSuave to colores.ganador
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (gano) {
            Icon(
                painterResource(R.drawable.ic_emoji_events),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = texto,
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            stringResource(if (gano) R.string.partida_ganada else R.string.partida_perdida),
            style = MaterialTheme.typography.labelMedium,
            color = texto,
        )
    }
}

/** Detalle de la partida: los puntos de cada ronda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetallePartida(jugada: PartidaJugada, alCerrar: () -> Unit) {
    val partida = jugada.partida
    ModalBottomSheet(
        onDismissRequest = alCerrar,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                stringResource(R.string.partida_equipos, partida.equipoUno.nombreVisible(), partida.equipoDos.nombreVisible()),
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(12.dp))
            if (partida.rondas.isEmpty()) {
                Text(
                    stringResource(R.string.detalle_sin_rondas),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TablaRondas(partida, conTotal = true)
        }
    }
}
