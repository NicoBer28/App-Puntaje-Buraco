package com.example.puntajeburaco20.ui.puntaje.camara

import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.CajaNormalizada
import com.example.puntajeburaco20.domain.model.FichaDetectada
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.BotonSecundario
import com.example.puntajeburaco20.ui.common.textoEncima
import com.example.puntajeburaco20.ui.puntaje.PuntajeViewModel.FotoFichas
import com.example.puntajeburaco20.ui.tema.TemaBuraco

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
    alSumar: (LadoEquipo) -> Unit,
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
                                nueva = it
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple(LadoEquipo.UNO, nombreUno, colores.equipoUno),
                    Triple(LadoEquipo.DOS, nombreDos, colores.equipoDos),
                ).forEach { (lado, nombre, color) ->
                    BotonPrincipal(
                        texto = stringResource(R.string.accion_sumar_a_equipo, nombre),
                        alTocar = { alSumar(lado) },
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
 * para corregirla; tocar fuera de las cajas marca un lugar para agregar una ficha.
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

    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Image(imagen, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        if (fichas == null) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
        } else {
            MarcasFoto(
                anchoImagen = imagen.width,
                altoImagen = imagen.height,
                fichas = fichas,
                seleccionada = seleccionada,
                nueva = nueva,
                modifier = Modifier.fillMaxSize(),
                alTocar = { x, y ->
                    val indice = fichas.indexOfFirst { it.caja.contiene(x, y) }
                    if (indice >= 0) alTocarFicha(indice) else alTocarVacio(cajaNueva(x, y, fichas))
                },
            )
        }
    }
}

/** Cajas de la foto analizada. [alTocar] recibe el punto tocado relativo a la imagen (0 a 1). */
@Composable
private fun MarcasFoto(
    anchoImagen: Int,
    altoImagen: Int,
    fichas: List<FichaDetectada>,
    seleccionada: Int?,
    nueva: CajaNormalizada?,
    modifier: Modifier = Modifier,
    alTocar: (x: Float, y: Float) -> Unit,
) {
    val medidor = rememberTextMeasurer()
    // El detector de toques se crea una sola vez: siempre tiene que llamar a la última versión.
    val alTocarActual by rememberUpdatedState(alTocar)
    Canvas(
        modifier.pointerInput(anchoImagen, altoImagen) {
            detectTapGestures { toque ->
                val area = areaImagen(Size(size.width.toFloat(), size.height.toFloat()), anchoImagen, altoImagen)
                val x = (toque.x - area.left) / area.width
                val y = (toque.y - area.top) / area.height
                if (x in 0f..1f && y in 0f..1f) alTocarActual(x, y)
            }
        },
    ) {
        val area = areaImagen(size, anchoImagen, altoImagen)
        dibujarFichas(fichas, area, medidor, seleccionada)
        if (nueva != null) {
            drawRect(
                color = ColorSeleccion,
                topLeft = Offset(area.left + nueva.izquierda * area.width, area.top + nueva.arriba * area.height),
                size = Size((nueva.derecha - nueva.izquierda) * area.width, (nueva.abajo - nueva.arriba) * area.height),
                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))),
            )
        }
    }
}

/** Fila de valores para corregir la ficha elegida (o elegir el de una ficha nueva). */
@Composable
private fun SelectorEtiqueta(actual: String?, permiteQuitar: Boolean, alElegir: (String?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Etiquetas.forEach { etiqueta ->
            OpcionEtiqueta(etiqueta, elegida = etiqueta == actual, alTocar = { alElegir(etiqueta) })
        }
        if (permiteQuitar) {
            OpcionEtiqueta(stringResource(R.string.accion_quitar_ficha), elegida = false, alTocar = { alElegir(null) })
        }
    }
}

@Composable
private fun OpcionEtiqueta(texto: String, elegida: Boolean, alTocar: () -> Unit) {
    Surface(
        onClick = alTocar,
        shape = RoundedCornerShape(50),
        color = if (elegida) ColorMarca else Color.White.copy(alpha = 0.10f),
        contentColor = if (elegida) ColorTextoMarca else Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
    ) {
        Box(Modifier.heightIn(min = 44.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Dibuja cada ficha dentro de [area] (el rectángulo que ocupa la imagen en el lienzo). */
private fun DrawScope.dibujarFichas(
    fichas: List<FichaDetectada>,
    area: Rect,
    medidor: TextMeasurer,
    seleccionada: Int?,
) {
    // Con muchas fichas las cajas son chicas: la etiqueta se achica para no taparlas.
    val estiloEtiqueta = TextStyle(
        color = ColorTextoMarca,
        fontSize = if (fichas.size > 20) 11.sp else 14.sp,
        fontWeight = FontWeight.Bold,
    )
    val trazo = Stroke(width = (if (fichas.size > 20) 2.dp else 3.dp).toPx())
    val esquina = CornerRadius(6.dp.toPx())
    val margen = 4.dp.toPx()
    fichas.forEachIndexed { indice, ficha ->
        val color = if (indice == seleccionada) ColorSeleccion else ColorMarca
        // Las coordenadas vienen normalizadas (0 a 1): se escalan al área de la imagen.
        val arribaIzquierda = Offset(area.left + ficha.caja.izquierda * area.width, area.top + ficha.caja.arriba * area.height)
        val tamanoCaja = Size(
            (ficha.caja.derecha - ficha.caja.izquierda) * area.width,
            (ficha.caja.abajo - ficha.caja.arriba) * area.height,
        )
        drawRoundRect(color, arribaIzquierda, tamanoCaja, esquina, style = trazo)

        val texto = medidor.measure(ficha.etiqueta, estiloEtiqueta)
        val fondo = Size(texto.size.width + margen * 2, texto.size.height.toFloat())
        val origenFondo = Offset(arribaIzquierda.x, (arribaIzquierda.y - fondo.height - 1.dp.toPx()).coerceAtLeast(0f))
        drawRoundRect(color, origenFondo, fondo, CornerRadius(4.dp.toPx()))
        drawText(texto, topLeft = Offset(origenFondo.x + margen, origenFondo.y))
    }
}

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
