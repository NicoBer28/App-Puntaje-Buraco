package com.example.puntajeburaco20.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.ui.tema.TemaBuraco
import com.example.puntajeburaco20.ui.tema.numerico

/**
 * Planilla con los puntos de cada ronda, como la que se anota en papel. Debajo de cada total va
 * el desglose "base + puntos". Con [conTotal] agrega una última fila con el total de cada equipo.
 */
@Composable
fun TablaRondas(
    partida: Partida,
    modifier: Modifier = Modifier,
    conTotal: Boolean = false,
) {
    val colores = TemaBuraco.colores
    Column(modifier.fillMaxWidth()) {
        Fila(fondo = Color.Transparent) {
            Celda("#", Modifier.width(AnchoPrimeraColumna), estilo = Estilo.ENCABEZADO)
            Text(
                partida.equipoUno.nombreVisible(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = colores.equipoUno,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                partida.equipoDos.nombreVisible(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = colores.equipoDos,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        partida.rondas.forEachIndexed { i, ronda ->
            Fila(fondo = if (i % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent) {
                Celda((i + 1).toString(), Modifier.width(AnchoPrimeraColumna), estilo = Estilo.ENCABEZADO)
                CeldaPuntaje(ronda.equipoUno, Modifier.weight(1f))
                CeldaPuntaje(ronda.equipoDos, Modifier.weight(1f))
            }
        }
        if (conTotal) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Fila(fondo = Color.Transparent) {
                Text(
                    stringResource(R.string.detalle_total),
                    modifier = Modifier.width(AnchoPrimeraColumna),
                    style = MaterialTheme.typography.titleSmall,
                )
                Celda(partida.totalUno.toString(), Modifier.weight(1f), estilo = Estilo.TOTAL)
                Celda(partida.totalDos.toString(), Modifier.weight(1f), estilo = Estilo.TOTAL)
            }
        }
    }
}

private val AnchoPrimeraColumna = 56.dp

private enum class Estilo { ENCABEZADO, TOTAL }

@Composable
private fun Fila(fondo: Color, contenido: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(fondo)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = contenido,
    )
}

@Composable
private fun Celda(texto: String, modifier: Modifier, estilo: Estilo) {
    when (estilo) {
        Estilo.ENCABEZADO -> Text(
            texto,
            modifier = modifier,
            style = MaterialTheme.typography.labelMedium.numerico,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Estilo.TOTAL -> Text(
            texto,
            modifier = modifier,
            style = MaterialTheme.typography.headlineSmall.numerico,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CeldaPuntaje(puntaje: PuntajeRonda, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(puntaje.total.toString(), style = MaterialTheme.typography.titleMedium.numerico)
        Text(
            if (puntaje.puntos < 0) {
                stringResource(R.string.detalle_desglose_resta, puntaje.base, -puntaje.puntos)
            } else {
                stringResource(R.string.detalle_desglose, puntaje.base, puntaje.puntos)
            },
            style = MaterialTheme.typography.bodySmall.numerico,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
