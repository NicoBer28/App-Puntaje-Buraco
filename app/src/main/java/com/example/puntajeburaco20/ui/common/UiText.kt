package com.example.puntajeburaco20.ui.common

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Texto a mostrar en pantalla. Permite que los ViewModels armen mensajes con recursos de strings
 * sin depender de un [Context].
 */
sealed interface UiText {

    data class Recurso(@StringRes val id: Int, val argumentos: List<Any> = emptyList()) : UiText

    /** Texto que varía según [cantidad], que además se usa como primer argumento. */
    data class Plural(@PluralsRes val id: Int, val cantidad: Int) : UiText

    data class Literal(val texto: String) : UiText

    fun resolver(context: Context): String = when (this) {
        is Recurso -> context.getString(id, *argumentos.toTypedArray())
        is Plural -> context.resources.getQuantityString(id, cantidad, cantidad)
        is Literal -> texto
    }

    companion object {
        fun de(@StringRes id: Int, vararg argumentos: Any): UiText = Recurso(id, argumentos.toList())
    }
}
