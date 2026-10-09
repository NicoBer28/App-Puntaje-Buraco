package com.example.puntajeburaco20.ui.login

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.BotonSecundario
import com.example.puntajeburaco20.ui.common.CampoTexto
import com.example.puntajeburaco20.ui.common.Ficha
import com.example.puntajeburaco20.ui.common.RecolectarEventos
import com.example.puntajeburaco20.ui.common.rememberMensajero
import com.example.puntajeburaco20.ui.tema.TemaBuraco
import kotlin.math.abs

@Composable
fun LoginScreen(
    alIniciarSesion: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val cargando by viewModel.cargando.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()
    val actividad = LocalActivity.current
    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Sin sesión no hay a dónde volver: atrás cierra la app.
    BackHandler { actividad?.finish() }
    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is LoginViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
            LoginViewModel.Evento.SesionIniciada -> alIniciarSesion()
        }
    }

    val colores = TemaBuraco.colores
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colores.encabezadoArriba, colores.encabezadoAbajo))),
    ) {
        val altoPantalla = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = altoPantalla * 0.42f)
                    .statusBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AbanicoDeFichas()
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.login_marca),
                    style = MaterialTheme.typography.displayMedium,
                    color = colores.sobreEncabezado,
                )
                Text(
                    stringResource(R.string.login_bajada).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colores.sobreEncabezadoSuave,
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = altoPantalla * 0.58f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column {
                        Text(stringResource(R.string.login_titulo), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            stringResource(R.string.login_subtitulo),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    CampoTexto(
                        valor = usuario,
                        alCambiar = { usuario = it },
                        etiqueta = stringResource(R.string.hint_usuario),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("usuario"),
                        teclado = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    )
                    CampoTexto(
                        valor = password,
                        alCambiar = { password = it },
                        etiqueta = stringResource(R.string.hint_password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password"),
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        acciones = KeyboardActions(onDone = { viewModel.ingresar(usuario, password) }),
                        transformacion = PasswordVisualTransformation(),
                    )
                    Spacer(Modifier.height(4.dp))
                    BotonPrincipal(
                        texto = stringResource(R.string.accion_login),
                        alTocar = { viewModel.ingresar(usuario, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btnLogin"),
                        cargando = cargando,
                    )
                    BotonSecundario(
                        texto = stringResource(R.string.accion_crear_usuario),
                        alTocar = { viewModel.registrarse(usuario, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btnCrear"),
                        habilitado = !cargando,
                    )
                }
            }
        }
        SnackbarHost(
            hostState = mensajero.estado,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding(),
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

/** Cuatro fichas abiertas en abanico, una de cada color del juego. */
@Composable
private fun AbanicoDeFichas() {
    val fichas = listOf(
        3 to Color(0xFF2858D6),
        7 to Color(0xFF0A7E9A),
        11 to Color(0xFFC9850F),
        13 to Color(0xFF0B1626),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(104.dp),
        contentAlignment = Alignment.Center,
    ) {
        fichas.forEachIndexed { i, (numero, color) ->
            val desdeElCentro = i - (fichas.size - 1) / 2f
            Ficha(
                numero = numero,
                color = color,
                modifier = Modifier
                    .offset(x = (desdeElCentro * 46).dp, y = (abs(desdeElCentro) * 9).dp)
                    .rotate(desdeElCentro * 11f),
            )
        }
    }
}
