package com.example.puntajeburaco20.ui.puntaje.camara

import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.FichaDetectada
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.textoEncima
import com.example.puntajeburaco20.ui.tema.TemaBuraco

private val ColorMarca = Color(0xFF7CC4FA)

/**
 * Cámara a pantalla completa para contar fichas. Mientras está en pantalla la [camara] está
 * encendida; al salir de la composición se apaga.
 */
@Composable
fun VistaCamara(
    camara: CamaraFichas,
    fichas: List<FichaDetectada>,
    nombreUno: String,
    nombreDos: String,
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
    BackHandler(onBack = alCerrar)

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
                    Text(
                        stringResource(R.string.camara_titulo),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        stringResource(R.string.camara_detalle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            val descripcion = stringResource(R.string.cd_vista_camara)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .semantics { contentDescription = descripcion },
            ) {
                AndroidView(factory = { vistaPrevia }, modifier = Modifier.fillMaxSize())
                MarcasFichas(fichas, Modifier.fillMaxSize())
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple(LadoEquipo.UNO, nombreUno, colores.equipoUno),
                    Triple(LadoEquipo.DOS, nombreDos, colores.equipoDos),
                ).forEach { (lado, nombre, color) ->
                    BotonPrincipal(
                        texto = stringResource(R.string.accion_sumar_a_equipo, nombre),
                        alTocar = { alSumar(lado) },
                        modifier = Modifier.weight(1f),
                        colores = ButtonDefaults.buttonColors(containerColor = color, contentColor = color.textoEncima()),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Recuadro y etiqueta de cada ficha detectada, sobre la vista previa. */
@Composable
private fun MarcasFichas(fichas: List<FichaDetectada>, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val estiloEtiqueta = TextStyle(color = Color(0xFF071A33), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Canvas(modifier) {
        val trazo = Stroke(width = 3.dp.toPx())
        val esquina = CornerRadius(8.dp.toPx())
        val margen = 6.dp.toPx()
        for (ficha in fichas) {
            // Las coordenadas vienen normalizadas (0 a 1): se escalan al tamaño del lienzo.
            val arribaIzquierda = Offset(ficha.caja.izquierda * size.width, ficha.caja.arriba * size.height)
            val tamanoCaja = Size(
                (ficha.caja.derecha - ficha.caja.izquierda) * size.width,
                (ficha.caja.abajo - ficha.caja.arriba) * size.height,
            )
            drawRoundRect(ColorMarca, arribaIzquierda, tamanoCaja, esquina, style = trazo)

            val texto = medidor.measure(ficha.etiqueta, estiloEtiqueta)
            val fondo = Size(texto.size.width + margen * 2, texto.size.height + margen)
            val origenFondo = Offset(arribaIzquierda.x, (arribaIzquierda.y - fondo.height - 2.dp.toPx()).coerceAtLeast(0f))
            drawRoundRect(ColorMarca, origenFondo, fondo, CornerRadius(6.dp.toPx()))
            drawText(texto, topLeft = Offset(origenFondo.x + margen, origenFondo.y + margen / 2))
        }
    }
}
