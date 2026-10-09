package com.example.puntajeburaco20.ui.historial

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.ModoJuego
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.CasillaJugador
import com.example.puntajeburaco20.ui.common.Encabezado
import com.example.puntajeburaco20.ui.common.EtiquetaEquipo
import com.example.puntajeburaco20.ui.common.FilaAtajo
import com.example.puntajeburaco20.ui.common.InsigniaVersus
import com.example.puntajeburaco20.ui.common.PantallaBuraco
import com.example.puntajeburaco20.ui.common.RecolectarEventos
import com.example.puntajeburaco20.ui.common.SeleccionJugadores
import com.example.puntajeburaco20.ui.common.SelectorModo
import com.example.puntajeburaco20.ui.common.Tarjeta
import com.example.puntajeburaco20.ui.common.rememberMensajero
import com.example.puntajeburaco20.ui.tema.TemaBuraco
import com.example.puntajeburaco20.ui.tema.numerico
import kotlin.math.roundToInt

@Composable
fun HistorialScreen(
    alVolver: () -> Unit,
    irAMisPartidas: () -> Unit,
    viewModel: HistorialViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()

    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is HistorialViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
        }
    }

    val seleccion = estado.seleccion
    // Posiciones según HistorialViewModel: 0 y 2 son el equipo, 1 y 3 el rival.
    val (posicionesEquipo, posicionesRival) = when (seleccion.modo) {
        ModoJuego.INDIVIDUAL -> listOf(0) to listOf(1)
        ModoJuego.PAREJAS -> listOf(0, 2) to listOf(1, 3)
    }

    PantallaBuraco(
        mensajero = mensajero,
        encabezado = {
            Encabezado(
                titulo = stringResource(R.string.titulo_estadisticas),
                subtitulo = stringResource(R.string.subtitulo_estadisticas),
                alVolver = alVolver,
            )
        },
    ) {
        FilaAtajo(
            titulo = stringResource(R.string.accion_mis_partidas),
            detalle = stringResource(R.string.accion_mis_partidas_detalle),
            icono = R.drawable.ic_history,
            alTocar = irAMisPartidas,
            modifier = Modifier.testTag("btnMisPartidas"),
        )
        SelectorModo(modo = seleccion.modo, alCambiar = viewModel::cambiarModo)
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GrupoJugadores(
                    titulo = stringResource(R.string.etiqueta_equipo),
                    posiciones = posicionesEquipo,
                    seleccion = seleccion,
                    textoVacio = stringResource(R.string.placeholder_jugador),
                    textoQuitar = stringResource(R.string.quitar_jugador),
                    color = TemaBuraco.colores.equipoUno,
                    colorSuave = TemaBuraco.colores.equipoUnoSuave,
                    alElegir = viewModel::elegirJugador,
                )
                GrupoJugadores(
                    titulo = stringResource(R.string.etiqueta_rival),
                    posiciones = posicionesRival,
                    seleccion = seleccion,
                    textoVacio = stringResource(R.string.placeholder_historial),
                    textoQuitar = stringResource(R.string.quitar_rival),
                    color = TemaBuraco.colores.equipoDos,
                    colorSuave = TemaBuraco.colores.equipoDosSuave,
                    alElegir = viewModel::elegirJugador,
                )
            }
            InsigniaVersus(Modifier.align(Alignment.Center))
        }
        BotonPrincipal(
            texto = stringResource(R.string.accion_buscar),
            alTocar = viewModel::buscar,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btnBuscar"),
            cargando = estado.buscando,
        )

        val hayResultados = estado.equipoUno.jugadas > 0 || estado.equipoDos.jugadas > 0
        if (!hayResultados) {
            SinResultados()
        } else {
            val hayRival = posicionesRival.any { seleccion.elegido(it) != null }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PanelEstadisticas(
                    nombre = nombres(seleccion, posicionesEquipo),
                    estadisticas = estado.equipoUno,
                    color = TemaBuraco.colores.equipoUno,
                    sufijo = "Uno",
                    modifier = Modifier.weight(1f),
                )
                if (hayRival) {
                    PanelEstadisticas(
                        nombre = nombres(seleccion, posicionesRival),
                        estadisticas = estado.equipoDos,
                        color = TemaBuraco.colores.equipoDos,
                        sufijo = "Dos",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GrupoJugadores(
    titulo: String,
    posiciones: List<Int>,
    seleccion: SeleccionJugadores,
    textoVacio: String,
    textoQuitar: String,
    color: Color,
    colorSuave: Color,
    alElegir: (Int, Jugador?) -> Unit,
) {
    val etiquetas = listOf(
        R.string.etiqueta_jugador_1,
        R.string.etiqueta_jugador_2,
    )
    Tarjeta {
        EtiquetaEquipo(titulo, color)
        posiciones.forEachIndexed { i, posicion ->
            CasillaJugador(
                etiqueta = stringResource(etiquetas[i]),
                elegido = seleccion.elegido(posicion),
                opciones = seleccion.opcionesPara(posicion),
                textoVacio = textoVacio,
                textoQuitar = textoQuitar,
                color = color,
                colorSuave = colorSuave,
                alElegir = { alElegir(posicion, it) },
                modifier = Modifier.testTag("jugador_$posicion"),
            )
        }
    }
}

@Composable
private fun nombres(seleccion: SeleccionJugadores, posiciones: List<Int>): String {
    val elegidos = posiciones.mapNotNull { seleccion.elegido(it)?.nombre }
    return when (elegidos.size) {
        2 -> stringResource(R.string.equipo_pareja, elegidos[0], elegidos[1])
        else -> elegidos.firstOrNull().orEmpty()
    }
}

@Composable
private fun SinResultados() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_bar_chart),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(R.string.estadisticas_vacias),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Porcentaje de partidas ganadas, con una barra, y el detalle de jugadas, ganadas y perdidas. */
@Composable
private fun PanelEstadisticas(
    nombre: String,
    estadisticas: Estadisticas,
    color: Color,
    sufijo: String,
    modifier: Modifier = Modifier,
) {
    val porcentaje = if (estadisticas.jugadas == 0L) 0f else estadisticas.ganadas.toFloat() / estadisticas.jugadas
    val progreso by animateFloatAsState(porcentaje, label = "porcentajeGanadas")
    Tarjeta(modifier) {
        Text(
            nombre,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column {
            Text(
                stringResource(R.string.porcentaje, (porcentaje * 100).roundToInt()),
                style = MaterialTheme.typography.displaySmall.numerico,
            )
            Text(
                stringResource(R.string.porcentaje_detalle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progreso)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Dato(stringResource(R.string.etiqueta_partidas_jugadas), estadisticas.jugadas, "jug$sufijo")
        Dato(stringResource(R.string.etiqueta_partidas_ganadas), estadisticas.ganadas, "gan$sufijo")
        Dato(stringResource(R.string.etiqueta_partidas_perdidas), estadisticas.perdidas, "perd$sufijo")
    }
}

@Composable
private fun Dato(etiqueta: String, valor: Long, tag: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            etiqueta,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            valor.toString(),
            modifier = Modifier.testTag(tag),
            style = MaterialTheme.typography.titleMedium.numerico,
        )
    }
}
