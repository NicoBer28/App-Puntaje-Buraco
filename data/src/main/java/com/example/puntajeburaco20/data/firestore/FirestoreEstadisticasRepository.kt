package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.ENFRENTAMIENTO
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EQUIPOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EQUIPO_GANADOR
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.ESTADISTICAS_PREVIAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.GANADAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.JUGADAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PARTIDAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.RIVALES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.claveEnfrentamiento
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Estadisticas
import com.example.puntajeburaco20.domain.repository.EstadisticasRepository
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Las estadísticas no están guardadas: se cuentan las partidas de `partidas/` que jugó y ganó
 * cada equipo, y se les suma lo que haya en `estadisticasPrevias/` (los resultados anteriores a
 * que existiera el detalle por partida). Ver [EsquemaFirestore].
 *
 * Firestore cuenta en el servidor, sin descargar las partidas, así que hace falta conexión.
 */
@Singleton
class FirestoreEstadisticasRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : EstadisticasRepository {

    private val partidas get() = db.collection(PARTIDAS)

    private fun previas(equipo: Equipo) = db.collection(ESTADISTICAS_PREVIAS).document(equipo.id)

    override suspend fun obtenerGenerales(equipo: Equipo): Estadisticas? = sumar(
        previas = previas(equipo),
        jugadas = partidas.whereArrayContains(EQUIPOS, equipo.id),
        ganadas = partidas.whereEqualTo(EQUIPO_GANADOR, equipo.id),
    )

    override suspend fun obtenerEnfrentamiento(equipo: Equipo, rival: Equipo): Estadisticas? {
        val entreEllos = partidas.whereEqualTo(ENFRENTAMIENTO, claveEnfrentamiento(equipo, rival))
        return sumar(
            previas = previas(equipo).collection(RIVALES).document(rival.id),
            jugadas = entreEllos,
            ganadas = entreEllos.whereEqualTo(EQUIPO_GANADOR, equipo.id),
        )
    }

    /** Hace las tres consultas en paralelo; `null` si el equipo no tiene ninguna partida. */
    private suspend fun sumar(previas: DocumentReference, jugadas: Query, ganadas: Query): Estadisticas? =
        try {
            coroutineScope {
                val anteriores = async { previas.get().await() }
                val cantidadJugadas = async { jugadas.contar() }
                val cantidadGanadas = async { ganadas.contar() }
                val total = Estadisticas(
                    jugadas = cantidadJugadas.await() + (anteriores.await().getLong(JUGADAS) ?: 0),
                    ganadas = cantidadGanadas.await() + (anteriores.await().getLong(GANADAS) ?: 0),
                )
                total.takeIf { it.jugadas > 0 }
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) throw ErrorUsuario.SinConexion
            throw e
        }

    private suspend fun Query.contar(): Long = count().get(AggregateSource.SERVER).await().count
}
