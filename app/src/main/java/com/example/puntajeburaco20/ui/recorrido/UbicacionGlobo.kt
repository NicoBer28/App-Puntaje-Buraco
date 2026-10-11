package com.example.puntajeburaco20.ui.recorrido

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Esquina superior izquierda del globo que explica un paso: debajo del [hueco] si entra, si no
 * arriba, y centrado sobre él sin salirse del [area] donde se puede dibujar (la pantalla sin las
 * barras del sistema). Si no entra en ninguno de los dos lados va del que tenga más lugar, aunque
 * tape parte del elemento.
 */
internal fun ubicarGlobo(area: Rect, hueco: Rect, globo: Size, separacion: Float): Offset {
    val x = (hueco.center.x - globo.width / 2).acotar(area.left, area.right - globo.width)
    val lugarAbajo = area.bottom - hueco.bottom
    val lugarArriba = hueco.top - area.top
    val necesario = globo.height + separacion
    val abajo = lugarAbajo >= necesario || (lugarArriba < necesario && lugarAbajo >= lugarArriba)
    val y = if (abajo) hueco.bottom + separacion else hueco.top - necesario
    return Offset(x, y.acotar(area.top, area.bottom - globo.height))
}

/** Como `coerceIn`, pero si el rango queda invertido (el globo no entra) gana el [minimo]. */
private fun Float.acotar(minimo: Float, maximo: Float): Float = coerceAtMost(maximo).coerceAtLeast(minimo)
