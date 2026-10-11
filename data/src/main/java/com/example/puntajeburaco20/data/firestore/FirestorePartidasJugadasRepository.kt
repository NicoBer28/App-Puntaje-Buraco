package com.example.puntajeburaco20.data.firestore

import android.util.Log
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.BASE_DOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.BASE_UNO
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.CREADA_POR
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EMPIEZA
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.ENFRENTAMIENTO
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EQUIPOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EQUIPO_DOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EQUIPO_GANADOR
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.EQUIPO_UNO
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.FECHA
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.JUGADORES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.NOMBRES_DE_JUGADORES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PARTIDAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PUNTOS_DOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PUNTOS_UNO
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.RONDAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.claveEnfrentamiento
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Equipo
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.LadoEquipo
import com.example.puntajeburaco20.domain.model.Partida
import com.example.puntajeburaco20.domain.model.PartidaJugada
import com.example.puntajeburaco20.domain.model.PuntajeRonda
import com.example.puntajeburaco20.domain.model.Ronda
import com.example.puntajeburaco20.domain.repository.AuthRepository
import com.example.puntajeburaco20.domain.repository.PartidasJugadasRepository
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cada partida es un solo documento de `partidas/`, compartido por todos sus jugadores. El
 * historial de alguien son las partidas que lo tienen en `jugadores`, ordenadas por fecha (hace
 * falta el índice compuesto de `firestore.indexes.json`).
 */
@Singleton
class FirestorePartidasJugadasRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: AuthRepository,
    private val sincronizacion: FirestoreSincronizacionRepository,
) : PartidasJugadasRepository {

    private val partidas get() = db.collection(PARTIDAS)

    override suspend fun guardar(partida: Partida) {
        val ganador = requireNotNull(partida.ganador) { "Solo se guardan partidas terminadas" }
        val cuenta = auth.cuenta.first() ?: throw ErrorUsuario.SinSesion
        val jugadores = partida.equipoUno.jugadores + partida.equipoDos.jugadores
        val datos = mapOf(
            EQUIPO_UNO to partida.equipoUno.ids(),
            EQUIPO_DOS to partida.equipoDos.ids(),
            // Se guarda el nombre que cada uno tenía al jugar, para mostrar la partida sin leer
            // un perfil por jugador.
            NOMBRES_DE_JUGADORES to jugadores.associate { it.id to it.nombre },
            // Lo que sigue repite a los equipos en la forma en que se consultan: el historial de
            // un jugador y las estadísticas de un equipo, en general o contra un rival.
            JUGADORES to jugadores.map { it.id },
            EQUIPOS to listOf(partida.equipoUno.id, partida.equipoDos.id),
            ENFRENTAMIENTO to claveEnfrentamiento(partida.equipoUno, partida.equipoDos),
            EQUIPO_GANADOR to partida.equipo(ganador).id,
            EMPIEZA to partida.empieza.id,
            RONDAS to partida.rondas.map { it.aMapa() },
            FECHA to FieldValue.serverTimestamp(),
            CREADA_POR to cuenta.uid,
        )
        sincronizacion.enviar(db.batch().apply { set(partidas.document(), datos) })
    }

    override suspend fun obtenerDe(jugador: Jugador, limite: Int): List<PartidaJugada> =
        partidas
            .whereArrayContains(JUGADORES, jugador.id)
            .orderBy(FECHA, Query.Direction.DESCENDING)
            .limit(limite.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.aPartidaJugada() }

    override suspend fun contarDe(jugador: Jugador): Long =
        partidas.whereArrayContains(JUGADORES, jugador.id).count().get(AggregateSource.SERVER).await().count

    private fun Equipo.ids(): List<String> = jugadores.map { it.id }

    private fun Ronda.aMapa(): Map<String, Int> = mapOf(
        BASE_UNO to equipoUno.base,
        PUNTOS_UNO to equipoUno.puntos,
        BASE_DOS to equipoDos.base,
        PUNTOS_DOS to equipoDos.puntos,
    )

    /** Un documento mal formado se descarta en lugar de romper todo el historial. */
    private fun DocumentSnapshot.aPartidaJugada(): PartidaJugada? = try {
        val nombres = get(NOMBRES_DE_JUGADORES) as Map<*, *>
        fun equipo(campo: String) = Equipo((get(campo) as List<*>).map { Jugador(it as String, nombres[it] as String) })
        val equipoUno = equipo(EQUIPO_UNO)
        val equipoDos = equipo(EQUIPO_DOS)
        val idEmpieza = requireNotNull(getString(EMPIEZA))
        // Una escritura todavía sin confirmar no tiene la hora del servidor: se usa la estimada.
        val fecha = getTimestamp(FECHA, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        PartidaJugada(
            partida = Partida(
                equipoUno = equipoUno,
                equipoDos = equipoDos,
                empieza = (equipoUno.jugadores + equipoDos.jugadores).first { it.id == idEmpieza },
                rondas = (get(RONDAS) as? List<*>).orEmpty().map { aRonda(it as Map<*, *>) },
                ganador = when (requireNotNull(getString(EQUIPO_GANADOR))) {
                    equipoUno.id -> LadoEquipo.UNO
                    equipoDos.id -> LadoEquipo.DOS
                    else -> error("El ganador no es ninguno de los dos equipos")
                },
            ),
            fecha = requireNotNull(fecha).toDate().time,
        )
    } catch (e: RuntimeException) {
        Log.w(TAG, "Partida $id inválida; se omite", e)
        null
    }

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
