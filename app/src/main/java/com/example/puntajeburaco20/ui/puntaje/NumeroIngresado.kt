package com.example.puntajeburaco20.ui.puntaje

/** Cifras que entran en un campo de la ronda, sin contar el signo. */
private const val MAXIMO_DE_CIFRAS = 6

/**
 * Lo que queda en un campo de la ronda cuando el teclado propone [escrito]: las cifras, con el
 * signo adelante si es negativo. El menos se puede tocar en cualquier momento, no solo antes de
 * la primera cifra, y tocarlo de nuevo lo saca. Cualquier otro carácter se ignora.
 *
 * @return `null` si no entra (demasiadas cifras): el campo queda como estaba.
 */
internal fun numeroIngresado(escrito: String): String? {
    val cifras = escrito.filter { it in '0'..'9' }
    if (cifras.length > MAXIMO_DE_CIFRAS) return null
    val negativo = escrito.count { it == '-' } % 2 == 1
    return if (negativo) "-$cifras" else cifras
}
