package com.example.puntajeburaco20.data.firestore

import android.util.Log
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.BASE_DOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.BASE_UNO
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EMPIEZA
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EQUIPO_DOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EQUIPO_UNO
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.FECHA
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.GANADOR
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PARTIDAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PUNTOS_DOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PUNTOS_UNO
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.RONDAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.USUARIOS
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PartidaJugada
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.domain.model.Ronda
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cada jugador tiene su copia de la partida en `users/{id}/partidas`, con el mismo id en todas.
 * Así consultar el historial de alguien es leer una sola colección, ordenada por fecha, sin
 * necesitar índices compuestos.
 */
@Singleton
class FirestorePartidasJugadasRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val sincronizacion: FirestoreSincronizacionRepository,
) : PartidasJugadasRepository {

    private fun partidasDe(idJugador: String) = db.collection(USUARIOS).document(idJugador).collection(PARTIDAS)

    override suspend fun guardar(partida: Partida) {
        val ganador = requireNotNull(partida.ganador) { "Solo se guardan partidas terminadas" }
        val datos = mapOf(
            EQUIPO_UNO to partida.equipoUno.nombres(),
            EQUIPO_DOS to partida.equipoDos.nombres(),
            EMPIEZA to partida.empieza.nombre,
            RONDAS to partida.rondas.map { it.aMapa() },
            GANADOR to ganador.name,
            FECHA to FieldValue.serverTimestamp(),
        )
        val idPartida = db.collection(PARTIDAS).document().id
        val jugadores = partida.equipoUno.jugadores + partida.equipoDos.jugadores
        sincronizacion.enviar(
            db.batch().apply {
                jugadores.forEach { set(partidasDe(it.id).document(idPartida), datos) }
            },
        )
    }

    override suspend fun obtenerDe(jugador: Jugador, limite: Int): List<PartidaJugada> =
        partidasDe(jugador.id)
            .orderBy(FECHA, Query.Direction.DESCENDING)
            .limit(limite.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.aPartidaJugada() }

    private fun Equipo.nombres(): List<String> = jugadores.map { it.nombre }

    private fun Ronda.aMapa(): Map<String, Int> = mapOf(
        BASE_UNO to equipoUno.base,
        PUNTOS_UNO to equipoUno.puntos,
        BASE_DOS to equipoDos.base,
        PUNTOS_DOS to equipoDos.puntos,
    )

    /** Un documento mal formado se descarta en lugar de romper todo el historial. */
    private fun DocumentSnapshot.aPartidaJugada(): PartidaJugada? = try {
        // Una escritura todavía sin confirmar no tiene la hora del servidor: se usa la estimada.
        val fecha = getTimestamp(FECHA, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        PartidaJugada(
            partida = Partida(
                equipoUno = Equipo(nombres(EQUIPO_UNO).map(::Jugador)),
                equipoDos = Equipo(nombres(EQUIPO_DOS).map(::Jugador)),
                empieza = Jugador(requireNotNull(getString(EMPIEZA))),
                rondas = (get(RONDAS) as? List<*>).orEmpty().map { aRonda(it as Map<*, *>) },
                ganador = LadoEquipo.valueOf(requireNotNull(getString(GANADOR))),
            ),
            fecha = requireNotNull(fecha).toDate().time,
        )
    } catch (e: RuntimeException) {
        Log.w(TAG, "Partida $id inválida; se omite", e)
        null
    }

    private fun DocumentSnapshot.nombres(campo: String): List<String> =
        (get(campo) as List<*>).map { it as String }

    private fun aRonda(mapa: Map<*, *>): Ronda {
        fun valor(campo: String) = (mapa[campo] as Number).toInt()
        return Ronda(
            equipoUno = PuntajeRonda(valor(BASE_UNO), valor(PUNTOS_UNO)),
            equipoDos = PuntajeRonda(valor(BASE_DOS), valor(PUNTOS_DOS)),
        )
    }

    private companion object {
        const val TAG = "PartidasJugadas"
    }
}
