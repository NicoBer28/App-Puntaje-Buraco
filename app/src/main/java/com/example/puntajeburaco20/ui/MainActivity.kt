package com.example.puntajeburaco20.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.puntajeburaco20.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única actividad: aloja el grafo de navegación. Qué pantalla se muestra al abrir la app
 * (login, nueva partida o partida en curso) lo decide [com.example.puntajeburaco20.ui.nuevapartida.NuevaPartidaFragment].
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main)
