package com.example.puntajeburaco20.ui.recorrido

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.domain.model.Recorrido
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import com.example.puntajeburaco20.ui.common.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Qué recorridos de ayuda falta mostrar en este dispositivo. */
@HiltViewModel
class RecorridoViewModel @Inject constructor(
    private val preferencias: PreferenciasRepository,
) : ViewModel() {

    /** `null` hasta que se lee la preferencia guardada. */
    val pendientes: StateFlow<Set<Recorrido>?> = preferencias.recorridosVistos
        .map { vistos -> Recorrido.entries.toSet() - vistos }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Lo deja como visto, se haya llegado al final o se lo haya omitido. */
    fun terminar(recorrido: Recorrido) {
        // Si no se puede guardar no hay nada que avisar: se vuelve a mostrar la próxima vez.
        viewModelScope.launch { intentar { preferencias.marcarRecorridoVisto(recorrido) } }
    }
}
