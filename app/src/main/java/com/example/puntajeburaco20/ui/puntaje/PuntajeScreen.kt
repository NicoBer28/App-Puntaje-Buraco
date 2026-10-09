package com.example.puntajeburaco20.ui.puntaje

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.BotonSecundario
import com.example.puntajeburaco20.ui.common.CampoTexto
import com.example.puntajeburaco20.ui.common.DialogoConfirmacion
import com.example.puntajeburaco20.ui.common.Encabezado
import com.example.puntajeburaco20.ui.common.EtiquetaEquipo
import com.example.puntajeburaco20.ui.common.PantallaBuraco
import com.example.puntajeburaco20.ui.common.RecolectarEventos
import com.example.puntajeburaco20.ui.common.TablaRondas
import com.example.puntajeburaco20.ui.common.Tarjeta
import com.example.puntajeburaco20.ui.common.TituloSeccion
import com.example.puntajeburaco20.ui.common.UiText
import com.example.puntajeburaco20.ui.common.nombreVisible
import com.example.puntajeburaco20.ui.common.rememberMensajero
import com.example.puntajeburaco20.ui.common.textoEncima
import com.example.puntajeburaco20.ui.puntaje.camara.CamaraFichas
import com.example.puntajeburaco20.ui.puntaje.camara.VistaCamara
import com.example.puntajeburaco20.ui.tema.TemaBuraco
import com.example.puntajeburaco20.ui.tema.numerico

private enum class Dialogo { SALIR, DESHACER, ELEGIR_GANADOR }

/** Un número entero, posiblemente negativo, todavía a medio escribir ("" o "-" también valen). */
private val FormatoNumero = Regex("-?\\d{0,6}")

@Composable
fun PuntajeScreen(
    alSalir: () -> Unit,
    viewModel: PuntajeViewModel = hiltViewModel(),
) {
    val partida by viewModel.partida.collectAsStateWithLifecycle()
    val camaraActiva by viewModel.camaraActiva.collectAsStateWithLifecycle()
    val fichas by viewModel.fichasEnPantalla.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }
    // Cada aviso de quién empieza hace latir el chip del marcador.
    var avisosEmpieza by remember { mutableIntStateOf(0) }

    var baseUno by rememberSaveable { mutableStateOf("") }
    var puntosUno by rememberSaveable { mutableStateOf("") }
    var baseDos by rememberSaveable { mutableStateOf("") }
    var puntosDos by rememberSaveable { mutableStateOf("") }

    val pedirPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) viewModel.abrirCamara() else mensajero.mostrar(UiText.de(R.string.error_permiso_camara))
    }
    // La cámara es opcional: en dispositivos sin cámara los puntos se cargan a mano.
    val hayCamara = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    val camara = remember(lifecycleOwner) { CamaraFichas(context, lifecycleOwner, viewModel::analizarImagen) }

    BackHandler { dialogo = Dialogo.SALIR }
    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is PuntajeViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
            is PuntajeViewModel.Evento.AvisarQuienEmpieza -> avisosEmpieza++
            PuntajeViewModel.Evento.LimpiarRonda -> {
                baseUno = ""
                puntosUno = ""
                baseDos = ""
                puntosDos = ""
            }
            is PuntajeViewModel.Evento.SumarPuntosDetectados -> when (evento.lado) {
                LadoEquipo.UNO -> puntosUno = sumar(puntosUno, evento.puntos)
                LadoEquipo.DOS -> puntosDos = sumar(puntosDos, evento.puntos)
            }
            PuntajeViewModel.Evento.Salir -> alSalir()
        }
    }

    val actual = partida
    if (actual == null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        return
    }
    val nombreUno = actual.equipoUno.nombreVisible()
    val nombreDos = actual.equipoDos.nombreVisible()
    val sumarRonda = {
        viewModel.sumarRonda(PuntajeViewModel.RondaIngresada(baseUno, puntosUno, baseDos, puntosDos))
    }

    PantallaBuraco(
        mensajero = mensajero,
        encabezado = {
            Encabezado(
                titulo = if (actual.terminada) {
                    stringResource(R.string.titulo_partida_terminada)
                } else {
                    stringResource(R.string.titulo_ronda, actual.rondas.size + 1)
                },
                navegacion = {
                    IconButton(onClick = { dialogo = Dialogo.SALIR }, modifier = Modifier.testTag("btnAtras")) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.accion_salir))
                    }
                },
                acciones = {
                    IconButton(
                        onClick = { dialogo = Dialogo.DESHACER },
                        enabled = actual.sePuedeDeshacer,
                        modifier = Modifier.testTag("btnDeshacer"),
                    ) {
                        Icon(painterResource(R.drawable.ic_undo), contentDescription = stringResource(R.string.accion_deshacer))
                    }
                    if (hayCamara && !actual.terminada) {
                        IconButton(
                            onClick = {
                                val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                if (permiso == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.abrirCamara()
                                } else {
                                    pedirPermisoCamara.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.testTag("btnCamara"),
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_photo_camera),
                                contentDescription = stringResource(R.string.accion_camara),
                            )
                        }
                    }
                },
            ) {
                Marcador(actual, nombreUno, nombreDos, avisosEmpieza)
            }
        },
        // Las acciones de la ronda quedan fijas abajo: a mano aun con el teclado abierto.
        pie = if (actual.terminada) {
            null
        } else {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonSecundario(
                        texto = stringResource(R.string.accion_fin),
                        alTocar = { dialogo = Dialogo.ELEGIR_GANADOR },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btnFin"),
                        icono = R.drawable.ic_sports_score,
                    )
                    BotonPrincipal(
                        texto = stringResource(R.string.accion_sumar),
                        alTocar = sumarRonda,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("btnSumar"),
                    )
                }
            }
        },
    ) {
        if (actual.terminada) {
            TarjetaGanador(
                nombre = if (actual.ganador == LadoEquipo.UNO) nombreUno else nombreDos,
                alSalir = { dialogo = Dialogo.SALIR },
            )
        } else {
            Tarjeta {
                TituloSeccion(stringResource(R.string.anotar_ronda))
                CamposEquipo(
                    nombre = nombreUno,
                    color = TemaBuraco.colores.equipoUno,
                    base = baseUno,
                    alCambiarBase = { baseUno = it },
                    puntos = puntosUno,
                    alCambiarPuntos = { puntosUno = it },
                    etiqueta = "Uno",
                    ultimoCampo = false,
                )
                CamposEquipo(
                    nombre = nombreDos,
                    color = TemaBuraco.colores.equipoDos,
                    base = baseDos,
                    alCambiarBase = { baseDos = it },
                    puntos = puntosDos,
                    alCambiarPuntos = { puntosDos = it },
                    etiqueta = "Dos",
                    ultimoCampo = true,
                )
            }
        }
        Tarjeta(relleno = PaddingValues(vertical = 20.dp)) {
            Box(Modifier.padding(horizontal = 20.dp)) {
                TituloSeccion(stringResource(R.string.rondas_titulo))
            }
            if (actual.rondas.isEmpty()) {
                Text(
                    stringResource(R.string.rondas_vacio),
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TablaRondas(actual)
            }
        }
    }

    if (camaraActiva) {
        VistaCamara(
            camara = camara,
            fichas = fichas,
            nombreUno = nombreUno,
            nombreDos = nombreDos,
            alSumar = viewModel::sumarFichasDetectadas,
            alCerrar = viewModel::cancelarCamara,
            alFallar = viewModel::informarErrorCamara,
        )
    }

    when (dialogo) {
        Dialogo.SALIR -> DialogoConfirmacion(
            mensaje = if (actual.terminada) R.string.dialogo_salir_partida_terminada else R.string.dialogo_salir_partida,
            alConfirmar = viewModel::salir,
            alCerrar = { dialogo = null },
        )
        Dialogo.DESHACER -> DialogoConfirmacion(
            mensaje = R.string.dialogo_deshacer_ronda,
            alConfirmar = viewModel::deshacerRonda,
            alCerrar = { dialogo = null },
        )
        Dialogo.ELEGIR_GANADOR -> DialogoGanador(
            nombreUno = nombreUno,
            nombreDos = nombreDos,
            alElegir = { lado ->
                dialogo = null
                viewModel.finalizar(lado)
            },
            alCerrar = { dialogo = null },
        )
        null -> Unit
    }
}

private fun sumar(actual: String, puntos: Int): String = ((actual.toIntOrNull() ?: 0) + puntos).toString()

/** Los dos totales, grandes, con quién empieza la próxima ronda. */
@Composable
private fun Marcador(partida: Partida, nombreUno: String, nombreDos: String, avisosEmpieza: Int) {
    val colores = TemaBuraco.colores
    val escalaChip = remember { Animatable(1f) }
    LaunchedEffect(avisosEmpieza) {
        if (avisosEmpieza > 0) {
            escalaChip.animateTo(1.12f, tween(140))
            escalaChip.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .height(IntrinsicSize.Min),
    ) {
        LadoMarcador(
            nombre = nombreUno,
            total = partida.totalUno,
            ultima = partida.ultimaRonda.equipoUno.takeIf { partida.rondas.isNotEmpty() },
            color = colores.equipoUnoEnEncabezado,
            gano = partida.ganador == LadoEquipo.UNO,
            etiqueta = "totalUno",
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .padding(vertical = 8.dp)
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.14f)),
        )
        LadoMarcador(
            nombre = nombreDos,
            total = partida.totalDos,
            ultima = partida.ultimaRonda.equipoDos.takeIf { partida.rondas.isNotEmpty() },
            color = colores.equipoDosEnEncabezado,
            gano = partida.ganador == LadoEquipo.DOS,
            etiqueta = "totalDos",
            modifier = Modifier.weight(1f),
        )
    }
    if (!partida.terminada) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .graphicsLayer {
                        scaleX = escalaChip.value
                        scaleY = escalaChip.value
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_play_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colores.ganadorEnEncabezado,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.empieza, partida.empieza.nombre),
                    modifier = Modifier.testTag("empiezaJug"),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun LadoMarcador(
    nombre: String,
    total: Int,
    ultima: PuntajeRonda?,
    color: Color,
    gano: Boolean,
    etiqueta: String,
    modifier: Modifier = Modifier,
) {
    val colores = TemaBuraco.colores
    Column(
        modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Misma altura con o sin trofeo, para que los dos lados queden alineados.
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (gano) {
                Icon(
                    painterResource(R.drawable.ic_emoji_events),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colores.ganadorEnEncabezado,
                )
            } else {
                Box(
                    Modifier
                        .size(width = 14.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            nombre,
            style = MaterialTheme.typography.titleSmall,
            color = colores.sobreEncabezadoSuave,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AnimatedContent(
            targetState = total,
            transitionSpec = {
                val sube = targetState > initialState
                (slideInVertically { if (sube) it else -it } + fadeIn()) togetherWith
                    (slideOutVertically { if (sube) -it else it } + fadeOut())
            },
            label = "total",
        ) { valor ->
            Text(
                valor.toString(),
                modifier = Modifier.testTag(etiqueta),
                style = MaterialTheme.typography.displayLarge.numerico,
                color = if (gano) colores.ganadorEnEncabezado else colores.sobreEncabezado,
            )
        }
        Text(
            if (ultima != null) stringResource(R.string.ultima_ronda, conSigno(ultima.total)) else " ",
            style = MaterialTheme.typography.bodySmall.numerico,
            color = colores.sobreEncabezadoSuave,
        )
    }
}

private fun conSigno(valor: Int): String = if (valor > 0) "+$valor" else valor.toString()

@Composable
private fun CamposEquipo(
    nombre: String,
    color: Color,
    base: String,
    alCambiarBase: (String) -> Unit,
    puntos: String,
    alCambiarPuntos: (String) -> Unit,
    etiqueta: String,
    ultimoCampo: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EtiquetaEquipo(nombre, color)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoNumero(
                valor = base,
                alCambiar = alCambiarBase,
                etiqueta = stringResource(R.string.hint_base),
                color = color,
                accionTeclado = ImeAction.Next,
                modifier = Modifier
                    .weight(1f)
                    .testTag("base$etiqueta"),
            )
            CampoNumero(
                valor = puntos,
                alCambiar = alCambiarPuntos,
                etiqueta = stringResource(R.string.hint_puntos),
                color = color,
                accionTeclado = if (ultimoCampo) ImeAction.Done else ImeAction.Next,
                conCambioDeSigno = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("puntos$etiqueta"),
            )
        }
    }
}

/**
 * Campo para un número entero. El teclado numérico no siempre tiene el signo menos, así que los
 * puntos (que pueden ser negativos) tienen un botón para cambiarlo.
 */
@Composable
private fun CampoNumero(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    color: Color,
    accionTeclado: ImeAction,
    modifier: Modifier = Modifier,
    conCambioDeSigno: Boolean = false,
) {
    val descripcionSigno = stringResource(R.string.accion_cambiar_signo)
    CampoTexto(
        valor = valor,
        alCambiar = { nuevo -> if (nuevo.matches(FormatoNumero)) alCambiar(nuevo) },
        etiqueta = etiqueta,
        modifier = modifier,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = accionTeclado),
        colorFoco = color,
        estiloTexto = MaterialTheme.typography.titleLarge.numerico,
        iconoFinal = if (conCambioDeSigno) {
            {
                IconButton(
                    onClick = { alCambiar(if (valor.startsWith("-")) valor.drop(1) else "-$valor") },
                    modifier = Modifier.semantics { contentDescription = descripcionSigno },
                ) {
                    Text("±", style = MaterialTheme.typography.titleLarge, color = color)
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun TarjetaGanador(nombre: String, alSalir: () -> Unit) {
    val colores = TemaBuraco.colores
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colores.ganadorSuave,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colores.ganador),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_emoji_events),
                        contentDescription = null,
                        tint = colores.ganador.textoEncima(),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        stringResource(R.string.ganador, nombre),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        stringResource(R.string.ganador_detalle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            BotonPrincipal(
                texto = stringResource(R.string.accion_salir),
                alTocar = alSalir,
                modifier = Modifier.fillMaxWidth(),
                colores = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
            )
        }
    }
}

@Composable
private fun DialogoGanador(
    nombreUno: String,
    nombreDos: String,
    alElegir: (LadoEquipo) -> Unit,
    alCerrar: () -> Unit,
) {
    val colores = TemaBuraco.colores
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.dialogo_fin_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.dialogo_fin_mensaje), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                listOf(
                    Triple(LadoEquipo.UNO, nombreUno, colores.equipoUno),
                    Triple(LadoEquipo.DOS, nombreDos, colores.equipoDos),
                ).forEach { (lado, nombre, color) ->
                    BotonPrincipal(
                        texto = nombre,
                        alTocar = { alElegir(lado) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ganador_${lado.name}"),
                        icono = R.drawable.ic_emoji_events,
                        colores = ButtonDefaults.buttonColors(containerColor = color, contentColor = color.textoEncima()),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.cancelar)) }
        },
    )
}
