package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.AMIGOS_IDS
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.AMIGOS_NOMBRES
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.NOMBRE
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.PASSWORD
import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.USUARIOS
import com.example.puntajeburaco20.domain.error.ErrorUsuario
import com.example.puntajeburaco20.domain.model.Jugador
import com.example.puntajeburaco20.domain.model.Usuario
import com.example.puntajeburaco20.domain.repository.UsuarioRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUsuarioRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : UsuarioRepository {

    private fun documento(id: String) = db.collection(USUARIOS).document(id)

    override fun observar(id: String): Flow<Usuario?> = callbackFlow {
        val registro = documento(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else {
                trySend(snapshot?.aUsuario())
            }
        }
        awaitClose { registro.remove() }
    }

    override suspend fun obtener(id: String): Usuario? =
        documento(id).get().await().aUsuario()

    override suspend fun autenticar(nombre: String, password: String): Usuario {
        val snapshot = documento(Jugador.idDesdeNombre(nombre)).get().await()
        val usuario = snapshot.aUsuario() ?: throw ErrorUsuario.UsuarioInexistente
        if (snapshot.getString(PASSWORD) != password) throw ErrorUsuario.ContrasenaIncorrecta
        return usuario
    }

    override suspend fun crear(nombre: String, password: String): Usuario {
        val usuario = Usuario(Jugador(nombre), amigos = emptyList())
        val datos = mapOf(
            NOMBRE to nombre,
            PASSWORD to password,
            AMIGOS_IDS to emptyList<String>(),
            AMIGOS_NOMBRES to emptyList<String>(),
        )
        documento(usuario.id).set(datos).await()
        return usuario
    }

    override suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador) {
        modificarAmistad(usuario, amigo) { FieldValue.arrayUnion(it) }
    }

    override suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador) {
        modificarAmistad(usuario, amigo) { FieldValue.arrayRemove(it) }
    }

    /** Aplica la misma operación de lista en ambos usuarios, de forma atómica. */
    private suspend fun modificarAmistad(
        usuario: Jugador,
        amigo: Jugador,
        operacion: (Any) -> FieldValue,
    ) {
        db.batch().apply {
            update(
                documento(usuario.id),
                AMIGOS_IDS, operacion(amigo.id),
                AMIGOS_NOMBRES, operacion(amigo.nombre),
            )
            update(
                documento(amigo.id),
                AMIGOS_IDS, operacion(usuario.id),
                AMIGOS_NOMBRES, operacion(usuario.nombre),
            )
        }.commit().await()
    }

    private fun DocumentSnapshot.aUsuario(): Usuario? {
        if (!exists()) return null
        val nombre = getString(NOMBRE) ?: return null
        val amigos = (get(AMIGOS_NOMBRES) as? List<*>).orEmpty().filterIsInstance<String>()
        return Usuario(Jugador(nombre), amigos.map(::Jugador))
    }
}
