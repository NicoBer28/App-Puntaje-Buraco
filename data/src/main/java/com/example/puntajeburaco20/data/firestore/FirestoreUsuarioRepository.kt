package com.example.puntajeburaco20.data.firestore

import com.example.puntajeburaco20.data.firestore.EsquemaFirestore.CUENTAS
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Perfiles en el esquema nuevo (ver [EsquemaFirestore]).
 *
 * Las amistades y los perfiles sin login todavía no están: llegan con la fase 2 de
 * docs/PLAN_AUTENTICACION.md, junto con sus reglas de seguridad.
 */
@Singleton
class FirestoreUsuarioRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : UsuarioRepository {

    private fun perfil(id: String) = db.collection(PERFILES).document(id)

    override fun observar(id: String): Flow<Usuario?> = perfil(id).snapshots().map { it.aUsuario() }

    override suspend fun obtener(id: String): Usuario? = perfil(id).get().await().aUsuario()

    override suspend fun buscarPorNombre(nombre: String): Usuario? {
        val reserva = db.collection(NOMBRES).document(Usuario.claveDeNombre(nombre)).get().await()
        return reserva.getString(PERFIL)?.let { obtener(it) }
    }

    override fun observarIdDeCuenta(uid: String): Flow<String?> =
        db.collection(CUENTAS).document(uid).snapshots()
            // Sin conexión y sin copia local, Firestore informa que el documento no existe: eso
            // no alcanza para decir que la cuenta no tiene perfil.
            .filter { it.exists() || !it.metadata.isFromCache }
            .map { it.getString(PERFIL) }

    /**
     * Usa una transacción: si otro dispositivo reserva el mismo nombre entre la lectura y la
     * escritura, Firestore reintenta y la segunda vez lo encuentra ocupado. Las transacciones
     * necesitan conexión.
     *
     * El perfil, el nombre, la cuenta y el mail se escriben juntos porque las reglas de seguridad
     * exigen que sean coherentes entre sí.
     */
    override suspend fun crear(cuenta: Cuenta, nombre: String): Usuario {
        val referencia = db.collection(PERFILES).document()
        val reservaNombre = db.collection(NOMBRES).document(Usuario.claveDeNombre(nombre))
        val creado = db.runTransaction { transaccion ->
            if (transaccion.get(reservaNombre).exists()) {
                false
            } else {
                val apuntaAlPerfil = mapOf(PERFIL to referencia.id)
                transaccion.set(referencia, mapOf(NOMBRE to nombre, UID to cuenta.uid))
                transaccion.set(reservaNombre, apuntaAlPerfil)
                transaccion.set(db.collection(CUENTAS).document(cuenta.uid), apuntaAlPerfil)
                transaccion.set(db.collection(MAILS).document(cuenta.mail), apuntaAlPerfil + (UID to cuenta.uid))
                true
            }
        }.await()
        if (!creado) throw ErrorUsuario.NombreEnUso
        return Usuario(Jugador(referencia.id, nombre), amigos = emptyList())
    }

    override suspend fun crearSinLogin(nombre: String, creador: Cuenta): Usuario = pendienteFase2()

    override suspend fun agregarAmistad(usuario: Jugador, amigo: Jugador): Unit = pendienteFase2()

    override suspend fun eliminarAmistad(usuario: Jugador, amigo: Jugador): Unit = pendienteFase2()

    private fun pendienteFase2(): Nothing =
        throw UnsupportedOperationException("Los amigos se migran al esquema nuevo en la fase 2")

    private fun DocumentSnapshot.aUsuario(): Usuario? {
        if (!exists()) return null
        val nombre = getString(NOMBRE) ?: return null
        return Usuario(Jugador(id, nombre), amigos = emptyList())
    }
}
