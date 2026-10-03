package com.example.puntajeburaco20.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntajeburaco20.ui.navegacion.NavegacionApp
import com.example.puntajeburaco20.ui.tema.TemaBuraco
import com.example.puntajeburaco20.ui.tema.TemaViewModel
import com.example.puntajeburaco20.ui.tema.esOscuro
import dagger.hilt.android.AndroidEntryPoint

/** Única actividad: aloja la navegación entre las pantallas, todas hechas con Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val temaViewModel: TemaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // La app se dibuja detrás de las barras del sistema (edge-to-edge); cada pantalla deja sus
        // márgenes. Todas tienen arriba un encabezado azul oscuro, así que los íconos de la barra
        // de estado van siempre en blanco.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            val modoTema by temaViewModel.modoTema.collectAsStateWithLifecycle()
            // Hasta leer el tema guardado no se dibuja nada: así no se ve un tema y enseguida el otro.
            modoTema?.let { modo ->
                val oscuro = modo.esOscuro()
                LaunchedEffect(oscuro) { ajustarBarraDeNavegacion(oscuro) }
                TemaBuraco(oscuro) {
                    NavegacionApp()
                }
            }
        }
    }

    /** La barra de navegación sigue al tema de la app, que puede no ser el del sistema. */
    private fun ajustarBarraDeNavegacion(oscuro: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(VELO_CLARO, VELO_OSCURO) { oscuro },
        )
    }

    private companion object {
        // Los mismos velos que usa enableEdgeToEdge por defecto.
        val VELO_CLARO = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val VELO_OSCURO = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
