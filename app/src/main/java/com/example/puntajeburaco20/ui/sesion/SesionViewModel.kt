package com.example.puntajeburaco20.ui.sesion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntajeburaco20.domain.model.EstadoSesion
import com.example.puntajeburaco20.domain.usecase.ObservarSesionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Estado de la sesión para toda la app: decide si se muestra el acceso o las pantallas. */
@HiltViewModel
class SesionViewModel @Inject constructor(
    observarSesion: ObservarSesionUseCase,
) : ViewModel() {

    /** `null` hasta saber en qué estado está la sesión. */
    val sesion: StateFlow<EstadoSesion?> = observarSesion()
        .catch { Log.e("PuntajeBuraco", "No se pudo seguir el estado de la sesión", it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
