package com.example.puntajeburaco20.ui.nuevapartida

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.ModoJuego
import com.example.puntajeburaco20.ui.common.Atajo
import com.example.puntajeburaco20.ui.common.Avatar
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.CasillaJugador
import com.example.puntajeburaco20.ui.common.DialogoConfirmacion
import com.example.puntajeburaco20.ui.common.Encabezado
import com.example.puntajeburaco20.ui.common.EtiquetaEquipo
import com.example.puntajeburaco20.ui.common.InsigniaVersus
import com.example.puntajeburaco20.ui.common.PantallaBuraco
import com.example.puntajeburaco20.ui.common.RecolectarEventos
import com.example.puntajeburaco20.ui.common.SeleccionJugadores
import com.example.puntajeburaco20.ui.common.SelectorModo
import com.example.puntajeburaco20.ui.common.Tarjeta
import com.example.puntajeburaco20.ui.common.rememberMensajero
import com.example.puntajeburaco20.ui.tema.TemaBuraco

/**
 * Destino inicial de la navegación. Redirige al login si no hay sesión, y a la partida en curso
 * si la app se cerró en medio de una.
 */
@Composable
fun NuevaPartidaScreen(
    irALogin: () -> Unit,
    irAPartida: () -> Unit,
    irAAmigos: () -> Unit,
    irAEstadisticas: () -> Unit,
    irAPerfil: () -> Unit,
    viewModel: NuevaPartidaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()
    val actividad = LocalActivity.current
    var confirmarSalida by remember { mutableStateOf(false) }

    LaunchedEffect(estado.sinSesion) {
        if (estado.sinSesion) irALogin()
    }
    BackHandler { confirmarSalida = true }
    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is NuevaPartidaViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
            NuevaPartidaViewModel.Evento.IrAPartida -> irAPartida()
        }
    }

    PantallaBuraco(
        mensajero = mensajero,
        encabezado = {
            Encabezado(
                titulo = stringResource(R.string.titulo_nueva_partida),
                subtitulo = stringResource(R.string.subtitulo_nueva_partida),
                navegacion = { Saludo(estado.nombreUsuario, irAPerfil) },
            ) {
                AnimatedVisibility(visible = estado.cambiosPendientes) {
                    AvisoSincronizacion()
                }
            }
        },
        pie = {
            BotonPrincipal(
                texto = stringResource(R.string.accion_nueva_partida),
                alTocar = viewModel::iniciarPartida,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btnNuevaPartida"),
                icono = R.drawable.ic_play_arrow,
                cargando = estado.iniciando,
            )
        },
    ) {
        SelectorModo(modo = estado.seleccion.modo, alCambiar = viewModel::cambiarModo)
        Equipos(estado.seleccion, viewModel::elegirJugador)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Atajo(
                titulo = stringResource(R.string.atajo_estadisticas),
                detalle = stringResource(R.string.atajo_estadisticas_detalle),
                icono = R.drawable.ic_bar_chart,
                alTocar = irAEstadisticas,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btnHistoriales"),
            )
            Atajo(
                titulo = stringResource(R.string.atajo_amigos),
                detalle = stringResource(R.string.atajo_amigos_detalle),
                icono = R.drawable.ic_group_add,
                alTocar = irAAmigos,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btnUsuario"),
            )
        }
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            mensaje = R.string.dialogo_salir_app,
            alConfirmar = { actividad?.finish() },
            alCerrar = { confirmarSalida = false },
        )
    }
}

/** Quién tiene la sesión iniciada. Es también el acceso a su perfil. */
@Composable
private fun Saludo(nombre: String, irAPerfil: () -> Unit) {
    val colores = TemaBuraco.colores
    Row(
        Modifier
            .padding(start = 4.dp)
            .clip(CircleShape)
            .clickable(
                onClickLabel = stringResource(R.string.accion_ver_perfil),
                role = Role.Button,
                onClick = irAPerfil,
            )
            .heightIn(min = 48.dp)
            .padding(start = 8.dp, end = 6.dp)
            .testTag("btnPerfil"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            nombre = nombre.ifEmpty { null },
            color = colores.equipoUnoEnEncabezado,
            fondoVacio = Color.White.copy(alpha = 0.12f),
            tamano = 36.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text(
                stringResource(R.string.saludo),
                style = MaterialTheme.typography.bodySmall,
                color = colores.sobreEncabezadoSuave,
            )
            Text(
                nombre,
                modifier = Modifier.testTag("nombreUsuario"),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colores.sobreEncabezadoSuave,
        )
    }
}

/** Aviso de datos que todavía no se subieron (por ejemplo, sin conexión). */
@Composable
private fun AvisoSincronizacion() {
    Row(
        Modifier
            .padding(top = 14.dp)
            .clip(MaterialTheme.shapes.small)
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_cloud_off),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TemaBuraco.colores.sobreEncabezadoSuave,
        )
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.mensaje_cambios_pendientes), style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Las dos tarjetas de equipos. En partidas de 2 cada equipo es un jugador (posiciones 0 y 1); en
 * las de 4 el equipo 1 son las posiciones 0 y 1, y el equipo 2 las 2 y 3.
 */
@Composable
private fun Equipos(seleccion: SeleccionJugadores, alElegir: (Int, Jugador?) -> Unit) {
    val colores = TemaBuraco.colores
    val (posicionesUno, posicionesDos) = when (seleccion.modo) {
        ModoJuego.INDIVIDUAL -> listOf(0) to listOf(1)
        ModoJuego.PAREJAS -> listOf(0, 1) to listOf(2, 3)
    }
    val etiquetas = listOf(
        R.string.etiqueta_jugador_1,
        R.string.etiqueta_jugador_2,
        R.string.etiqueta_jugador_3,
        R.string.etiqueta_jugador_4,
    )
    val avisoEmpieza = when (seleccion.modo) {
        ModoJuego.INDIVIDUAL -> R.string.aviso_empieza_primero
        ModoJuego.PAREJAS -> R.string.aviso_empieza_primero_parejas
    }
    // El aviso va pegado a las tarjetas y centrado: es una nota al pie de los equipos, no el
    // título de los atajos que siguen.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple(R.string.equipo_uno, posicionesUno, colores.equipoUno to colores.equipoUnoSuave),
                    Triple(R.string.equipo_dos, posicionesDos, colores.equipoDos to colores.equipoDosSuave),
                ).forEach { (nombreEquipo, posiciones, paleta) ->
                    val (color, suave) = paleta
                    Tarjeta {
                        EtiquetaEquipo(stringResource(nombreEquipo), color)
                        posiciones.forEach { posicion ->
                            CasillaJugador(
                                etiqueta = stringResource(etiquetas[posicion]),
                                elegido = seleccion.elegido(posicion),
                                opciones = seleccion.opcionesPara(posicion),
                                textoVacio = stringResource(R.string.placeholder_jugador),
                                textoQuitar = stringResource(R.string.quitar_jugador),
                                color = color,
                                colorSuave = suave,
                                alElegir = { alElegir(posicion, it) },
                                modifier = Modifier.testTag("jugador_$posicion"),
                            )
                        }
                    }
                }
            }
            InsigniaVersus(Modifier.align(Alignment.Center))
        }
        Text(
            stringResource(avisoEmpieza),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
