package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.ESTADISTICAS_INDIVIDUALES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.ESTADISTICAS_PAREJAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PAREJAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PARTIDAS_GANADAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PARTIDAS_JUGADAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.USUARIOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.idParejaAnterior
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Los jugadores individuales guardan sus estadísticas en su propio documento de usuario; las
 * parejas, en la colección de dobles. Ver [EsquemaFirestore].
 *
 * Las parejas pueden tener además un documento con el id del formato anterior: al leer se suman
 * ambos, así el historial previo no se pierde y no hace falta migrar datos.
 */
@Singleton
class FirestoreEstadisticasRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val sincronizacion: FirestoreSincronizacionRepository,
) : EstadisticasRepository {

    override suspend fun registrarResultado(ganador: Equipo, perdedor: Equipo) {
        val victoria = mapOf(
            PARTIDAS_JUGADAS to FieldValue.increment(1),
            PARTIDAS_GANADAS to FieldValue.increment(1),
        )
        val derrota = mapOf(PARTIDAS_JUGADAS to FieldValue.increment(1))

        sincronizacion.enviar(
            db.batch().apply {
                set(generales(ganador.id, ganador.esPareja), victoria, SetOptions.merge())
                set(generales(perdedor.id, perdedor.esPareja), derrota, SetOptions.merge())
                set(enfrentamiento(ganador.id, perdedor.id, ganador.esPareja), victoria, SetOptions.merge())
                set(enfrentamiento(perdedor.id, ganador.id, perdedor.esPareja), derrota, SetOptions.merge())
            },
        )
    }

    override suspend fun obtenerGenerales(equipo: Equipo): Estadisticas? =
        leerYSumar(
            buildList {
                add(generales(equipo.id, equipo.esPareja))
                if (equipo.esPareja) add(generales(idParejaAnterior(equipo), esPareja = true))
            },
        )

    override suspend fun obtenerEnfrentamiento(equipo: Equipo, rival: Equipo): Estadisticas? =
        leerYSumar(
            buildList {
                add(enfrentamiento(equipo.id, rival.id, equipo.esPareja))
                if (equipo.esPareja) {
                    add(enfrentamiento(idParejaAnterior(equipo), idParejaAnterior(rival), esPareja = true))
                }
            },
        )

    private fun generales(id: String, esPareja: Boolean): DocumentReference {
        val coleccion = if (esPareja) PAREJAS else USUARIOS
        return db.collection(coleccion).document(id)
    }

    private fun enfrentamiento(id: String, idRival: String, esPareja: Boolean): DocumentReference {
        val subcoleccion = if (esPareja) ESTADISTICAS_PAREJAS else ESTADISTICAS_INDIVIDUALES
        return generales(id, esPareja).collection(subcoleccion).document(idRival)
    }

    /** Lee los documentos en paralelo y suma los que existen; `null` si no existe ninguno. */
    private suspend fun leerYSumar(documentos: List<DocumentReference>): Estadisticas? = coroutineScope {
        documentos
            .map { async { it.get().await().aEstadisticas() } }
            .awaitAll()
            .filterNotNull()
            .reduceOrNull { total, otras -> total + otras }
    }

    private fun DocumentSnapshot.aEstadisticas(): Estadisticas? {
        if (!exists()) return null
        return Estadisticas(
            jugadas = getLong(PARTIDAS_JUGADAS) ?: 0,
            ganadas = getLong(PARTIDAS_GANADAS) ?: 0,
        )
    }
}
