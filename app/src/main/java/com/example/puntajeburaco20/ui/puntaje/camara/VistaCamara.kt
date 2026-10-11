package com.example.puntajeburaco20.ui.puntaje.camara

import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.CajaNormalizada
import com.example.puntajeburaco20.domain.model.FichaDetectada
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.BotonSecundario
import com.example.puntajeburaco20.ui.common.SelectorSegmentado
import com.example.puntajeburaco20.ui.common.textoEncima
import com.example.puntajeburaco20.ui.puntaje.PuntajeViewModel.FotoFichas
import com.example.puntajeburaco20.ui.tema.TemaBuraco
import kotlin.math.roundToInt

private val ColorMarca = Color(0xFF7CC4FA)
private val ColorSeleccion = Color(0xFFFFC857)
private val ColorTextoMarca = Color(0xFF071A33)

/** Valores que se pueden elegir al corregir una ficha, en el orden del juego. */
private val Etiquetas = (1..13).map { it.toString() } + "C"

/**
 * Cámara a pantalla completa para contar fichas. Mientras está en pantalla la [camara] está
 * encendida; al salir de la composición se apaga.
 *
 * Tiene dos modos: la vista previa en vivo (con las [fichas] que se ven en cada cuadro) y, después
 * de sacar una [foto], la foto analizada por recortes, donde se puede corregir cada ficha tocándola
 * o agregar una que faltó tocando su lugar.
 */
@Composable
fun VistaCamara(
    camara: CamaraFichas,
    fichas: List<FichaDetectada>,
    foto: FotoFichas?,
    nombreUno: String,
    nombreDos: String,
    alSacarFoto: () -> Unit,
    alDescartarFoto: () -> Unit,
    alCorregir: (indice: Int, etiqueta: String?) -> Unit,
    alAgregar: (CajaNormalizada, String) -> Unit,
    alSumar: (lado: LadoEquipo, restar: Boolean) -> Unit,
    alCerrar: () -> Unit,
    alFallar: (Throwable) -> Unit,
) {
    val context = LocalContext.current
    val alFallarActual by rememberUpdatedState(alFallar)
    val vistaPrevia = remember {
        PreviewView(context).apply {
            // TextureView en lugar de SurfaceView: se recorta bien con las esquinas redondeadas.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(camara, vistaPrevia) {
        camara.iniciar(vistaPrevia) { alFallarActual(it) }
        onDispose { camara.detener() }
    }
    BackHandler(onBack = if (foto != null) alDescartarFoto else alCerrar)

    // Ficha elegida para corregir, o lugar tocado para agregar una. Se olvida con cada foto nueva.
    var seleccionada by remember(foto?.imagen) { mutableStateOf<Int?>(null) }
    var nueva by remember(foto?.imagen) { mutableStateOf<CajaNormalizada?>(null) }
    val fichasFoto = foto?.fichas
    // Si las fichas de la foto suman o descuentan. La elección se mantiene al sacar otra foto, y
    // vuelve a sumar cada vez que se abre la cámara. Lo que se ve en vivo, sin foto, siempre suma.
    var eligioRestar by remember { mutableStateOf(false) }
    val restan = foto != null && eligioRestar

    val colores = TemaBuraco.colores
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Los toques no llegan a la pantalla de puntaje que queda debajo.
            .pointerInput(Unit) {},
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            Encabezado(foto, alCerrar)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val descripcion = stringResource(R.string.cd_vista_camara)
                // 3:4, la misma proporción que los cuadros y las fotos de la cámara: así las cajas
                // caen donde corresponde, sin recortes.
                Box(
                    Modifier
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(28.dp))
                        .semantics { contentDescription = descripcion },
                ) {
                    AndroidView(factory = { vistaPrevia }, modifier = Modifier.fillMaxSize())
                    if (foto == null) {
                        MarcasFichas(fichas, Modifier.fillMaxSize())
                    } else {
                        FotoConMarcas(
                            foto = foto,
                            seleccionada = seleccionada,
                            nueva = nueva,
                            alTocarFicha = {
                                seleccionada = it
                                nueva = null
                            },
                            alTocarVacio = {
                                // Con algo ya elegido, tocar afuera solo lo suelta: para agregar
                                // una ficha hay que tocar de nuevo.
                                nueva = if (seleccionada == null && nueva == null) it else null
                                seleccionada = null
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (fichasFoto != null && (seleccionada != null || nueva != null)) {
                SelectorEtiqueta(
                    actual = seleccionada?.let { fichasFoto.getOrNull(it)?.etiqueta },
                    permiteQuitar = seleccionada != null,
                    alElegir = { etiqueta ->
                        val indice = seleccionada
                        val caja = nueva
                        when {
                            indice != null -> alCorregir(indice, etiqueta)
                            caja != null && etiqueta != null -> alAgregar(caja, etiqueta)
                        }
                        seleccionada = null
                        nueva = null
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            if (foto == null) {
                BotonPrincipal(
                    texto = stringResource(R.string.accion_sacar_foto),
                    alTocar = alSacarFoto,
                    icono = R.drawable.ic_photo_camera,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (foto != null) {
                SelectorSigno(restan, alCambiar = { eligioRestar = it })
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple(LadoEquipo.UNO, nombreUno, colores.equipoUno),
                    Triple(LadoEquipo.DOS, nombreDos, colores.equipoDos),
                ).forEach { (lado, nombre, color) ->
                    BotonPrincipal(
                        texto = stringResource(
                            if (restan) R.string.accion_restar_a_equipo else R.string.accion_sumar_a_equipo,
                            nombre,
                        ),
                        alTocar = { alSumar(lado, restan) },
                        modifier = Modifier.weight(1f),
                        habilitado = foto == null || !foto.analizando,
                        colores = ButtonDefaults.buttonColors(containerColor = color, contentColor = color.textoEncima()),
                    )
                }
            }
            if (foto != null) {
                Spacer(Modifier.height(8.dp))
                BotonSecundario(
                    texto = stringResource(R.string.accion_otra_foto),
                    alTocar = alDescartarFoto,
                    icono = R.drawable.ic_photo_camera,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Si las fichas de la foto suman (las que se bajaron) o descuentan (las que quedaron en la mano).
 * Como cada resultado se acumula en los puntos de la ronda, sirve para sacar una foto de cada cosa.
 */
@Composable
private fun SelectorSigno(restan: Boolean, alCambiar: (Boolean) -> Unit) {
    // La pantalla de la cámara es siempre negra: el control va con los colores del tema oscuro.
    TemaBuraco(oscuro = true) {
        SelectorSegmentado(
            // El orden de R.array.signos_fichas es: suman, restan.
            opciones = stringArrayResource(R.array.signos_fichas).asList(),
            elegida = if (restan) 1 else 0,
            alElegir = { alCambiar(it == 1) },
            tag = { if (it == 1) "fichasRestan" else "fichasSuman" },
        )
    }
}

@Composable
private fun Encabezado(foto: FotoFichas?, alCerrar: () -> Unit) {
    val fichas = foto?.fichas
    val titulo = when {
        foto == null -> stringResource(R.string.camara_titulo)
        fichas == null -> stringResource(R.string.camara_analizando)
        else -> stringResource(R.string.camara_resultado, fichas.size, foto.puntos)
    }
    val detalle = when {
        foto == null -> stringResource(R.string.camara_detalle)
        fichas == null -> stringResource(R.string.camara_analizando_detalle)
        else -> stringResource(R.string.camara_corregir_detalle)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            onClick = alCerrar,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White.copy(alpha = 0.14f),
                contentColor = Color.White,
            ),
        ) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.cerrar))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

/** Recuadro y etiqueta de cada ficha detectada en vivo, sobre la vista previa. */
@Composable
private fun MarcasFichas(fichas: List<FichaDetectada>, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    Canvas(modifier) {
        dibujarFichas(fichas, Rect(Offset.Zero, size), medidor, seleccionada = null)
    }
}

/**
 * La foto ya sacada (ajustada al recuadro sin recortarla) con sus fichas. Tocar una caja la elige
 * para corregirla; tocar fuera de las cajas marca un lugar para agregar una ficha. Se puede acercar
 * pellizcando y mover arrastrando.
 */
@Composable
private fun FotoConMarcas(
    foto: FotoFichas,
    seleccionada: Int?,
    nueva: CajaNormalizada?,
    alTocarFicha: (Int) -> Unit,
    alTocarVacio: (CajaNormalizada) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imagen = remember(foto.imagen) { foto.imagen.asImageBitmap() }
    val fichas = foto.fichas
    // Cada foto nueva empieza sin acercar.
    var zoom by remember(foto.imagen) { mutableStateOf(ZoomFoto()) }

    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        MarcasFoto(
            imagen = imagen,
            fichas = fichas.orEmpty(),
            seleccionada = seleccionada,
            nueva = nueva,
            zoom = zoom,
            alCambiarZoom = { zoom = it },
            modifier = Modifier.fillMaxSize(),
            alTocar = { x, y ->
                if (fichas != null) {
                    val indice = fichas.indexOfFirst { it.caja.contiene(x, y) }
                    if (indice >= 0) alTocarFicha(indice) else alTocarVacio(cajaNueva(x, y, fichas))
                }
            },
        )
        if (fichas == null) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
        }
    }
}

/**
 * La foto con sus cajas, todo en un mismo lienzo para que el [zoom] las mueva juntas. [alTocar]
 * recibe el punto tocado relativo a la imagen (0 a 1).
 */
@Composable
private fun MarcasFoto(
    imagen: ImageBitmap,
    fichas: List<FichaDetectada>,
    seleccionada: Int?,
    nueva: CajaNormalizada?,
    zoom: ZoomFoto,
    alCambiarZoom: (ZoomFoto) -> Unit,
    modifier: Modifier = Modifier,
    alTocar: (x: Float, y: Float) -> Unit,
) {
    val medidor = rememberTextMeasurer()
    // Los detectores de gestos se crean una sola vez: siempre tienen que usar la última versión.
    val alTocarActual by rememberUpdatedState(alTocar)
    val alCambiarZoomActual by rememberUpdatedState(alCambiarZoom)
    val zoomActual by rememberUpdatedState(zoom)
    Canvas(
        modifier
            .pointerInput(imagen) {
                detectTapGestures { toque ->
                    val area = zoomActual.area(areaImagen(size.toSize(), imagen.width, imagen.height))
                    val x = (toque.x - area.left) / area.width
                    val y = (toque.y - area.top) / area.height
                    if (x in 0f..1f && y in 0f..1f) alTocarActual(x, y)
                }
            }
            .pointerInput(imagen) {
                detectTransformGestures { centro, pan, factor, _ ->
                    val base = areaImagen(size.toSize(), imagen.width, imagen.height)
                    alCambiarZoomActual(zoomActual.transformar(base, centro, pan, factor))
                }
            },
    ) {
        val area = zoom.area(areaImagen(size, imagen.width, imagen.height))
        drawImage(
            image = imagen,
            dstOffset = IntOffset(area.left.roundToInt(), area.top.roundToInt()),
            dstSize = IntSize(area.width.roundToInt(), area.height.roundToInt()),
        )
        dibujarFichas(fichas, area, medidor, seleccionada)
        if (nueva != null) {
            val lugar = nueva.en(area)
            drawRect(
                color = ColorSeleccion,
                topLeft = lugar.topLeft,
                size = lugar.size,
                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))),
            )
        }
    }
}

/** Fila de valores para corregir la ficha elegida (o elegir el de una ficha nueva). */
@Composable
private fun SelectorEtiqueta(actual: String?, permiteQuitar: Boolean, alElegir: (String?) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val ancho = anchoOpcion(disponible = maxWidth)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SeparacionOpciones),
        ) {
            Etiquetas.forEach { etiqueta ->
                OpcionEtiqueta(
                    texto = etiqueta,
                    elegida = etiqueta == actual,
                    alTocar = { alElegir(etiqueta) },
                    modifier = Modifier.width(ancho),
                )
            }
            if (permiteQuitar) {
                OpcionEtiqueta(
                    texto = stringResource(R.string.accion_quitar_ficha),
                    elegida = false,
                    alTocar = { alElegir(null) },
                    modifier = Modifier.widthIn(min = ancho),
                )
            }
        }
    }
}

@Composable
private fun OpcionEtiqueta(texto: String, elegida: Boolean, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = alTocar,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (elegida) ColorMarca else Color.White.copy(alpha = 0.10f),
        contentColor = if (elegida) ColorTextoMarca else Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
    ) {
        Box(Modifier.heightIn(min = 44.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(texto, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

private val SeparacionOpciones = 8.dp
private val AnchoMinimoOpcion = 48.dp

/** Parte que se llega a ver de la primera opción que no entra entera. */
private const val FRACCION_OPCION_CORTADA = 0.6f

/**
 * Ancho de cada opción para que, en el espacio [disponible], la última que se ve quede cortada:
 * así se nota que la fila sigue y se puede deslizar.
 */
private fun anchoOpcion(disponible: Dp): Dp {
    val enteras = ((disponible - AnchoMinimoOpcion * FRACCION_OPCION_CORTADA) / (AnchoMinimoOpcion + SeparacionOpciones))
        .toInt()
        .coerceAtLeast(1)
    return (disponible - SeparacionOpciones * enteras) / (enteras + FRACCION_OPCION_CORTADA)
}

/** Dibuja cada ficha dentro de [area] (el rectángulo que ocupa la imagen en el lienzo). */
private fun DrawScope.dibujarFichas(
    fichas: List<FichaDetectada>,
    area: Rect,
    medidor: TextMeasurer,
    seleccionada: Int?,
) {
    if (fichas.isEmpty()) return
    val cajas = fichas.map { it.caja.en(area) }
    val margen = 4.dp.toPx()
    // La etiqueta acompaña al ancho de las cajas en pantalla: chica cuando hay muchas fichas, para
    // no taparlas, y más grande a medida que se acerca la foto.
    val anchoTipico = cajas.map { it.width }.sorted()[cajas.size / 2]
    val estiloEtiqueta = TextStyle(
        color = ColorTextoMarca,
        fontSize = ((anchoTipico - margen * 2) / ANCHO_POR_TAMANO_DE_LETRA).coerceIn(11.sp.toPx(), 16.sp.toPx()).toSp(),
        fontWeight = FontWeight.Bold,
    )
    val trazo = Stroke(width = (if (anchoTipico < 24.dp.toPx()) 2.dp else 3.dp).toPx())
    val esquina = CornerRadius(6.dp.toPx())
    // La elegida va última, para que quede encima de las demás.
    val orden = fichas.indices.sortedBy { it == seleccionada }
    fun color(indice: Int) = if (indice == seleccionada) ColorSeleccion else ColorMarca

    // Primero todos los recuadros y después todas las etiquetas: así ningún recuadro tapa el
    // número de la ficha de al lado.
    orden.forEach { indice ->
        drawRoundRect(color(indice), cajas[indice].topLeft, cajas[indice].size, esquina, style = trazo)
    }
    // Arriba del todo la etiqueta se mete dentro de la caja en vez de salirse de la foto.
    val tope = minOf(area.top, 0f)
    orden.forEach { indice ->
        val caja = cajas[indice]
        val texto = medidor.measure(fichas[indice].etiqueta, estiloEtiqueta)
        val fondo = Size(texto.size.width + margen * 2, texto.size.height.toFloat())
        val origenFondo = Offset(caja.left, (caja.top - fondo.height - 1.dp.toPx()).coerceAtLeast(tope))
        drawRoundRect(color(indice), origenFondo, fondo, CornerRadius(4.dp.toPx()))
        drawText(texto, topLeft = Offset(origenFondo.x + margen, origenFondo.y))
    }
}

/** Ancho de una etiqueta de dos cifras en negrita, en relación con el tamaño de su letra. */
private const val ANCHO_POR_TAMANO_DE_LETRA = 1.2f

/** Las coordenadas vienen normalizadas (0 a 1): se escalan al [area] que ocupa la imagen. */
private fun CajaNormalizada.en(area: Rect) = Rect(
    left = area.left + izquierda * area.width,
    top = area.top + arriba * area.height,
    right = area.left + derecha * area.width,
    bottom = area.top + abajo * area.height,
)

/** Rectángulo que ocupa una imagen de [ancho]x[alto] ajustada (sin recortar) dentro de [lienzo]. */
private fun areaImagen(lienzo: Size, ancho: Int, alto: Int): Rect {
    val escala = minOf(lienzo.width / ancho, lienzo.height / alto)
    val tamano = Size(ancho * escala, alto * escala)
    val origen = Offset((lienzo.width - tamano.width) / 2f, (lienzo.height - tamano.height) / 2f)
    return Rect(origen, tamano)
}

private fun CajaNormalizada.contiene(x: Float, y: Float): Boolean =
    x in izquierda..derecha && y in arriba..abajo

/** Caja para una ficha agregada a mano: centrada en el toque y del tamaño típico de las demás. */
private fun cajaNueva(x: Float, y: Float, fichas: List<FichaDetectada>): CajaNormalizada {
    fun mediana(valores: List<Float>, porDefecto: Float) =
        if (valores.isEmpty()) porDefecto else valores.sorted()[valores.size / 2]
    val ancho = mediana(fichas.map { it.caja.derecha - it.caja.izquierda }, porDefecto = 0.06f)
    val alto = mediana(fichas.map { it.caja.abajo - it.caja.arriba }, porDefecto = 0.08f)
    return CajaNormalizada(
        izquierda = (x - ancho / 2).coerceIn(0f, 1f),
        arriba = (y - alto / 2).coerceIn(0f, 1f),
        derecha = (x + ancho / 2).coerceIn(0f, 1f),
        abajo = (y + alto / 2).coerceIn(0f, 1f),
    )
}
