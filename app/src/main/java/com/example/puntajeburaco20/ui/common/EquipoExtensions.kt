package com.example.puntajeburaco20.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.example.puntajeburaco20.R
import com.example.puntajeburaco20.domain.model.Equipo

/** "Ana" para un jugador solo, "Ana y Beto" para una pareja. */
@Composable
@ReadOnlyComposable
fun Equipo.nombreVisible(): String {
    val nombres = jugadores.map { it.nombre }
    return if (esPareja) stringResource(R.string.equipo_pareja, nombres[0], nombres[1]) else nombres[0]
}
