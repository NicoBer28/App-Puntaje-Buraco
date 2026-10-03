package com.example.puntajeburaco20.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.puntajeburaco20.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única actividad: aloja el grafo de navegación. Qué pantalla se muestra al abrir la app
 * (login, nueva partida o partida en curso) lo decide [com.example.puntajeburaco20.ui.nuevapartida.NuevaPartidaFragment].
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Desde Android 15 la app se dibuja detrás de las barras del sistema (edge-to-edge).
        // Se activa en todas las versiones para que se vea igual, y se dejan márgenes para que
        // el contenido no quede tapado por las barras, el recorte de la cámara o el teclado.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment)) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime(),
            )
            vista.updatePadding(left = barras.left, top = barras.top, right = barras.right, bottom = barras.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}
