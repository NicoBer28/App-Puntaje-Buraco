package com.example.puntajeburaco20.ui.navegacion

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.puntajeburaco20.ui.amigos.AmigosScreen
import com.example.puntajeburaco20.ui.historial.HistorialScreen
import com.example.puntajeburaco20.ui.login.LoginScreen
import com.example.puntajeburaco20.ui.nuevapartida.NuevaPartidaScreen
import com.example.puntajeburaco20.ui.partidas.PartidasJugadasScreen
import com.example.puntajeburaco20.ui.perfil.PerfilScreen
import com.example.puntajeburaco20.ui.puntaje.PuntajeScreen
import kotlinx.serialization.Serializable

/** Destinos de la navegación. */
sealed interface Ruta {
    @Serializable data object NuevaPartida : Ruta
    @Serializable data object Login : Ruta
    @Serializable data object Puntaje : Ruta
    @Serializable data object Amigos : Ruta
    @Serializable data object Historial : Ruta
    @Serializable data object PartidasJugadas : Ruta
    @Serializable data object Perfil : Ruta
}

private const val DURACION_MS = 280

/**
 * Grafo de navegación. Qué pantalla se muestra al abrir la app (login, nueva partida o partida en
 * curso) lo decide [NuevaPartidaScreen], que es el destino inicial.
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
                irALogin = {
                    // Solo desde esta pantalla: evita apilar dos logins si el estado se emite de nuevo.
                    if (navController.currentDestination?.hasRoute<Ruta.NuevaPartida>() == true) {
                        navController.navigate(Ruta.Login)
                    }
                },
                irAPartida = { navController.navigate(Ruta.Puntaje) { launchSingleTop = true } },
                irAAmigos = { navController.navigate(Ruta.Amigos) },
                irAEstadisticas = { navController.navigate(Ruta.Historial) },
                irAPerfil = { navController.navigate(Ruta.Perfil) { launchSingleTop = true } },
            )
        }
        composable<Ruta.Login> {
            LoginScreen(alIniciarSesion = { navController.popBackStack() })
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
            PerfilScreen(
                alVolver = { navController.popBackStack() },
                // El login reemplaza al perfil: debajo queda solo la pantalla principal.
                alCerrarSesion = { navController.navigate(Ruta.Login) { popUpTo<Ruta.NuevaPartida>() } },
            )
        }
    }
}
