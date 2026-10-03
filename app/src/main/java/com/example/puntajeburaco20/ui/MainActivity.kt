package com.example.puntajeburaco20.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.puntajeburaco20.ui.navegacion.NavegacionApp
import com.example.puntajeburaco20.ui.tema.TemaBuraco
import dagger.hilt.android.AndroidEntryPoint

/** Única actividad: aloja la navegación entre las pantallas, todas hechas con Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // La app se dibuja detrás de las barras del sistema (edge-to-edge); cada pantalla deja sus
        // márgenes. Todas tienen arriba un encabezado azul oscuro, así que los íconos de la barra
        // de estado van siempre en blanco.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            TemaBuraco {
                NavegacionApp()
            }
        }
    }
}
