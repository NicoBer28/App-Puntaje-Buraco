package com.example.puntajeburaco20.ui.tema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.domain.model.ModoTema
import com.example.puntajeburaco20.domain.repository.PreferenciasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Tema elegido por el usuario, para toda la app. */
@HiltViewModel
class TemaViewModel @Inject constructor(preferencias: PreferenciasRepository) : ViewModel() {

    /** `null` hasta que se lee la preferencia guardada. */
    val modoTema: StateFlow<ModoTema?> =
        preferencias.modoTema.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
