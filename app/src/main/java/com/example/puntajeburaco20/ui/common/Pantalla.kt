package com.example.puntajeburaco20.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.ui.tema.TemaBuraco

/**
 * Estructura común de las pantallas: un [encabezado] azul arriba, el [contenido] desplazable
 * debajo y, si hay, un [pie] fijo con la acción principal. Se encarga de los márgenes de las
 * barras del sistema y del teclado.
 */
@Composable
fun PantallaBuraco(
    mensajero: Mensajero,
    encabezado: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pie: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val desplazamiento = rememberScrollState()
        val desplazado by remember { derivedStateOf { desplazamiento.value > 0 } }
        Column(
            Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(desplazamiento),
            ) {
                encabezado()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = contenido,
                )
                if (pie == null) Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            if (pie != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(
                        Modifier
                            .navigationBarsPadding()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) { pie() }
                }
            }
        }
        // Al desplazar, el contenido no se ve detrás de la barra de estado (sus íconos son blancos).
        if (desplazado) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(TemaBuraco.colores.encabezadoArriba),
            )
        }
        SnackbarHost(
            hostState = mensajero.estado,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = if (pie != null) 84.dp else 8.dp),
        ) { datos ->
            Snackbar(
                snackbarData = datos,
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

/**
 * Bloque azul de arriba de cada pantalla, con esquinas inferiores redondeadas. Se dibuja detrás de
 * la barra de estado.
 */
@Composable
fun Encabezado(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    alVolver: (() -> Unit)? = null,
    navegacion: (@Composable RowScope.() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit = {},
) {
    val colores = TemaBuraco.colores
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(colores.encabezadoArriba, colores.encabezadoAbajo)))
            .drawBehind { dibujarFichasDeFondo(colores.sobreEncabezado.copy(alpha = 0.07f)) }
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 24.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides colores.sobreEncabezado) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    navegacion != null -> navegacion()
                    alVolver != null -> BotonVolver(alVolver)
                }
                Spacer(Modifier.weight(1f))
                acciones()
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(titulo, style = MaterialTheme.typography.headlineLarge)
                if (subtitulo != null) {
                    Text(
                        subtitulo,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.sobreEncabezadoSuave,
                    )
                }
                contenido()
            }
        }
    }
}

@Composable
private fun BotonVolver(alVolver: () -> Unit) {
    IconButton(onClick = alVolver, modifier = Modifier.testTag("btnVolver")) {
        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.accion_volver))
    }
}

/** Dos fichas grandes, apenas visibles, asomando por la esquina derecha del encabezado. */
private fun DrawScope.dibujarFichasDeFondo(color: Color) {
    val ancho = 92.dp.toPx()
    val alto = 124.dp.toPx()
    val trazo = Stroke(width = 2.dp.toPx())
    val esquina = CornerRadius(14.dp.toPx())
    rotate(degrees = 14f, pivot = Offset(size.width - ancho * 0.2f, alto * 0.3f)) {
        drawRoundRect(color, Offset(size.width - ancho * 0.55f, -alto * 0.25f), Size(ancho, alto), esquina, style = trazo)
    }
    rotate(degrees = -8f, pivot = Offset(size.width - ancho, alto * 0.5f)) {
        drawRoundRect(color, Offset(size.width - ancho * 1.35f, alto * 0.05f), Size(ancho, alto), esquina, style = trazo)
        drawCircle(color, radius = 9.dp.toPx(), center = Offset(size.width - ancho * 0.85f, alto * 0.82f), style = trazo)
    }
}
