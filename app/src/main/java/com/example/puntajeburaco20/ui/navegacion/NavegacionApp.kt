package com.example.puntajeburaco20.ui.navegacion

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.node.Ref
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.ui.amigos.AmigosScreen
import com.example.puntajeburaco20.ui.historial.HistorialScreen
import com.example.puntajeburaco20.ui.login.FondoAcceso
import com.example.puntajeburaco20.ui.login.LoginScreen
import com.example.puntajeburaco20.ui.nuevapartida.NuevaPartidaScreen
import com.example.puntajeburaco20.ui.partidas.PartidasJugadasScreen
import com.example.puntajeburaco20.ui.perfil.PerfilScreen
import com.example.puntajeburaco20.ui.puntaje.PuntajeScreen
import com.example.puntajeburaco20.ui.sesion.SesionViewModel
import kotlinx.serialization.Serializable

/** Destinos de la navegación. */
sealed interface Ruta {
    @Serializable data object NuevaPartida : Ruta
    @Serializable data object Puntaje : Ruta
    @Serializable data object Amigos : Ruta
    @Serializable data object Historial : Ruta
    @Serializable data object PartidasJugadas : Ruta
    @Serializable data object Perfil : Ruta
}

private const val DURACION_MS = 280

/**
 * Raíz de la app. Mientras la sesión no esté completa se muestra el acceso (ingresar, verificar
 * el mail y elegir nombre de usuario); recién después, las pantallas.
 */
@Composable
fun RaizApp(viewModel: SesionViewModel = hiltViewModel()) {
    val sesion by viewModel.sesion.collectAsStateWithLifecycle()
    // La clave es el tipo de contenido, no la sesión: así el acceso no se reinicia entre sus pasos.
    val contenido = when (sesion) {
        null -> Contenido.CARGANDO
        is EstadoSesion.Completa -> Contenido.PANTALLAS
        else -> Contenido.ACCESO
    }
    // Mientras el acceso se desvanece la sesión ya está completa: sigue mostrando su último paso.
    val ultimoPasoDeAcceso = remember { Ref<EstadoSesion>() }
    sesion?.takeIf { it !is EstadoSesion.Completa }?.let { ultimoPasoDeAcceso.value = it }

    Crossfade(contenido, animationSpec = tween(DURACION_MS), label = "raiz") { visible ->
        val paso = ultimoPasoDeAcceso.value
        when {
            visible == Contenido.PANTALLAS -> NavegacionApp()
            visible == Contenido.ACCESO && paso != null -> LoginScreen(paso)
            else -> FondoAcceso()
        }
    }
}

private enum class Contenido { CARGANDO, ACCESO, PANTALLAS }

/**
 * Grafo de navegación de las pantallas con sesión iniciada. [NuevaPartidaScreen] es el destino
 * inicial, y lleva a la partida en curso si la app se cerró en medio de una.
 */
@Composable
fun NavegacionApp(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Ruta.NuevaPartida,
        enterTransition = { fadeIn(tween(DURACION_MS)) + slideInHorizontally(tween(DURACION_MS)) { it / 8 } },
        exitTransition = { fadeOut(tween(DURACION_MS)) },
        popEnterTransition = { fadeIn(tween(DURACION_MS)) },
        popExitTransition = { fadeOut(tween(DURACION_MS)) + slideOutHorizontally(tween(DURACION_MS)) { it / 8 } },
    ) {
        composable<Ruta.NuevaPartida> {
            NuevaPartidaScreen(
                irAPartida = { navController.navigate(Ruta.Puntaje) { launchSingleTop = true } },
                irAAmigos = { navController.navigate(Ruta.Amigos) },
                irAEstadisticas = { navController.navigate(Ruta.Historial) },
                irAPerfil = { navController.navigate(Ruta.Perfil) { launchSingleTop = true } },
            )
        }
        composable<Ruta.Puntaje> {
            PuntajeScreen(alSalir = { navController.popBackStack() })
        }
        composable<Ruta.Amigos> {
            AmigosScreen(alVolver = { navController.popBackStack() })
        }
        composable<Ruta.Historial> {
            HistorialScreen(
                alVolver = { navController.popBackStack() },
                irAMisPartidas = { navController.navigate(Ruta.PartidasJugadas) },
            )
        }
        composable<Ruta.PartidasJugadas> {
            PartidasJugadasScreen(alVolver = { navController.popBackStack() })
        }
        composable<Ruta.Perfil> {
            PerfilScreen(alVolver = { navController.popBackStack() })
        }
    }
}
