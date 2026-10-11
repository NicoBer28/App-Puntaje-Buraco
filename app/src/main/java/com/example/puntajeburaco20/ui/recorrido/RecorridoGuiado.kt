package com.example.puntajeburaco20.ui.recorrido

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Recorrido
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Un paso de un recorrido: el elemento que se ilumina y lo que se explica de él. */
data class PasoRecorrido(
    /** Id con el que el elemento se marcó como [destacable]. */
    val destino: String,
    @StringRes val titulo: Int,
    @StringRes val detalle: Int,
)

/** Dónde está cada elemento [destacable] de la pantalla y cuál se está explicando. */
@Stable
private class EstadoRecorrido {
    /** Parte visible de cada elemento, en coordenadas de la raíz. */
    val limites = mutableStateMapOf<String, Rect>()
    var destinoActivo by mutableStateOf<String?>(null)
}

private val LocalRecorrido = compositionLocalOf<EstadoRecorrido?> { null }

/**
 * Marca un elemento para que un paso de [RecorridoGuiado] lo pueda iluminar. Cuando le toca, se
 * trae a la vista si quedó fuera de la parte visible. Fuera de un recorrido no hace nada.
 */
fun Modifier.destacable(id: String): Modifier = composed {
    val estado = LocalRecorrido.current ?: return@composed this
    val solicitante = remember { BringIntoViewRequester() }
    val activo = estado.destinoActivo == id
    LaunchedEffect(activo) { if (activo) solicitante.bringIntoView() }
    DisposableEffect(estado, id) { onDispose { estado.limites.remove(id) } }
    bringIntoViewRequester(solicitante).onGloballyPositioned { coordenadas ->
        val limites = coordenadas.boundsInRoot()
        if (estado.limites[id] != limites) estado.limites[id] = limites
    }
}

/**
 * Envuelve una pantalla y, mientras el [recorrido] no se haya visto en este dispositivo, la
 * oscurece e ilumina de a uno los elementos de los [pasos], con un globo que explica cada uno.
 * Mientras dura, la pantalla de abajo no reacciona a los toques.
 *
 * @param habilitado `false` mientras la pantalla no está en condiciones de mostrarlo.
 */
@Composable
fun RecorridoGuiado(
    recorrido: Recorrido,
    pasos: List<PasoRecorrido>,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    viewModel: RecorridoViewModel = hiltViewModel(),
    contenido: @Composable () -> Unit,
) {
    val pendientes by viewModel.pendientes.collectAsStateWithLifecycle()
    val estado = remember { EstadoRecorrido() }
    // Se cierra en el momento, sin esperar a que quede guardado como visto.
    var cerrado by remember(recorrido) { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalRecorrido provides estado, content = contenido)
        if (habilitado && !cerrado && pendientes?.contains(recorrido) == true) {
            CapaRecorrido(
                pasos = pasos,
                estado = estado,
                alTerminar = {
                    cerrado = true
                    viewModel.terminar(recorrido)
                },
            )
        }
    }
}

@Composable
private fun CapaRecorrido(pasos: List<PasoRecorrido>, estado: EstadoRecorrido, alTerminar: () -> Unit) {
    var indice by rememberSaveable { mutableIntStateOf(0) }
    // Arranca cuando terminó la transición de entrada: recién ahí cada elemento está en su lugar.
    var listo by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(DEMORA_INICIO_MS)
        listo = true
    }
    // Los pasos cuyo elemento no está en la pantalla se saltean (la cámara, si el teléfono no tiene).
    val visibles by remember(pasos) { derivedStateOf { pasos.filter { it.destino in estado.limites } } }
    if (!listo || visibles.isEmpty()) return

    val posicion = indice.coerceIn(visibles.indices)
    val paso = visibles[posicion]
    DisposableEffect(paso.destino) {
        estado.destinoActivo = paso.destino
        onDispose { estado.destinoActivo = null }
    }
    // Atrás sale del recorrido en lugar de salir de la pantalla.
    BackHandler(onBack = alTerminar)

    var origen by remember { mutableStateOf(Offset.Zero) }
    val objetivo = estado.limites[paso.destino]?.translate(-origen) ?: return
    val hueco = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    LaunchedEffect(objetivo) {
        when {
            // El elemento quedó fuera de la parte visible: se espera a que se lo traiga.
            objetivo.isEmpty -> Unit
            // El primero aparece en su lugar; de ahí en más la luz se desliza de uno a otro.
            hueco.value.isEmpty -> hueco.snapTo(objetivo)
            else -> hueco.animateTo(objetivo)
        }
    }
    val hayHueco by remember { derivedStateOf { !hueco.value.isEmpty } }
    val aparicion = remember { Animatable(0f) }
    LaunchedEffect(Unit) { aparicion.animateTo(1f) }
    val barras = WindowInsets.safeDrawing

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origen = it.positionInRoot() }
            .graphicsLayer { alpha = aparicion.value }
            // Recibe todos los toques: lo que está debajo no reacciona.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                // Sin una capa propia, el hueco borraría también la pantalla de abajo.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(ColorVelo)
            if (!hayHueco) return@Canvas
            val iluminado = hueco.value.inflate(MargenHueco.toPx())
            val esquina = CornerRadius(RadioHueco.toPx())
            drawRoundRect(Color.Black, iluminado.topLeft, iluminado.size, esquina, blendMode = BlendMode.Clear)
            drawRoundRect(ColorBorde, iluminado.topLeft, iluminado.size, esquina, style = Stroke(2.dp.toPx()))
        }
        Layout(
            content = {
                Globo(
                    paso = paso,
                    posicion = posicion,
                    cantidad = visibles.size,
                    alRetroceder = { indice = posicion - 1 },
                    alAvanzar = { indice = posicion + 1 },
                    alTerminar = alTerminar,
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { medibles, restricciones ->
            val margen = MargenGlobo.toPx()
            val area = Rect(
                left = barras.getLeft(this, layoutDirection) + margen,
                top = barras.getTop(this) + margen,
                right = restricciones.maxWidth - barras.getRight(this, layoutDirection) - margen,
                bottom = restricciones.maxHeight - barras.getBottom(this) - margen,
            )
            val globo = medibles.single().measure(
                Constraints(
                    maxWidth = minOf(area.width, AnchoMaximoGlobo.toPx()).roundToInt().coerceAtLeast(0),
                    maxHeight = area.height.roundToInt().coerceAtLeast(0),
                ),
            )
            layout(restricciones.maxWidth, restricciones.maxHeight) {
                val tamano = Size(globo.width.toFloat(), globo.height.toFloat())
                val lugar = if (hayHueco) {
                    ubicarGlobo(area, hueco.value.inflate(MargenHueco.toPx()), tamano, SeparacionGlobo.toPx())
                } else {
                    // Sin nada iluminado todavía, el globo queda en el medio: siempre se puede omitir.
                    area.center - Offset(tamano.width / 2, tamano.height / 2)
                }
                globo.place(lugar.x.roundToInt(), lugar.y.roundToInt())
            }
        }
    }
}

/** La explicación del paso, con los controles para moverse por el recorrido o salir de él. */
@Composable
private fun Globo(
    paso: PasoRecorrido,
    posicion: Int,
    cantidad: Int,
    alRetroceder: () -> Unit,
    alAvanzar: () -> Unit,
    alTerminar: () -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val esElUltimo = posicion == cantidad - 1
    Surface(
        modifier = Modifier.testTag("recorridoGlobo"),
        shape = MaterialTheme.shapes.large,
        color = esquema.surface,
        border = BorderStroke(1.dp, esquema.outlineVariant),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(top = 18.dp, bottom = 6.dp)) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    // Los lectores de pantalla anuncian cada paso al llegar.
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(paso.titulo), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(paso.detalle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = esquema.onSurfaceVariant,
                )
            }
            // Los controles van acá y no al pie de la pantalla, donde taparían los botones que
            // algunos pasos iluminan.
            Row(
                Modifier.padding(start = 8.dp, top = 6.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (!esElUltimo) {
                        TextButton(onClick = alTerminar, modifier = Modifier.testTag("recorridoOmitir")) {
                            Text(stringResource(R.string.accion_omitir))
                        }
                    }
                }
                Progreso(posicion, cantidad)
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = alRetroceder,
                        enabled = posicion > 0,
                        modifier = Modifier.testTag("recorridoAnterior"),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_paso_anterior),
                        )
                    }
                    if (esElUltimo) {
                        TextButton(onClick = alTerminar, modifier = Modifier.testTag("recorridoListo")) {
                            Text(stringResource(R.string.accion_listo))
                        }
                    } else {
                        IconButton(onClick = alAvanzar, modifier = Modifier.testTag("recorridoSiguiente")) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_forward),
                                contentDescription = stringResource(R.string.cd_paso_siguiente),
                                tint = esquema.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Un circulito por paso, con el actual resaltado. */
@Composable
private fun Progreso(posicion: Int, cantidad: Int) {
    val esquema = MaterialTheme.colorScheme
    val descripcion = stringResource(R.string.recorrido_progreso, posicion + 1, cantidad)
    Row(
        Modifier.clearAndSetSemantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(cantidad) { i ->
            Box(
                Modifier
                    .size(7.dp)
                    .background(if (i == posicion) esquema.primary else esquema.outlineVariant, CircleShape),
            )
        }
    }
}

private const val DEMORA_INICIO_MS = 450L

private val ColorVelo = Color.Black.copy(alpha = 0.72f)
private val ColorBorde = Color.White.copy(alpha = 0.85f)

/** Aire entre el elemento y el borde de la zona iluminada. */
private val MargenHueco = 6.dp
private val RadioHueco = 16.dp

/** Distancia del globo a los bordes de la pantalla y a la zona iluminada. */
private val MargenGlobo = 16.dp
private val SeparacionGlobo = 12.dp
private val AnchoMaximoGlobo = 400.dp
