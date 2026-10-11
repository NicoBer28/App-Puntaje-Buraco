package com.example.puntajeburaco20.data.firestore

import android.util.Log
import com.example.puntajeburaco20.domain.repository.SincronizacionRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Envía las escrituras a Firestore y sigue cuáles todavía no confirmó el servidor.
 *
 * Firestore guarda cada escritura en el dispositivo apenas se hace y la sube cuando hay conexión.
 * Por eso [enviar] no espera al servidor: sin conexión, la app sigue funcionando y
 * [hayCambiosPendientes] avisa que quedan datos por subir.
 */
@Singleton
class FirestoreSincronizacionRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : SincronizacionRepository {

    /** Número de la última escritura enviada: solo su confirmación indica que no queda nada pendiente. */
    private val ultimaEscritura = AtomicLong()

    // Arranca en true hasta saber si quedaron escrituras encoladas de una ejecución anterior.
    private val pendientes = MutableStateFlow(true)

    override val hayCambiosPendientes: StateFlow<Boolean> = pendientes.asStateFlow()

    init {
        esperarConfirmacion()
    }

    fun enviar(lote: WriteBatch) {
        lote.commit().addOnFailureListener { error ->
            Log.e(TAG, "El servidor rechazó una escritura", error)
        }
        pendientes.value = true
        esperarConfirmacion()
    }

    private fun esperarConfirmacion() {
        val escritura = ultimaEscritura.incrementAndGet()
        // Completa cuando el servidor confirma todas las escrituras hechas hasta ahora.
        db.waitForPendingWrites().addOnCompleteListener {
            if (ultimaEscritura.get() == escritura) pendientes.value = false
        }
    }

    private companion object {
        const val TAG = "Sincronizacion"
    }
}
