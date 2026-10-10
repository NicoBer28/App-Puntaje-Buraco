package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.AMIGOS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.CREADO_POR
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.CUENTAS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.DESDE
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.MAILS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.NOMBRE
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.NOMBRES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PERFIL
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PERFILES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.UID
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Cuenta
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Perfiles y amistades en el esquema nuevo (ver [EsquemaFirestore]). */
@Singleton
class FirestoreUsuarioRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val sincronizacion: FirestoreSincronizacionRepository,
) : UsuarioRepository {

    private fun perfil(id: String) = db.collection(PERFILES).document(id)

    private fun amigos(idPerfil: String) = perfil(idPerfil).collection(AMIGOS)

    private fun reservaDe(nombre: String) = db.collection(NOMBRES).document(Usuario.claveDeNombre(nombre))

    override fun observar(id: String): Flow<Usuario?> =
        combine(perfil(id).snapshots(), amigos(id).snapshots()) { documento, amistades ->
            documento.aUsuario(amistades)
        }

    override suspend fun obtener(id: String): Usuario? {
        val documento = perfil(id).get().await()
        if (!documento.exists()) return null
        return documento.aUsuario(amigos(id).get().await())
    }

    override suspend fun buscarPorNombre(nombre: String): Jugador? {
        val idPerfil = reservaDe(nombre).get().await().getString(PERFIL) ?: return null
        return perfil(idPerfil).get().await().aJugador()
    }

    override fun observarIdDeCuenta(uid: String): Flow<String?> =
        db.collection(CUENTAS).document(uid).snapshots()
            // Sin conexión y sin copia local, Firestore informa que el documento no existe: eso
            // no alcanza para decir que la cuenta no tiene perfil.
            .filter { it.exists() || !it.metadata.isFromCache }
            .map { it.getString(PERFIL) }

    /**
     * El perfil, el nombre, la cuenta y el mail se escriben juntos porque las reglas de seguridad
     * exigen que sean coherentes entre sí.
     */
    override suspend fun crear(cuenta: Cuenta, nombre: String): Usuario {
        val referencia = db.collection(PERFILES).document()
        reservandoNombre(nombre) { transaccion ->
            val apuntaAlPerfil = mapOf(PERFIL to referencia.id)
            transaccion.set(referencia, mapOf(NOMBRE to nombre, UID to cuenta.uid))
            transaccion.set(reservaDe(nombre), apuntaAlPerfil)
            transaccion.set(db.collection(CUENTAS).document(cuenta.uid), apuntaAlPerfil)
            transaccion.set(db.collection(MAILS).document(cuenta.mail), apuntaAlPerfil + (UID to cuenta.uid))
        }
        return Usuario(Jugador(referencia.id, nombre), amigos = emptyList())
    }

    /** El perfil nace ya con la amistad: las reglas no aceptan uno que nadie tenga en su lista. */
    override suspend fun crearAmigoSinLogin(nombre: String, creador: Cuenta, amigoDe: Jugador): Jugador {
        val referencia = db.collection(PERFILES).document()
        val nuevo = Jugador(referencia.id, nombre)
        reservandoNombre(nombre) { transaccion ->
            transaccion.set(referencia, mapOf(NOMBRE to nombre, CREADO_POR to creador.uid))
            transaccion.set(reservaDe(nombre), mapOf(PERFIL to referencia.id))
            transaccion.set(amigos(nuevo.id).document(amigoDe.id), amistadCon(amigoDe))
            transaccion.set(amigos(amigoDe.id).document(nuevo.id), amistadCon(nuevo))
        }
        return nuevo
    }

    /** Los dos lados van en el mismo lote: las reglas rechazan una amistad a medias. */
    override suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador) {
        sincronizacion.enviar(
            db.batch().apply {
                set(amigos(usuario.id).document(amigo.id), amistadCon(amigo))
                set(amigos(amigo.id).document(usuario.id), amistadCon(usuario))
            },
        )
    }

    override suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador) {
        sincronizacion.enviar(
            db.batch().apply {
                delete(amigos(usuario.id).document(amigo.id))
                delete(amigos(amigo.id).document(usuario.id))
            },
        )
    }

    /**
     * Ejecuta [escrituras] solo si el nombre de usuario está libre. Usa una transacción: si otro
     * dispositivo reserva el mismo nombre entre la lectura y la escritura, Firestore reintenta y
     * la segunda vez lo encuentra ocupado. Las transacciones necesitan conexión.
     *
     * @throws ErrorUsuario.NombreEnUso si el nombre ya está reservado.
     */
    private suspend fun reservandoNombre(nombre: String, escrituras: (Transaction) -> Unit) {
        val libre = db.runTransaction { transaccion ->
            val estaLibre = !transaccion.get(reservaDe(nombre)).exists()
            if (estaLibre) escrituras(transaccion)
            estaLibre
        }.await()
        if (!libre) throw ErrorUsuario.NombreEnUso
    }

    /** Lo que guarda cada perfil sobre un amigo: su nombre, para no leer un perfil por cada uno. */
    private fun amistadCon(amigo: Jugador) = mapOf(NOMBRE to amigo.nombre, DESDE to FieldValue.serverTimestamp())

    private fun DocumentSnapshot.aJugador(): Jugador? = getString(NOMBRE)?.let { Jugador(id, it) }

    private fun DocumentSnapshot.aUsuario(amistades: QuerySnapshot): Usuario? {
        val jugador = aJugador() ?: return null
        val amigos = amistades.documents.mapNotNull { it.aJugador() }.sortedBy { it.nombre.lowercase(Locale.ROOT) }
        return Usuario(jugador, amigos)
    }
}
