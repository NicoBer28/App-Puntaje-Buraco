package com.example.puntajeburaco20.ui.login

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.ui.common.BotonPrincipal
import com.example.puntajeburaco20.ui.common.BotonSecundario
import com.example.puntajeburaco20.ui.common.CampoContrasena
import com.example.puntajeburaco20.ui.common.CampoTexto
import com.example.puntajeburaco20.ui.common.Ficha
import com.example.puntajeburaco20.ui.common.Mensajero
import com.example.puntajeburaco20.ui.common.RecolectarEventos
import com.example.puntajeburaco20.ui.common.rememberMensajero
import com.example.puntajeburaco20.ui.tema.TemaBuraco
import kotlin.math.abs

/**
 * Acceso a la app. Según el estado de la [sesion] muestra el ingreso, el aviso para verificar el
 * mail o la elección del nombre de usuario (o del perfil que la persona ya tenía); cuando la
 * sesión queda completa, deja de mostrarse.
 */
@Composable
fun LoginScreen(
    sesion: EstadoSesion,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val cargando by viewModel.cargando.collectAsStateWithLifecycle()
    val usuarioAnterior by viewModel.usuarioAnterior.collectAsStateWithLifecycle()
    val mensajero = rememberMensajero()
    val actividad = LocalActivity.current

    // Sin sesión no hay a dónde volver: atrás cierra la app.
    BackHandler { actividad?.finish() }
    RecolectarEventos(viewModel.eventos) { evento ->
        when (evento) {
            is LoginViewModel.Evento.Mensaje -> mensajero.mostrar(evento.texto)
        }
    }

    MarcoAcceso(mensajero) {
        when (sesion) {
            is EstadoSesion.SinVerificar -> PasoVerificacion(
                mail = sesion.cuenta.mail,
                cargando = cargando,
                alComprobar = viewModel::comprobarVerificacion,
                alReenviar = viewModel::reenviarVerificacion,
                alSalir = viewModel::salir,
            )
            is EstadoSesion.SinPerfil -> {
                // Quien ya entraba en este dispositivo con una versión anterior arranca
                // recuperando su perfil: si eligiera un nombre nuevo perdería su historial.
                var eligioRecuperar by rememberSaveable { mutableStateOf<Boolean?>(null) }
                if (eligioRecuperar ?: (usuarioAnterior != null)) {
                    PasoPerfilAnterior(
                        usuarioAnterior = usuarioAnterior,
                        cargando = cargando,
                        alVincular = { nombre, passwordAnterior ->
                            // Al vincular, el usuario anterior se olvida: sin esto el paso
                            // cambiaría mientras el acceso se desvanece.
                            eligioRecuperar = true
                            viewModel.vincularPerfil(nombre, passwordAnterior)
                        },
                        alCrearNuevo = { eligioRecuperar = false },
                    )
                } else {
                    PasoNombre(
                        cargando = cargando,
                        alElegir = viewModel::elegirNombre,
                        alRecuperar = { eligioRecuperar = true },
                        alSalir = viewModel::salir,
                    )
                }
            }
            else -> PasoIngreso(
                cargando = cargando,
                alIngresar = viewModel::ingresar,
                alRegistrarse = viewModel::registrarse,
                alRecuperar = viewModel::recuperarContrasena,
            )
        }
    }
}

/** Fondo de la app mientras todavía no se sabe si hay una sesión iniciada. */
@Composable
fun FondoAcceso(modifier: Modifier = Modifier) {
    val colores = TemaBuraco.colores
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colores.encabezadoArriba, colores.encabezadoAbajo))),
    )
}

/** La marca arriba y, abajo, una hoja con el contenido del paso actual. */
@Composable
private fun MarcoAcceso(mensajero: Mensajero, paso: @Composable ColumnScope.() -> Unit) {
    val colores = TemaBuraco.colores
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val altoPantalla = maxHeight
        FondoAcceso()
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
                    content = paso,
                )
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

@Composable
private fun TituloPaso(titulo: String, detalle: String) {
    Column {
        Text(titulo, style = MaterialTheme.typography.headlineMedium)
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BotonDeTexto(texto: String, alTocar: () -> Unit, habilitado: Boolean, tag: String) {
    TextButton(
        onClick = alTocar,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        enabled = habilitado,
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

/** Ingresar con una cuenta existente o crear una nueva, con los mismos dos campos. */
@Composable
private fun PasoIngreso(
    cargando: Boolean,
    alIngresar: (mail: String, password: String) -> Unit,
    alRegistrarse: (mail: String, password: String) -> Unit,
    alRecuperar: (mail: String) -> Unit,
) {
    var mail by rememberSaveable { mutableStateOf("") }
    val password = rememberTextFieldState()

    TituloPaso(stringResource(R.string.login_titulo), stringResource(R.string.login_subtitulo))
    CampoTexto(
        valor = mail,
        alCambiar = { mail = it },
        etiqueta = stringResource(R.string.hint_mail),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mail"),
        teclado = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Next,
        ),
    )
    CampoContrasena(
        estado = password,
        etiqueta = stringResource(R.string.hint_password),
        alConfirmar = { alIngresar(mail, password.text.toString()) },
        etiquetaDePrueba = "password",
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    BotonPrincipal(
        texto = stringResource(R.string.accion_login),
        alTocar = { alIngresar(mail, password.text.toString()) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("btnLogin"),
        cargando = cargando,
    )
    BotonSecundario(
        texto = stringResource(R.string.accion_crear_cuenta),
        alTocar = { alRegistrarse(mail, password.text.toString()) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("btnCrear"),
        habilitado = !cargando,
    )
    BotonDeTexto(
        texto = stringResource(R.string.accion_olvide_contrasena),
        alTocar = { alRecuperar(mail) },
        habilitado = !cargando,
        tag = "btnOlvide",
    )
}

/** La cuenta existe pero falta abrir el enlace que se envió por mail. */
@Composable
private fun PasoVerificacion(
    mail: String,
    cargando: Boolean,
    alComprobar: (avisar: Boolean) -> Unit,
    alReenviar: () -> Unit,
    alSalir: () -> Unit,
) {
    // El mail se verifica fuera de la app: al volver se comprueba sola, sin tocar nada.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { alComprobar(false) }

    TituloPaso(
        stringResource(R.string.verificacion_titulo),
        stringResource(R.string.verificacion_detalle, mail),
    )
    Spacer(Modifier.height(4.dp))
    BotonPrincipal(
        texto = stringResource(R.string.accion_ya_verifique),
        alTocar = { alComprobar(true) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("btnYaVerifique"),
        cargando = cargando,
    )
    BotonSecundario(
        texto = stringResource(R.string.accion_reenviar_mail),
        alTocar = alReenviar,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("btnReenviar"),
        habilitado = !cargando,
    )
    BotonDeTexto(
        texto = stringResource(R.string.accion_usar_otra_cuenta),
        alTocar = alSalir,
        habilitado = !cargando,
        tag = "btnOtraCuenta",
    )
}

/** Mail verificado: falta elegir el nombre con el que la van a buscar los demás. */
@Composable
private fun PasoNombre(
    cargando: Boolean,
    alElegir: (nombre: String) -> Unit,
    alRecuperar: () -> Unit,
    alSalir: () -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf("") }

    TituloPaso(stringResource(R.string.nombre_titulo), stringResource(R.string.nombre_detalle))
    CampoTexto(
        valor = nombre,
        alCambiar = { nombre = it },
        etiqueta = stringResource(R.string.hint_usuario),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("usuarioNuevo"),
        teclado = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
        acciones = KeyboardActions(onDone = { alElegir(nombre) }),
    )
    Spacer(Modifier.height(4.dp))
    BotonPrincipal(
        texto = stringResource(R.string.accion_continuar),
        alTocar = { alElegir(nombre) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("btnContinuar"),
        cargando = cargando,
    )
    BotonSecundario(
        texto = stringResource(R.string.accion_ya_tenia_perfil),
        alTocar = alRecuperar,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("btnYaTeniaPerfil"),
        habilitado = !cargando,
    )
    BotonDeTexto(
        texto = stringResource(R.string.accion_usar_otra_cuenta),
        alTocar = alSalir,
        habilitado = !cargando,
        tag = "btnOtraCuenta",
    )
}

/**
 * Para quien ya usaba la app antes de las cuentas con mail: en lugar de elegir un nombre nuevo,
 * recupera su perfil con el usuario y la contraseña que tenía.
 */
@Composable
private fun PasoPerfilAnterior(
    usuarioAnterior: String?,
    cargando: Boolean,
    alVincular: (nombre: String, passwordAnterior: String) -> Unit,
    alCrearNuevo: () -> Unit,
) {
    // El usuario con el que se entraba en este dispositivo ya viene escrito.
    var nombre by rememberSaveable { mutableStateOf(usuarioAnterior.orEmpty()) }
    val password = rememberTextFieldState()

    TituloPaso(stringResource(R.string.perfil_anterior_titulo), stringResource(R.string.perfil_anterior_detalle))
    CampoTexto(
        valor = nombre,
        alCambiar = { nombre = it },
        etiqueta = stringResource(R.string.hint_usuario),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("usuarioAnterior"),
        teclado = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
    )
    CampoContrasena(
        estado = password,
        etiqueta = stringResource(R.string.hint_password_anterior),
        alConfirmar = { alVincular(nombre, password.text.toString()) },
        etiquetaDePrueba = "passwordAnterior",
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    BotonPrincipal(
        texto = stringResource(R.string.accion_vincular_perfil),
        alTocar = { alVincular(nombre, password.text.toString()) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("btnVincular"),
        cargando = cargando,
    )
    BotonDeTexto(
        texto = stringResource(R.string.accion_perfil_nuevo),
        alTocar = alCrearNuevo,
        habilitado = !cargando,
        tag = "btnPerfilNuevo",
    )
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
