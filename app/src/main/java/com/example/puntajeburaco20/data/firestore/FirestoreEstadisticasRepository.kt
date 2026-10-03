package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.ESTADISTICAS_INDIVIDUALES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.ESTADISTICAS_PAREJAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PAREJAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PARTIDAS_GANADAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PARTIDAS_JUGADAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.USUARIOS
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Los jugadores individuales guardan sus estadísticas en su propio documento de usuario; las
 * parejas, en la colección de dobles. Ver [EsquemaFirestore].
 */
@Singleton
class FirestoreEstadisticasRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : EstadisticasRepository {

    override suspend fun registrarResultado(ganador: Equipo, perdedor: Equipo) {
        val victoria = mapOf(
            PARTIDAS_JUGADAS to FieldValue.increment(1),
            PARTIDAS_GANADAS to FieldValue.increment(1),
        )
        val derrota = mapOf(PARTIDAS_JUGADAS to FieldValue.increment(1))

        db.batch().apply {
            set(generales(ganador), victoria, SetOptions.merge())
            set(generales(perdedor), derrota, SetOptions.merge())
            set(enfrentamiento(ganador, perdedor), victoria, SetOptions.merge())
            set(enfrentamiento(perdedor, ganador), derrota, SetOptions.merge())
        }.commit().await()
    }

    override suspend fun obtenerGenerales(equipo: Equipo): Estadisticas? =
        generales(equipo).get().await().aEstadisticas()

    override suspend fun obtenerEnfrentamiento(equipo: Equipo, rival: Equipo): Estadisticas? =
        enfrentamiento(equipo, rival).get().await().aEstadisticas()

    private fun generales(equipo: Equipo): DocumentReference {
        val coleccion = if (equipo.esPareja) PAREJAS else USUARIOS
        return db.collection(coleccion).document(equipo.id)
    }

    private fun enfrentamiento(equipo: Equipo, rival: Equipo): DocumentReference {
        val subcoleccion = if (equipo.esPareja) ESTADISTICAS_PAREJAS else ESTADISTICAS_INDIVIDUALES
        return generales(equipo).collection(subcoleccion).document(rival.id)
    }

    private fun DocumentSnapshot.aEstadisticas(): Estadisticas? {
        if (!exists()) return null
        return Estadisticas(
            jugadas = getLong(PARTIDAS_JUGADAS) ?: 0,
            ganadas = getLong(PARTIDAS_GANADAS) ?: 0,
        )
    }
}
