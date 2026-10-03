package com.example.puntajeburaco20.ui.common

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.annotation.ArrayRes
import com.example.puntajeburaco20.R

fun Spinner.alSeleccionar(accion: (posicion: Int) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, posicion: Int, id: Long) {
            accion(posicion)
        }

        override fun onNothingSelected(parent: AdapterView<*>) = Unit
    }
}

/** Spinner con opciones fijas definidas en un string-array, con el estilo de la app. */
fun Spinner.configurarOpciones(@ArrayRes opciones: Int, alSeleccionar: (posicion: Int) -> Unit) {
    adapter = ArrayAdapter.createFromResource(context, opciones, R.layout.item_spinner_seleccionado)
        .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    alSeleccionar(alSeleccionar)
}

fun Spinner.seleccionarSiCambia(posicion: Int) {
    if (selectedItemPosition != posicion) setSelection(posicion)
}
